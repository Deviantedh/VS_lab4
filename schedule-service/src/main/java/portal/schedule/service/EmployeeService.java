package portal.schedule.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portal.dto.EmployeeAssignmentDto;
import portal.dto.EmployeeDto;
import portal.dto.EmployeeStatus;
import portal.entity.*;
import portal.schedule.exception.BusinessConflictException;
import portal.schedule.exception.ResourceNotFoundException;
import portal.schedule.repository.EmployeeAssignmentRepository;
import portal.schedule.repository.EmployeeRepository;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeAssignmentRepository assignmentRepository;
    private final BranchService branchService;
    private final PositionService positionService;

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

        return toResponse(employeeRepository.save(employee));
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

        return toResponse(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeDto.Response dismiss(Long id) {
        Employee employee = findEmployeeById(id);
        if (employee.getStatus() == EmployeeStatus.DISMISSED) {
            throw new BusinessConflictException("Сотрудник " + employee.getName() + " уже уволен");
        }
        employee.setStatus(EmployeeStatus.DISMISSED);
        employee.setDismissalDate(LocalDate.now());
        return toResponse(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeDto.Response rehire(Long id) {
        Employee employee = findEmployeeById(id);
        if (employee.getStatus() != EmployeeStatus.DISMISSED) {
            throw new BusinessConflictException("Сотрудник " + employee.getName() + " не является уволенным");
        }
        employee.setStatus(EmployeeStatus.ACTIVE);
        employee.setDismissalDate(null);
        return toResponse(employeeRepository.save(employee));
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
        return toAssignmentResponse(saved);
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
