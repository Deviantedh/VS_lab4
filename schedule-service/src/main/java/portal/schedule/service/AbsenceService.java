package portal.schedule.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portal.dto.EmployeeAbsenceDto;
import portal.entity.Employee;
import portal.entity.EmployeeAbsence;
import portal.entity.Request;
import portal.schedule.exception.BusinessConflictException;
import portal.schedule.exception.ResourceNotFoundException;
import portal.schedule.repository.EmployeeAbsenceRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AbsenceService {

    private final EmployeeAbsenceRepository absenceRepository;
    private final EmployeeService employeeService;

    @Transactional(readOnly = true)
    public List<EmployeeAbsenceDto.Response> getAll() {
        return absenceRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EmployeeAbsenceDto.Response> getByEmployee(Long employeeId) {
        employeeService.findEmployeeById(employeeId);
        return absenceRepository.findAllByEmployeeId(employeeId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public EmployeeAbsenceDto.Response create(EmployeeAbsenceDto.Request request) {
        if (request.getDateFrom().isAfter(request.getDateTo())) {
            throw new BusinessConflictException("Дата начала отсутствия не может быть позже даты окончания");
        }

        Employee employee = employeeService.findEmployeeById(request.getEmployeeId());

        EmployeeAbsence absence = EmployeeAbsence.builder()
                .employee(employee)
                .type(request.getType())
                .dateFrom(request.getDateFrom())
                .dateTo(request.getDateTo())
                .comment(request.getComment())
                .build();

        return toResponse(absenceRepository.save(absence));
    }

    public EmployeeAbsence findAbsenceById(Long id) {
        return absenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Запись об отсутствии с ID " + id + " не найдена"));
    }

    public EmployeeAbsenceDto.Response toResponse(EmployeeAbsence a) {
        return EmployeeAbsenceDto.Response.builder()
                .id(a.getId())
                .employeeId(a.getEmployee().getId())
                .employeeName(a.getEmployee().getName())
                .requestId(a.getRequest() != null ? a.getRequest().getId() : null)
                .type(a.getType())
                .dateFrom(a.getDateFrom())
                .dateTo(a.getDateTo())
                .comment(a.getComment())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
