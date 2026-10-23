package portal.schedule.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import portal.dto.AttendanceRecordDto;
import portal.dto.BranchDto;

import static org.assertj.core.api.Assertions.assertThat;

class AdditionalClientsCircuitBreakerTest {

    @Test
    @DisplayName("BranchClientFallback returns valid fallback profile when employee-service is down")
    void testBranchClientFallback() {
        BranchClientFallback fallback = new BranchClientFallback();
        BranchDto.Response response = fallback.getBranchById(10L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getName()).contains("Резервный филиал");
        assertThat(response.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("AttendanceClientFallback returns valid fallback profile when attendance-service is down")
    void testAttendanceClientFallback() {
        AttendanceClientFallback fallback = new AttendanceClientFallback();
        AttendanceRecordDto.Response response = fallback.getAttendanceById(55L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(55L);
        assertThat(response.getEmployeeName()).contains("Attendance Service временно недоступен");
        assertThat(response.getActualStart()).isNotNull();
    }
}
