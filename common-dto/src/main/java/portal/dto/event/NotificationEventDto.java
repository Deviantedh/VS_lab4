package portal.dto.event;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import portal.dto.RoleCode;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Событие отправки уведомления пользователю/роли (Kafka)")
public class NotificationEventDto {

    public enum Priority {
        LOW,
        NORMAL,
        HIGH,
        CRITICAL
    }

    @Schema(description = "Уникальный ID события")
    private String eventId;

    @Schema(description = "ID конкретного пользователя-получателя (nullable, если адресовано роли)")
    private Long recipientUserId;

    @Schema(description = "ID сотрудника-получателя (nullable)")
    private Long recipientEmployeeId;

    @Schema(description = "Целевая роль получателей (nullable, если адресовано конкретному пользователю)")
    private RoleCode targetRole;

    @Schema(description = "Заголовок уведомления")
    private String title;

    @Schema(description = "Текст уведомления")
    private String message;

    @Schema(description = "Тип уведомления (INFO, SUCCESS, WARNING, ALERT)")
    private String type;

    @Schema(description = "Приоритет")
    private Priority priority;

    @Schema(description = "Ссылка для перехода из уведомления")
    private String link;

    @Schema(description = "Временная метка генерации события")
    private Instant timestamp;
}
