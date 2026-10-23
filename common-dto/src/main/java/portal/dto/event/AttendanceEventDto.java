package portal.dto.event;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Событие учета рабочего времени и явки (Kafka)")
public class AttendanceEventDto {

    public enum EventType {
        CHECK_IN,
        CHECK_OUT,
        LATE_ARRIVAL,
        OVERTIME
    }

    @Schema(description = "Тип события явки")
    private EventType eventType;

    @Schema(description = "Идентификатор записи явки")
    private Long recordId;

    @Schema(description = "Идентификатор сотрудника")
    private Long employeeId;

    @Schema(description = "Идентификатор смены")
    private Long shiftId;

    @Schema(description = "Фактическое время прихода")
    private LocalDateTime actualStart;

    @Schema(description = "Фактическое время ухода")
    private LocalDateTime actualEnd;

    @Schema(description = "Минуты опоздания")
    private Integer lateMinutes;

    @Schema(description = "Минуты переработки")
    private Integer overtimeMinutes;

    @Schema(description = "Временная метка события")
    private Instant timestamp;
}
