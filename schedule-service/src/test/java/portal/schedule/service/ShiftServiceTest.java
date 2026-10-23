package portal.schedule.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import portal.dto.*;
import portal.entity.*;
import portal.schedule.exception.BusinessConflictException;
import portal.schedule.exception.ResourceNotFoundException;
import portal.schedule.repository.EmployeeAbsenceRepository;
import portal.schedule.repository.ShiftEmployeeLogRepository;
import portal.schedule.repository.ShiftRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShiftServiceTest {

    @Mock
    private ShiftRepository shiftRepository;

    @Mock
    private ShiftEmployeeLogRepository logRepository;

    @Mock
    private EmployeeAbsenceRepository absenceRepository;

    @Mock
    private ScheduleService scheduleService;

    @Mock
    private EmployeeService employeeService;

    private ShiftService shiftService;

    private Schedule schedule;
    private Employee activeEmployee;
    private Employee dismissedEmployee;
    private Employee manager;
    private Shift shift;

    @BeforeEach
    void setUp() {
        shiftService = new ShiftService(
                shiftRepository,
                logRepository,
                absenceRepository,
                scheduleService,
                employeeService
        );

        schedule = Schedule.builder()
                .id(1L)
                .dateFrom(LocalDate.of(2026, 10, 1))
                .dateTo(LocalDate.of(2026, 10, 31))
                .build();

        activeEmployee = Employee.builder()
                .id(10L)
                .name("Иван Разработчик")
                .phone("+79991112233")
                .status(EmployeeStatus.ACTIVE)
                .build();

        dismissedEmployee = Employee.builder()
                .id(11L)
                .name("Уволенный Сотрудник")
                .phone("+79991112244")
                .status(EmployeeStatus.DISMISSED)
                .build();

        manager = Employee.builder()
                .id(1L)
                .name("Анна Менеджер")
                .phone("+79998889900")
                .build();

        shift = Shift.builder()
                .id(50L)
                .schedule(schedule)
                .date(LocalDate.of(2026, 10, 15))
                .timeFrom(LocalTime.of(9, 0))
                .timeTo(LocalTime.of(18, 0))
                .breakMinutes(60)
                .employees(new ArrayList<>())
                .attendanceRecords(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Create shift with valid times and date inside schedule period succeeds")
    void testCreateShiftSuccess() {
        ShiftDto.Request request = ShiftDto.Request.builder()
                .scheduleId(1L)
                .date(LocalDate.of(2026, 10, 15))
                .timeFrom(LocalTime.of(9, 0))
                .timeTo(LocalTime.of(18, 0))
                .breakMinutes(45)
                .build();

        when(scheduleService.findScheduleById(1L)).thenReturn(schedule);
        when(shiftRepository.save(any(Shift.class))).thenAnswer(i -> {
            Shift s = i.getArgument(0);
            s.setId(100L);
            return s;
        });

        ShiftDto.Response response = shiftService.create(request);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getDate()).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(response.getTimeFrom()).isEqualTo(LocalTime.of(9, 0));
        assertThat(response.getTimeTo()).isEqualTo(LocalTime.of(18, 0));
        assertThat(response.getBreakMinutes()).isEqualTo(45);
    }

    @Test
    @DisplayName("Create shift with invalid time (timeFrom >= timeTo) throws BusinessConflictException")
    void testCreateShiftInvalidTimeThrowsException() {
        ShiftDto.Request request = ShiftDto.Request.builder()
                .scheduleId(1L)
                .date(LocalDate.of(2026, 10, 15))
                .timeFrom(LocalTime.of(18, 0))
                .timeTo(LocalTime.of(9, 0))
                .build();

        assertThatThrownBy(() -> shiftService.create(request))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("Время начала смены должно быть строго раньше времени окончания");

        verify(shiftRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create shift outside schedule date bounds throws BusinessConflictException")
    void testCreateShiftDateOutOfScheduleRangeThrowsException() {
        ShiftDto.Request request = ShiftDto.Request.builder()
                .scheduleId(1L)
                .date(LocalDate.of(2026, 11, 5))
                .timeFrom(LocalTime.of(9, 0))
                .timeTo(LocalTime.of(18, 0))
                .build();

        when(scheduleService.findScheduleById(1L)).thenReturn(schedule);

        assertThatThrownBy(() -> shiftService.create(request))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("выходит за рамки периода расписания");

        verify(shiftRepository, never()).save(any());
    }

    @Test
    @DisplayName("Assign employee to shift succeeds: employee added, audit log recorded with ASSIGNED")
    void testAssignEmployeeSuccess() {
        when(shiftRepository.findById(50L)).thenReturn(Optional.of(shift));
        when(employeeService.findEmployeeById(10L)).thenReturn(activeEmployee);
        when(employeeService.findEmployeeById(1L)).thenReturn(manager);
        when(absenceRepository.findOverlappingAbsences(10L, shift.getDate(), shift.getDate()))
                .thenReturn(List.of());
        when(shiftRepository.findOverlappingShiftsForEmployee(10L, shift.getDate(), shift.getTimeFrom(), shift.getTimeTo(), 50L))
                .thenReturn(List.of());
        when(shiftRepository.save(any(Shift.class))).thenAnswer(i -> i.getArgument(0));

        ShiftDto.Response response = shiftService.assignEmployee(50L, 10L, 1L);

        assertThat(shift.getEmployees()).contains(activeEmployee);
        verify(shiftRepository).save(shift);

        ArgumentCaptor<ShiftEmployeeLog> logCaptor = ArgumentCaptor.forClass(ShiftEmployeeLog.class);
        verify(logRepository).save(logCaptor.capture());
        ShiftEmployeeLog log = logCaptor.getValue();
        assertThat(log.getAction()).isEqualTo(ShiftAction.ASSIGNED);
        assertThat(log.getEmployee()).isEqualTo(activeEmployee);
        assertThat(log.getCreatedBy()).isEqualTo(manager);
    }

    @Test
    @DisplayName("Assign dismissed employee throws BusinessConflictException")
    void testAssignDismissedEmployeeThrowsException() {
        when(shiftRepository.findById(50L)).thenReturn(Optional.of(shift));
        when(employeeService.findEmployeeById(11L)).thenReturn(dismissedEmployee);

        assertThatThrownBy(() -> shiftService.assignEmployee(50L, 11L, 1L))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("Нельзя назначить уволенного сотрудника на смену");

        verify(logRepository, never()).save(any());
    }

    @Test
    @DisplayName("Assign employee already assigned to the shift throws BusinessConflictException")
    void testAssignAlreadyAssignedEmployeeThrowsException() {
        shift.getEmployees().add(activeEmployee);
        when(shiftRepository.findById(50L)).thenReturn(Optional.of(shift));
        when(employeeService.findEmployeeById(10L)).thenReturn(activeEmployee);

        assertThatThrownBy(() -> shiftService.assignEmployee(50L, 10L, 1L))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("уже назначен на эту смену");

        verify(logRepository, never()).save(any());
    }

    @Test
    @DisplayName("Assign employee with overlapping absence throws BusinessConflictException")
    void testAssignEmployeeWithOverlappingAbsenceThrowsException() {
        EmployeeAbsence absence = EmployeeAbsence.builder()
                .employee(activeEmployee)
                .type(AbsenceType.VACATION)
                .dateFrom(LocalDate.of(2026, 10, 10))
                .dateTo(LocalDate.of(2026, 10, 20))
                .build();

        when(shiftRepository.findById(50L)).thenReturn(Optional.of(shift));
        when(employeeService.findEmployeeById(10L)).thenReturn(activeEmployee);
        when(absenceRepository.findOverlappingAbsences(10L, shift.getDate(), shift.getDate()))
                .thenReturn(List.of(absence));

        assertThatThrownBy(() -> shiftService.assignEmployee(50L, 10L, 1L))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("недоступен: на дату 2026-10-15 зафиксировано отсутствие (VACATION)");

        verify(logRepository, never()).save(any());
    }

    @Test
    @DisplayName("Assign employee with overlapping shift throws BusinessConflictException")
    void testAssignEmployeeWithOverlappingShiftThrowsException() {
        Shift otherShift = Shift.builder()
                .id(51L)
                .date(shift.getDate())
                .timeFrom(LocalTime.of(12, 0))
                .timeTo(LocalTime.of(20, 0))
                .build();

        when(shiftRepository.findById(50L)).thenReturn(Optional.of(shift));
        when(employeeService.findEmployeeById(10L)).thenReturn(activeEmployee);
        when(absenceRepository.findOverlappingAbsences(10L, shift.getDate(), shift.getDate()))
                .thenReturn(List.of());
        when(shiftRepository.findOverlappingShiftsForEmployee(10L, shift.getDate(), shift.getTimeFrom(), shift.getTimeTo(), 50L))
                .thenReturn(List.of(otherShift));

        assertThatThrownBy(() -> shiftService.assignEmployee(50L, 10L, 1L))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("Клонирование сотрудников не поддерживается: у сотрудника уже есть пересекающаяся смена в это время!");

        verify(logRepository, never()).save(any());
    }

    @Test
    @DisplayName("Remove employee from shift succeeds: employee removed, audit log recorded with REMOVED")
    void testRemoveEmployeeSuccess() {
        shift.getEmployees().add(activeEmployee);

        when(shiftRepository.findById(50L)).thenReturn(Optional.of(shift));
        when(employeeService.findEmployeeById(10L)).thenReturn(activeEmployee);
        when(employeeService.findEmployeeById(1L)).thenReturn(manager);
        when(shiftRepository.save(any(Shift.class))).thenAnswer(i -> i.getArgument(0));

        ShiftDto.Response response = shiftService.removeEmployee(50L, 10L, 1L);

        assertThat(shift.getEmployees()).doesNotContain(activeEmployee);
        verify(shiftRepository).save(shift);

        ArgumentCaptor<ShiftEmployeeLog> logCaptor = ArgumentCaptor.forClass(ShiftEmployeeLog.class);
        verify(logRepository).save(logCaptor.capture());
        ShiftEmployeeLog log = logCaptor.getValue();
        assertThat(log.getAction()).isEqualTo(ShiftAction.REMOVED);
        assertThat(log.getEmployee()).isEqualTo(activeEmployee);
        assertThat(log.getCreatedBy()).isEqualTo(manager);
    }

    @Test
    @DisplayName("Remove employee not assigned to the shift throws BusinessConflictException")
    void testRemoveUnassignedEmployeeThrowsException() {
        when(shiftRepository.findById(50L)).thenReturn(Optional.of(shift));
        when(employeeService.findEmployeeById(10L)).thenReturn(activeEmployee);

        assertThatThrownBy(() -> shiftService.removeEmployee(50L, 10L, 1L))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("не был назначен на эту смену");

        verify(logRepository, never()).save(any());
    }

    @Test
    @DisplayName("Get shift audit logs ordered by date desc")
    void testGetLogs() {
        ShiftEmployeeLog log1 = ShiftEmployeeLog.builder()
                .id(1L)
                .shift(shift)
                .employee(activeEmployee)
                .action(ShiftAction.ASSIGNED)
                .build();

        when(shiftRepository.findById(50L)).thenReturn(Optional.of(shift));
        when(logRepository.findAllByShiftIdOrderByCreatedAtDesc(50L)).thenReturn(List.of(log1));

        List<ShiftEmployeeLogDto> logs = shiftService.getLogs(50L);

        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getAction()).isEqualTo(ShiftAction.ASSIGNED);
        assertThat(logs.get(0).getEmployeeId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Delete shift unlinks attendance records and deletes shift")
    void testDeleteShift() {
        AttendanceRecord att = AttendanceRecord.builder().id(99L).shift(shift).build();
        shift.getAttendanceRecords().add(att);

        when(shiftRepository.findById(50L)).thenReturn(Optional.of(shift));

        shiftService.delete(50L);

        assertThat(att.getShift()).isNull();
        verify(shiftRepository).delete(shift);
    }
}
