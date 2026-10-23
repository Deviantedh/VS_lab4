package portal.employee.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portal.dto.EmployeeAssignmentDto;
import portal.dto.EmployeeDto;
import portal.dto.EmployeeStatus;
import portal.entity.*;
import portal.employee.exception.BusinessConflictException;
import portal.employee.exception.ResourceNotFoundException;
import portal.employee.repository.EmployeeAssignmentRepository;
import portal.employee.repository.EmployeeRepository;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeAssignmentRepository assignmentRepository;
    private final BranchService branchService;
    private final PositionService positionService;
    private final org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional(readOnly = true)
    public Page<EmployeeDto.Response> getAllPaged(Pageable pageable) {
        return employeeRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public EmployeeDto.Response getById(Long id) {
        return toResponse(findEmployeeById(id));
    }

    @Transactional
    public EmployeeDto.Response create(EmployeeDto.Request request) {
        String phone = request.getPhone().trim();
        if (employeeRepository.existsByPhone(phone)) {
            throw new BusinessConflictException("Сотрудник с номером " + phone + " уже числится в штате");
        }

        Employee employee = Employee.builder()
                .name(request.getName().trim())
                .phone(phone)
                .birthDate(request.getBirthDate())
                .hireDate(request.getHireDate() != null ? request.getHireDate() : LocalDate.now())
                .status(request.getStatus() != null ? request.getStatus() : EmployeeStatus.ACTIVE)
                .build();

        Employee saved = employeeRepository.save(employee);
        publishEmployeeEvent(portal.dto.event.EmployeeEventDto.EventType.EMPLOYEE_CREATED, saved, null, null);
        return toResponse(saved);
    }

    @Transactional
    public EmployeeDto.Response update(Long id, EmployeeDto.Request request) {
        Employee employee = findEmployeeById(id);
        String phone = request.getPhone().trim();

        if (!employee.getPhone().equals(phone) && employeeRepository.existsByPhone(phone)) {
            throw new BusinessConflictException("Номер " + phone + " уже принадлежит другому сотруднику");
        }

        employee.setName(request.getName().trim());
        employee.setPhone(phone);
        employee.setBirthDate(request.getBirthDate());
        if (request.getHireDate() != null) {
            employee.setHireDate(request.getHireDate());
        }
        if (request.getStatus() != null) {
            employee.setStatus(request.getStatus());
            if (request.getStatus() == EmployeeStatus.ACTIVE) {
                employee.setDismissalDate(null);
            }
        }

        Employee saved = employeeRepository.save(employee);
        publishEmployeeEvent(portal.dto.event.EmployeeEventDto.EventType.EMPLOYEE_UPDATED, saved, null, null);
        return toResponse(saved);
    }

    @Transactional
    public EmployeeDto.Response dismiss(Long id) {
        Employee employee = findEmployeeById(id);
        if (employee.getStatus() == EmployeeStatus.DISMISSED) {
            throw new BusinessConflictException("Сотрудник " + employee.getName() + " уже уволен");
        }
        employee.setStatus(EmployeeStatus.DISMISSED);
        employee.setDismissalDate(LocalDate.now());
        Employee saved = employeeRepository.save(employee);
        publishEmployeeEvent(portal.dto.event.EmployeeEventDto.EventType.EMPLOYEE_DISMISSED, saved, null, null);
        return toResponse(saved);
    }

    @Transactional
    public EmployeeDto.Response rehire(Long id) {
        Employee employee = findEmployeeById(id);
        if (employee.getStatus() != EmployeeStatus.DISMISSED) {
            throw new BusinessConflictException("Сотрудник " + employee.getName() + " не является уволенным");
        }
        employee.setStatus(EmployeeStatus.ACTIVE);
        employee.setDismissalDate(null);
        Employee saved = employeeRepository.save(employee);
        publishEmployeeEvent(portal.dto.event.EmployeeEventDto.EventType.EMPLOYEE_UPDATED, saved, null, null);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Employee employee = findEmployeeById(id);
        employeeRepository.delete(employee);
    }

    @Transactional
    public EmployeeAssignmentDto.Response assignToBranch(EmployeeAssignmentDto.Request request) {
        Employee employee = findEmployeeById(request.getEmployeeId());
        Branch branch = branchService.findBranchById(request.getBranchId());
        Position position = positionService.findPositionById(request.getPositionId());

        EmployeeAssignment assignment = EmployeeAssignment.builder()
                .employee(employee)
                .branch(branch)
                .position(position)
                .startedAt(request.getStartedAt() != null ? request.getStartedAt() : LocalDate.now())
                .endedAt(request.getEndedAt())
                .isPrimary(request.getIsPrimary() != null ? request.getIsPrimary() : true)
                .build();

        EmployeeAssignment saved = assignmentRepository.save(assignment);
        publishEmployeeEvent(portal.dto.event.EmployeeEventDto.EventType.ASSIGNMENT_CREATED, employee, branch.getId(), position.getId());
        return toAssignmentResponse(saved);
    }

    private void publishEmployeeEvent(portal.dto.event.EmployeeEventDto.EventType eventType, Employee employee, Long branchId, Long positionId) {
        if (kafkaTemplate != null && employee != null) {
            try {
                portal.dto.event.EmployeeEventDto event = portal.dto.event.EmployeeEventDto.builder()
                        .eventType(eventType)
                        .employeeId(employee.getId())
                        .fullName(employee.getName())
                        .phone(employee.getPhone())
                        .status(employee.getStatus() != null ? employee.getStatus().name() : null)
                        .branchId(branchId)
                        .positionId(positionId)
                        .timestamp(java.time.Instant.now())
                        .build();
                kafkaTemplate.send("employee-events", String.valueOf(employee.getId()), event);
            } catch (Exception e) {
                log.warn("Не удалось отправить событие в Kafka: {}", e.getMessage());
            }
        }
    }

    @Transactional(readOnly = true)
    public List<EmployeeAssignmentDto.Response> getAssignments(Long employeeId) {
        findEmployeeById(employeeId);
        return assignmentRepository.findAllByEmployeeId(employeeId).stream()
                .map(this::toAssignmentResponse)
                .toList();
    }

    public Employee findEmployeeById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Сотрудник с ID " + id + " не найден: кажется, он пока не зарегистрирован в системе"));
    }

    public EmployeeDto.Response toResponse(Employee employee) {
        return EmployeeDto.Response.builder()
                .id(employee.getId())
                .name(employee.getName())
                .phone(employee.getPhone())
                .birthDate(employee.getBirthDate())
                .hireDate(employee.getHireDate())
                .dismissalDate(employee.getDismissalDate())
                .status(employee.getStatus())
                .createdAt(employee.getCreatedAt())
                .updatedAt(employee.getUpdatedAt())
                .build();
    }

    private EmployeeAssignmentDto.Response toAssignmentResponse(EmployeeAssignment a) {
        return EmployeeAssignmentDto.Response.builder()
                .id(a.getId())
                .employeeId(a.getEmployee().getId())
                .employeeName(a.getEmployee().getName())
                .branchId(a.getBranch().getId())
                .branchName(a.getBranch().getName())
                .positionId(a.getPosition().getId())
                .positionTitle(a.getPosition().getTitle())
                .startedAt(a.getStartedAt())
                .endedAt(a.getEndedAt())
                .isPrimary(a.getIsPrimary())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
