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
@Schema(description = "Событие жизненного цикла сотрудника (Kafka)")
public class EmployeeEventDto {

    public enum EventType {
        EMPLOYEE_CREATED,
        EMPLOYEE_UPDATED,
        EMPLOYEE_DISMISSED,
        ASSIGNMENT_CREATED
    }

    @Schema(description = "Тип события")
    private EventType eventType;

    @Schema(description = "Идентификатор сотрудника")
    private Long employeeId;

    @Schema(description = "ФИО сотрудника")
    private String fullName;

    @Schema(description = "Телефон сотрудника")
    private String phone;

    @Schema(description = "Статус сотрудника")
    private String status;

    @Schema(description = "Идентификатор филиала")
    private Long branchId;

    @Schema(description = "Идентификатор должности")
    private Long positionId;

    @Schema(description = "Временная метка события")
    private Instant timestamp;
}
