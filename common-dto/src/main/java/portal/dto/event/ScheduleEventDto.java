package portal.dto.event;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Событие планирования расписания и смен (Kafka)")
public class ScheduleEventDto {

    public enum EventType {
        SCHEDULE_CREATED,
        SHIFT_ASSIGNED,
        SHIFT_REMOVED,
        REQUEST_CREATED,
        REQUEST_APPROVED,
        REQUEST_REJECTED
    }

    @Schema(description = "Тип события расписания")
    private EventType eventType;

    @Schema(description = "Идентификатор расписания")
    private Long scheduleId;

    @Schema(description = "Идентификатор смены")
    private Long shiftId;

    @Schema(description = "Идентификатор сотрудника")
    private Long employeeId;

    @Schema(description = "Идентификатор филиала")
    private Long branchId;

    @Schema(description = "Дата смены")
    private LocalDate shiftDate;

    @Schema(description = "Идентификатор заявки (если событие связано с заявкой)")
    private Long requestId;

    @Schema(description = "Тип заявки")
    private String requestType;

    @Schema(description = "Временная метка события")
    private Instant timestamp;
}
