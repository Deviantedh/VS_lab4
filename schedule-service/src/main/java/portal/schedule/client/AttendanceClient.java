package portal.schedule.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import portal.dto.AttendanceRecordDto;

@FeignClient(name = "attendance-service", contextId = "attendanceClient", fallback = AttendanceClientFallback.class)
public interface AttendanceClient {

    @GetMapping("/api/attendance/{id}")
    AttendanceRecordDto.Response getAttendanceById(@PathVariable("id") Long id);
}
