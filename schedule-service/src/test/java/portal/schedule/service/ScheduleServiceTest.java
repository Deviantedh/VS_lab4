package portal.schedule.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import portal.dto.ScheduleDto;
import portal.entity.Branch;
import portal.entity.Employee;
import portal.entity.Schedule;
import portal.schedule.exception.BusinessConflictException;
import portal.schedule.exception.ResourceNotFoundException;
import portal.schedule.repository.ScheduleRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private BranchService branchService;

    @Mock
    private EmployeeService employeeService;

    private ScheduleService scheduleService;

    private Branch branch;
    private Employee manager;
    private Schedule schedule;

    @BeforeEach
    void setUp() {
        scheduleService = new ScheduleService(scheduleRepository, branchService, employeeService);

        branch = Branch.builder()
                .id(1L)
                .name("Центральный офис")
                .address("г. Москва, ул. Ленина, д. 1")
                .build();

        manager = Employee.builder()
                .id(10L)
                .name("Ольга Администратор")
                .phone("+79993334455")
                .build();

        schedule = Schedule.builder()
                .id(5L)
                .branch(branch)
                .dateFrom(LocalDate.of(2026, 11, 1))
                .dateTo(LocalDate.of(2026, 11, 30))
                .createdBy(manager)
                .build();
    }

    @Test
    @DisplayName("Get schedules by branch ID returns list of responses")
    void testGetByBranch() {
        when(scheduleRepository.findAllByBranchId(1L)).thenReturn(List.of(schedule));

        List<ScheduleDto.Response> responses = scheduleService.getByBranch(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getId()).isEqualTo(5L);
        assertThat(responses.get(0).getBranchName()).isEqualTo("Центральный офис");
    }

    @Test
    @DisplayName("Get schedule by ID returns response")
    void testGetById() {
        when(scheduleRepository.findById(5L)).thenReturn(Optional.of(schedule));

        ScheduleDto.Response response = scheduleService.getById(5L);

        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getDateFrom()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(response.getDateTo()).isEqualTo(LocalDate.of(2026, 11, 30));
    }

    @Test
    @DisplayName("Get non-existent schedule by ID throws ResourceNotFoundException")
    void testGetByIdNotFoundThrowsException() {
        when(scheduleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleService.getById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("не найдено");
    }

    @Test
    @DisplayName("Create schedule with valid dates succeeds")
    void testCreateScheduleSuccess() {
        ScheduleDto.Request request = ScheduleDto.Request.builder()
                .branchId(1L)
                .dateFrom(LocalDate.of(2026, 11, 1))
                .dateTo(LocalDate.of(2026, 11, 30))
                .createdById(10L)
                .build();

        when(branchService.findBranchById(1L)).thenReturn(branch);
        when(employeeService.findEmployeeById(10L)).thenReturn(manager);
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(i -> {
            Schedule s = i.getArgument(0);
            s.setId(20L);
            return s;
        });

        ScheduleDto.Response response = scheduleService.create(request);

        assertThat(response.getId()).isEqualTo(20L);
        assertThat(response.getBranchId()).isEqualTo(1L);
        assertThat(response.getDateFrom()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(response.getDateTo()).isEqualTo(LocalDate.of(2026, 11, 30));
        assertThat(response.getCreatedById()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Create schedule with dateFrom after dateTo throws BusinessConflictException")
    void testCreateScheduleInvertedDatesThrowsException() {
        ScheduleDto.Request request = ScheduleDto.Request.builder()
                .branchId(1L)
                .dateFrom(LocalDate.of(2026, 11, 30))
                .dateTo(LocalDate.of(2026, 11, 1))
                .build();

        assertThatThrownBy(() -> scheduleService.create(request))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("дата начала не может быть позже даты окончания расписания");

        verify(scheduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Delete schedule by ID deletes entity")
    void testDeleteSchedule() {
        when(scheduleRepository.findById(5L)).thenReturn(Optional.of(schedule));

        scheduleService.delete(5L);

        verify(scheduleRepository).delete(schedule);
    }
}
