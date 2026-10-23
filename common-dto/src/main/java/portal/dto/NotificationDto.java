package portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Модель уведомления пользователя")
public class NotificationDto {

    @Schema(description = "Идентификатор уведомления", example = "1")
    private Long id;

    @Schema(description = "ID пользователя-получателя", example = "2")
    private Long recipientUserId;

    @Schema(description = "Заголовок уведомления", example = "Заявка на отпуск одобрена")
    private String title;

    @Schema(description = "Текст сообщения", example = "Ваш отпуск с 10 по 20 число успешно утвержден.")
    private String message;

    @Schema(description = "Тип уведомления", example = "SUCCESS")
    private String type;

    @Schema(description = "Ссылка для перехода", example = "/requests/42")
    private String link;

    @Schema(description = "Флаг прочтения", example = "false")
    private boolean isRead;

    @Schema(description = "Дата и время создания")
    private LocalDateTime createdAt;
}
