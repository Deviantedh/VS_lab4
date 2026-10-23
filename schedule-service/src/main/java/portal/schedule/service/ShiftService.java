package portal.schedule.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portal.dto.EmployeeStatus;
import portal.dto.ShiftAction;
import portal.dto.ShiftDto;
import portal.dto.ShiftEmployeeLogDto;
import portal.entity.*;
import portal.schedule.exception.BusinessConflictException;
import portal.schedule.exception.ResourceNotFoundException;
import portal.schedule.repository.EmployeeAbsenceRepository;
import portal.schedule.repository.ShiftEmployeeLogRepository;
import portal.schedule.repository.ShiftRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShiftService {

    private final ShiftRepository shiftRepository;
    private final ShiftEmployeeLogRepository logRepository;
    private final EmployeeAbsenceRepository absenceRepository;
    private final ScheduleService scheduleService;
    private final EmployeeService employeeService;

    @Transactional(readOnly = true)
    public Page<ShiftDto.Response> getAllPaged(Pageable pageable) {
        return shiftRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ShiftDto.Response> getBySchedule(Long scheduleId, Pageable pageable) {
        return shiftRepository.findAllByScheduleId(scheduleId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ShiftDto.Response getById(Long id) {
        return toResponse(findShiftById(id));
    }

    @Transactional
    public ShiftDto.Response create(ShiftDto.Request request) {
        if (!request.getTimeFrom().isBefore(request.getTimeTo())) {
            throw new BusinessConflictException("Время начала смены должно быть строго раньше времени окончания");
        }

        Schedule schedule = scheduleService.findScheduleById(request.getScheduleId());

        if (request.getDate().isBefore(schedule.getDateFrom()) || request.getDate().isAfter(schedule.getDateTo())) {
            throw new BusinessConflictException("Дата смены " + request.getDate() + " выходит за рамки периода расписания (" + schedule.getDateFrom() + " - " + schedule.getDateTo() + ")");
        }

        Shift shift = Shift.builder()
                .schedule(schedule)
                .date(request.getDate())
                .timeFrom(request.getTimeFrom())
                .timeTo(request.getTimeTo())
                .breakMinutes(request.getBreakMinutes() != null ? request.getBreakMinutes() : 0)
                .build();

        return toResponse(shiftRepository.save(shift));
    }

    /**
     * ТРАНЗАКЦИОННЫЙ СЦЕНАРИЙ №1:
     * Назначение сотрудника на смену с обязательной проверкой:
     * 1) Отсутствий/отпусков сотрудника на эту дату;
     * 2) Пересечений с другими сменами сотрудника;
     * 3) Автоматической записью в аудит-лог.
     */
    @Transactional
    public ShiftDto.Response assignEmployee(Long shiftId, Long employeeId, Long assignedById) {
        Shift shift = findShiftById(shiftId);
        Employee employee = employeeService.findEmployeeById(employeeId);

        if (employee.getStatus() == EmployeeStatus.DISMISSED) {
            throw new BusinessConflictException("Нельзя назначить уволенного сотрудника на смену");
        }

        // Проверка: уже назначен на эту смену?
        boolean alreadyAssigned = shift.getEmployees().stream()
                .anyMatch(e -> e.getId().equals(employeeId));
        if (alreadyAssigned) {
            throw new BusinessConflictException("Сотрудник " + employee.getName() + " уже назначен на эту смену");
        }

        // Проверка 1: отпуск или больничный в этот день
        List<EmployeeAbsence> absences = absenceRepository.findOverlappingAbsences(
                employeeId, shift.getDate(), shift.getDate()
        );
        if (!absences.isEmpty()) {
            EmployeeAbsence absence = absences.get(0);
            throw new BusinessConflictException("Сотрудник " + employee.getName() + " недоступен: на дату " + shift.getDate() + " зафиксировано отсутствие (" + absence.getType() + ")");
        }

        // Проверка 2: пересекающиеся смены в это же время
        List<Shift> overlapping = shiftRepository.findOverlappingShiftsForEmployee(
                employeeId, shift.getDate(), shift.getTimeFrom(), shift.getTimeTo(), shift.getId()
        );
        if (!overlapping.isEmpty()) {
            throw new BusinessConflictException("Клонирование сотрудников не поддерживается: у сотрудника уже есть пересекающаяся смена в это время!");
        }

        // Назначаем
        shift.getEmployees().add(employee);

        // Пишем аудит-лог
        Employee assignedBy = null;
        if (assignedById != null) {
            assignedBy = employeeService.findEmployeeById(assignedById);
        }

        ShiftEmployeeLog log = ShiftEmployeeLog.builder()
                .shift(shift)
                .employee(employee)
                .action(ShiftAction.ASSIGNED)
                .createdBy(assignedBy)
                .build();
        logRepository.save(log);

        return toResponse(shiftRepository.save(shift));
    }

    /**
     * Снятие сотрудника со смены с записью в аудит-лог.
     */
    @Transactional
    public ShiftDto.Response removeEmployee(Long shiftId, Long employeeId, Long removedById) {
        Shift shift = findShiftById(shiftId);
        Employee employee = employeeService.findEmployeeById(employeeId);

        boolean removed = shift.getEmployees().removeIf(e -> e.getId().equals(employeeId));
        if (!removed) {
            throw new BusinessConflictException("Сотрудник " + employee.getName() + " не был назначен на эту смену");
        }

        Employee removedBy = null;
        if (removedById != null) {
            removedBy = employeeService.findEmployeeById(removedById);
        }

        ShiftEmployeeLog log = ShiftEmployeeLog.builder()
                .shift(shift)
                .employee(employee)
                .action(ShiftAction.REMOVED)
                .createdBy(removedBy)
                .build();
        logRepository.save(log);

        return toResponse(shiftRepository.save(shift));
    }

    @Transactional(readOnly = true)
    public List<ShiftEmployeeLogDto> getLogs(Long shiftId) {
        findShiftById(shiftId);
        return logRepository.findAllByShiftIdOrderByCreatedAtDesc(shiftId).stream()
                .map(this::toLogDto)
                .toList();
    }

    @Transactional
    public void delete(Long id) {
        Shift shift = findShiftById(id);
        for (AttendanceRecord record : shift.getAttendanceRecords()) {
            record.setShift(null);
        }
        shiftRepository.delete(shift);
    }

    public Shift findShiftById(Long id) {
        return shiftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Смена с ID " + id + " не найдена в расписании"));
    }

    public ShiftDto.Response toResponse(Shift shift) {
        return ShiftDto.Response.builder()
                .id(shift.getId())
                .scheduleId(shift.getSchedule().getId())
                .date(shift.getDate())
                .timeFrom(shift.getTimeFrom())
                .timeTo(shift.getTimeTo())
                .breakMinutes(shift.getBreakMinutes())
                .assignedEmployees(shift.getEmployees().stream()
                        .map(employeeService::toResponse)
                        .toList())
                .createdAt(shift.getCreatedAt())
                .updatedAt(shift.getUpdatedAt())
                .build();
    }

    private ShiftEmployeeLogDto toLogDto(ShiftEmployeeLog log) {
        return ShiftEmployeeLogDto.builder()
                .id(log.getId())
                .shiftId(log.getShift().getId())
                .employeeId(log.getEmployee().getId())
                .employeeName(log.getEmployee().getName())
                .action(log.getAction())
                .createdById(log.getCreatedBy() != null ? log.getCreatedBy().getId() : null)
                .createdByName(log.getCreatedBy() != null ? log.getCreatedBy().getName() : null)
                .createdAt(log.getCreatedAt())
                .build();
    }
}
