package portal.schedule.client;

import org.springframework.stereotype.Component;
import portal.dto.AttendanceRecordDto;

import java.time.Instant;

@Component
public class AttendanceClientFallback implements AttendanceClient {

    @Override
    public AttendanceRecordDto.Response getAttendanceById(Long id) {
        return AttendanceRecordDto.Response.builder()
                .id(id)
                .employeeId(0L)
                .employeeName("Неизвестный сотрудник (Attendance Service временно недоступен)")
                .actualStart(Instant.now())
                .comment("Резервный ответ: Attendance Service временно недоступен")
                .build();
    }
}
