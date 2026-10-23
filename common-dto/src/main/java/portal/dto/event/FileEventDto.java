package portal.dto.event;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Событие загрузки/удаления файла (Kafka)")
public class FileEventDto {

    public enum EventType {
        FILE_UPLOADED,
        FILE_DELETED
    }

    @Schema(description = "Тип события с файлом")
    private EventType eventType;

    @Schema(description = "Идентификатор файла")
    private Long fileId;

    @Schema(description = "Имя сохраненного файла на диске")
    private String filename;

    @Schema(description = "Оригинальное имя загруженного файла")
    private String originalFilename;

    @Schema(description = "MIME-тип контента")
    private String contentType;

    @Schema(description = "Размер в байтах")
    private Long size;

    @Schema(description = "Логин или ID пользователя, загрузившего файл")
    private String uploadedBy;

    @Schema(description = "Временная метка события")
    private Instant timestamp;
}
