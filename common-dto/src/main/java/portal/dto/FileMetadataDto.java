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
@Schema(description = "Метаданные сохраненного файла")
public class FileMetadataDto {

    @Schema(description = "Идентификатор файла", example = "101")
    private Long id;

    @Schema(description = "Внутреннее имя файла на диске", example = "a1b2c3d4-scan.pdf")
    private String filename;

    @Schema(description = "Оригинальное имя загруженного файла", example = "Больничный_лист.pdf")
    private String originalFilename;

    @Schema(description = "MIME-тип контента", example = "application/pdf")
    private String contentType;

    @Schema(description = "Размер файла в байтах", example = "2048576")
    private Long size;

    @Schema(description = "URL для скачивания файла", example = "/api/files/101/download")
    private String downloadUrl;

    @Schema(description = "Пользователь, загрузивший файл", example = "ivanov")
    private String uploadedBy;

    @Schema(description = "Дата и время загрузки файла")
    private LocalDateTime uploadedAt;
}
