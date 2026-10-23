package portal.schedule.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import portal.dto.EmployeeDto;
import portal.dto.EmployeeStatus;
import portal.dto.ShiftDto;
import portal.entity.Shift;
import portal.schedule.repository.ShiftRepository;
import portal.schedule.service.ReactiveScheduleBridgeService;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeClientCircuitBreakerTest {

    @Mock
    private ShiftRepository shiftRepository;

    @Mock
    private portal.schedule.service.ShiftService shiftService;

    @Mock
    private EmployeeClient employeeClient;

    private final EmployeeClientFallback fallback = new EmployeeClientFallback();

    @Test
    @DisplayName("EmployeeClientFallback returns valid fallback profile when employee-service is unavailable")
    void testFallbackDirectly() {
        Long employeeId = 42L;

        EmployeeDto.Response fallbackResponse = fallback.getEmployeeById(employeeId);

        assertThat(fallbackResponse).isNotNull();
        assertThat(fallbackResponse.getId()).isEqualTo(employeeId);
        assertThat(fallbackResponse.getName()).contains("Employee Service временно недоступен");
        assertThat(fallbackResponse.getPhone()).isEqualTo("+0000000000");
        assertThat(fallbackResponse.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
        assertThat(fallbackResponse.getHireDate()).isNotNull();
    }

    @Test
    @DisplayName("ReactiveScheduleBridgeService completes successfully when Circuit Breaker returns fallback")
    void testBridgeServiceWithCircuitBreakerFallback() {
        Long shiftId = 1L;
        Long employeeId = 42L;

        ShiftDto.Response expectedResponse = ShiftDto.Response.builder()
                .id(shiftId)
                .date(LocalDate.of(2026, 10, 20))
                .build();

        // Simulate Circuit Breaker invoking fallback due to employee-service downtime
        when(employeeClient.getEmployeeById(employeeId))
                .thenReturn(fallback.getEmployeeById(employeeId));
        when(shiftService.assignEmployee(shiftId, employeeId, null))
                .thenReturn(expectedResponse);

        ReactiveScheduleBridgeService bridgeService =
                new ReactiveScheduleBridgeService(shiftRepository, shiftService, employeeClient);

        Mono<ShiftDto.Response> result = bridgeService.assignEmployeeToShiftReactive(shiftId, employeeId);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getId()).isEqualTo(shiftId);
                    assertThat(response.getDate()).isEqualTo(LocalDate.of(2026, 10, 20));
                })
                .verifyComplete();

        verify(employeeClient).getEmployeeById(employeeId);
        verify(shiftService).assignEmployee(shiftId, employeeId, null);
    }
}
