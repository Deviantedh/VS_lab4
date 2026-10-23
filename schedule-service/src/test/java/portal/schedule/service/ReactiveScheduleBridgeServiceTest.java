package portal.schedule.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import portal.dto.EmployeeDto;
import portal.dto.EmployeeStatus;
import portal.dto.ShiftDto;
import portal.entity.Schedule;
import portal.entity.Shift;
import portal.schedule.client.EmployeeClient;
import portal.schedule.exception.ResourceNotFoundException;
import portal.schedule.repository.ShiftRepository;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactiveScheduleBridgeServiceTest {

    @Mock
    private ShiftRepository shiftRepository;

    @Mock
    private ShiftService shiftService;

    @Mock
    private EmployeeClient employeeClient;

    @InjectMocks
    private ReactiveScheduleBridgeService bridgeService;

    private Shift shift;
    private Schedule schedule;

    @BeforeEach
    void setUp() {
        schedule = Schedule.builder().id(1L).build();
        shift = Shift.builder()
                .id(10L)
                .schedule(schedule)
                .date(LocalDate.of(2026, 10, 15))
                .timeFrom(LocalTime.of(9, 0))
                .timeTo(LocalTime.of(18, 0))
                .breakMinutes(60)
                .employees(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("assignEmployeeToShiftReactive: successfully verifies employee via Feign and delegates to shiftService")
    void testAssignEmployeeToShiftReactiveSuccess() {
        EmployeeDto.Response employeeResponse = EmployeeDto.Response.builder()
                .id(5L)
                .name("Иван Разработчик")
                .status(EmployeeStatus.ACTIVE)
                .build();

        ShiftDto.Response expectedShiftResponse = ShiftDto.Response.builder()
                .id(10L)
                .scheduleId(1L)
                .date(LocalDate.of(2026, 10, 15))
                .build();

        when(employeeClient.getEmployeeById(5L)).thenReturn(employeeResponse);
        when(shiftService.assignEmployee(10L, 5L, null)).thenReturn(expectedShiftResponse);

        Mono<ShiftDto.Response> result = bridgeService.assignEmployeeToShiftReactive(10L, 5L);

        StepVerifier.create(result)
                .assertNext(resp -> {
                    assertThat(resp.getId()).isEqualTo(10L);
                    assertThat(resp.getScheduleId()).isEqualTo(1L);
                })
                .verifyComplete();

        verify(employeeClient).getEmployeeById(5L);
        verify(shiftService).assignEmployee(10L, 5L, null);
    }

    @Test
    @DisplayName("assignEmployeeToShiftReactive: throws ResourceNotFoundException when employeeClient returns null")
    void testAssignEmployeeToShiftReactiveEmployeeNull() {
        when(employeeClient.getEmployeeById(99L)).thenReturn(null);

        Mono<ShiftDto.Response> result = bridgeService.assignEmployeeToShiftReactive(10L, 99L);

        StepVerifier.create(result)
                .expectErrorMatches(t -> t instanceof ResourceNotFoundException
                        && t.getMessage().contains("Сотрудник с ID 99 не найден"))
                .verify();

        verify(shiftService, never()).assignEmployee(any(), any(), any());
    }

    @Test
    @DisplayName("getShiftsReactive with date filter: verify Mono<Page<ShiftDto.Response>> emissions via StepVerifier")
    void testGetShiftsReactiveWithDates() {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 31);
        Page<Shift> page = new PageImpl<>(List.of(shift));

        when(shiftRepository.findByDateBetween(eq(from), eq(to), any(Pageable.class))).thenReturn(page);

        Mono<Page<ShiftDto.Response>> result = bridgeService.getShiftsReactive(from, to, 0, 10);

        StepVerifier.create(result)
                .assertNext(p -> {
                    assertThat(p.getContent()).hasSize(1);
                    assertThat(p.getContent().get(0).getId()).isEqualTo(10L);
                    assertThat(p.getContent().get(0).getDate()).isEqualTo(LocalDate.of(2026, 10, 15));
                })
                .verifyComplete();

        verify(shiftRepository).findByDateBetween(eq(from), eq(to), any(Pageable.class));
    }

    @Test
    @DisplayName("getShiftsReactive without date filter: verify findAll fallback via StepVerifier")
    void testGetShiftsReactiveWithoutDates() {
        Page<Shift> page = new PageImpl<>(List.of(shift));
        when(shiftRepository.findAll(any(Pageable.class))).thenReturn(page);

        Mono<Page<ShiftDto.Response>> result = bridgeService.getShiftsReactive(null, null, 0, 10);

        StepVerifier.create(result)
                .assertNext(p -> {
                    assertThat(p.getContent()).hasSize(1);
                    assertThat(p.getContent().get(0).getId()).isEqualTo(10L);
                })
                .verifyComplete();

        verify(shiftRepository).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("getShiftByIdReactive: verify successful item emission with StepVerifier")
    void testGetShiftByIdReactiveFound() {
        when(shiftRepository.findById(10L)).thenReturn(Optional.of(shift));

        Mono<ShiftDto.Response> result = bridgeService.getShiftByIdReactive(10L);

        StepVerifier.create(result)
                .assertNext(resp -> {
                    assertThat(resp.getId()).isEqualTo(10L);
                    assertThat(resp.getScheduleId()).isEqualTo(1L);
                    assertThat(resp.getTimeFrom()).isEqualTo(LocalTime.of(9, 0));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getShiftByIdReactive: verify ResourceNotFoundException when shift does not exist")
    void testGetShiftByIdReactiveNotFound() {
        when(shiftRepository.findById(999L)).thenReturn(Optional.empty());

        Mono<ShiftDto.Response> result = bridgeService.getShiftByIdReactive(999L);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof ResourceNotFoundException
                        && throwable.getMessage().contains("Смена с ID 999 не найдена"))
                .verify();
    }
}
