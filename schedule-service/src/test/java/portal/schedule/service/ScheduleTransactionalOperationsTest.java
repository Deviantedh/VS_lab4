package portal.schedule.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import portal.dto.AbsenceType;
import portal.dto.RequestDto;
import portal.dto.RequestStatus;
import portal.dto.RequestType;
import portal.dto.ShiftAction;
import portal.entity.*;
import portal.schedule.exception.BusinessConflictException;
import portal.schedule.repository.EmployeeAbsenceRepository;
import portal.schedule.repository.RequestRepository;
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
class ScheduleTransactionalOperationsTest {

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private EmployeeAbsenceRepository absenceRepository;

    @Mock
    private ShiftRepository shiftRepository;

    @Mock
    private ShiftEmployeeLogRepository shiftLogRepository;

    @Mock
    private EmployeeService employeeService;

    private RequestService requestService;
    private ObjectMapper objectMapper;

    private Employee employee;
    private Employee manager;
    private Request vacationRequest;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        requestService = new RequestService(
                requestRepository,
                absenceRepository,
                shiftRepository,
                shiftLogRepository,
                employeeService,
                objectMapper
        );

        employee = Employee.builder()
                .id(10L)
                .name("Леша Разработчик")
                .phone("+79991112233")
                .build();

        manager = Employee.builder()
                .id(1L)
                .name("Менеджер Проекта")
                .phone("+79998889900")
                .build();

        vacationRequest = Request.builder()
                .id(100L)
                .employee(employee)
                .type(RequestType.VACATION)
                .status(RequestStatus.PENDING)
                .requestData("{\"dateFrom\":\"2026-10-10\",\"dateTo\":\"2026-10-24\"}")
                .build();
    }

    @Test
    @DisplayName("Transactional approval of vacation: status=APPROVED, absence created, employee removed from conflicting shifts with audit log")
    void testApproveVacationTransactionalScenario() {
        RequestDto.Process processDto = RequestDto.Process.builder()
                .status(RequestStatus.APPROVED)
                .processedById(1L)
                .resolutionComment("Отпуск согласован")
                .build();

        Shift conflictingShift = Shift.builder()
                .id(50L)
                .date(LocalDate.of(2026, 10, 15))
                .timeFrom(LocalTime.of(9, 0))
                .timeTo(LocalTime.of(18, 0))
                .employees(new ArrayList<>(List.of(employee)))
                .build();

        when(requestRepository.findById(100L)).thenReturn(Optional.of(vacationRequest));
        when(employeeService.findEmployeeById(1L)).thenReturn(manager);
        when(shiftRepository.findShiftsForEmployeeBetweenDates(10L, LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 24)))
                .thenReturn(List.of(conflictingShift));
        when(requestRepository.save(any(Request.class))).thenAnswer(i -> i.getArgument(0));

        RequestDto.Response response = requestService.processRequest(100L, processDto);

        // 1. Verify Request status
        assertThat(response.getStatus()).isEqualTo(RequestStatus.APPROVED);
        assertThat(vacationRequest.getStatus()).isEqualTo(RequestStatus.APPROVED);
        assertThat(vacationRequest.getProcessedBy()).isEqualTo(manager);
        assertThat(vacationRequest.getResolutionComment()).isEqualTo("Отпуск согласован");

        // 2. Verify Absence created
        ArgumentCaptor<EmployeeAbsence> absenceCaptor = ArgumentCaptor.forClass(EmployeeAbsence.class);
        verify(absenceRepository).save(absenceCaptor.capture());
        EmployeeAbsence savedAbsence = absenceCaptor.getValue();
        assertThat(savedAbsence.getEmployee()).isEqualTo(employee);
        assertThat(savedAbsence.getType()).isEqualTo(AbsenceType.VACATION);
        assertThat(savedAbsence.getDateFrom()).isEqualTo(LocalDate.of(2026, 10, 10));
        assertThat(savedAbsence.getDateTo()).isEqualTo(LocalDate.of(2026, 10, 24));

        // 3. Verify Employee removed from conflicting shift
        assertThat(conflictingShift.getEmployees()).doesNotContain(employee);
        verify(shiftRepository).save(conflictingShift);

        // 4. Verify Audit Log entry
        ArgumentCaptor<ShiftEmployeeLog> logCaptor = ArgumentCaptor.forClass(ShiftEmployeeLog.class);
        verify(shiftLogRepository).save(logCaptor.capture());
        ShiftEmployeeLog log = logCaptor.getValue();
        assertThat(log.getAction()).isEqualTo(ShiftAction.REMOVED);
        assertThat(log.getEmployee()).isEqualTo(employee);
        assertThat(log.getCreatedBy()).isEqualTo(manager);
    }

    @Test
    @DisplayName("Transactional rejection of vacation: status=REJECTED, NO absence record created, NO shifts modified")
    void testRejectVacationTransactionalScenario() {
        RequestDto.Process processDto = RequestDto.Process.builder()
                .status(RequestStatus.REJECTED)
                .processedById(1L)
                .resolutionComment("Недостаточно людей на смене в эти даты")
                .build();

        when(requestRepository.findById(100L)).thenReturn(Optional.of(vacationRequest));
        when(employeeService.findEmployeeById(1L)).thenReturn(manager);
        when(requestRepository.save(any(Request.class))).thenAnswer(i -> i.getArgument(0));

        RequestDto.Response response = requestService.processRequest(100L, processDto);

        // 1. Verify Request status
        assertThat(response.getStatus()).isEqualTo(RequestStatus.REJECTED);
        assertThat(vacationRequest.getStatus()).isEqualTo(RequestStatus.REJECTED);

        // 2. Absence should NOT be created
        verify(absenceRepository, never()).save(any());

        // 3. Shifts should NOT be touched
        verify(shiftRepository, never()).findShiftsForEmployeeBetweenDates(any(), any(), any());
        verify(shiftLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Processing already processed request throws BusinessConflictException")
    void testProcessAlreadyProcessedRequest() {
        vacationRequest.setStatus(RequestStatus.APPROVED);
        when(requestRepository.findById(100L)).thenReturn(Optional.of(vacationRequest));

        RequestDto.Process processDto = RequestDto.Process.builder()
                .status(RequestStatus.APPROVED)
                .build();

        assertThatThrownBy(() -> requestService.processRequest(100L, processDto))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("уже была обработана ранее");

        verify(absenceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Processing request with inverted dates (dateFrom > dateTo) throws BusinessConflictException")
    void testProcessWithInvalidDates() {
        vacationRequest.setRequestData("{\"dateFrom\":\"2026-10-25\",\"dateTo\":\"2026-10-10\"}");
        when(requestRepository.findById(100L)).thenReturn(Optional.of(vacationRequest));
        when(employeeService.findEmployeeById(1L)).thenReturn(manager);

        RequestDto.Process processDto = RequestDto.Process.builder()
                .status(RequestStatus.APPROVED)
                .processedById(1L)
                .build();

        assertThatThrownBy(() -> requestService.processRequest(100L, processDto))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("не может быть позже даты окончания");

        verify(absenceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Processing request with missing requestData parameters throws BusinessConflictException")
    void testProcessWithMissingRequestData() {
        vacationRequest.setRequestData("{}");
        when(requestRepository.findById(100L)).thenReturn(Optional.of(vacationRequest));
        when(employeeService.findEmployeeById(1L)).thenReturn(manager);

        RequestDto.Process processDto = RequestDto.Process.builder()
                .status(RequestStatus.APPROVED)
                .processedById(1L)
                .build();

        assertThatThrownBy(() -> requestService.processRequest(100L, processDto))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("отсутствуют обязательные поля dateFrom или dateTo");

        verify(absenceRepository, never()).save(any());
    }
}
