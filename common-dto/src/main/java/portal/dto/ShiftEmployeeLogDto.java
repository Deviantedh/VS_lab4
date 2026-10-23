package portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;


import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "ShiftEmployeeLogResponse")
public class ShiftEmployeeLogDto {
    @Schema(description = "ID записи аудита", example = "1")
    private Long id;
    @Schema(description = "ID смены", example = "1")
    private Long shiftId;
    @Schema(description = "ID сотрудника", example = "1")
    private Long employeeId;
    @Schema(description = "ФИО сотрудника", example = "Ваш текст")
    private String employeeName;
    @Schema(description = "Действие над сменой", example = "ASSIGNED")
    private ShiftAction action;
    @Schema(description = "ID пользователя, выполнившего действие", example = "1")
    private Long createdById;
    @Schema(description = "Имя пользователя, выполнившего действие", example = "Ваш текст")
    private String createdByName;
    private Instant createdAt;
}
