package portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;


import java.time.Instant;
import java.time.LocalDate;

public class EmployeeAbsenceDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "EmployeeAbsenceRequest")
    public static class Request {
        @NotNull(message = "ID сотрудника обязателен")
        @Schema(description = "ID сотрудника", example = "1")
        private Long employeeId;

        @Schema(description = "ID заявки (опционально)", example = "1")
        private Long requestId;

        @NotNull(message = "Тип отсутствия обязателен")
        @Schema(description = "Тип отсутствия (VACATION, SICK_LEAVE, UNPAID_LEAVE)", example = "VACATION")
        private AbsenceType type;

        @NotNull(message = "Дата начала обязательна")
        @Schema(description = "Дата начала", example = "2026-10-01")
        private LocalDate dateFrom;

        @NotNull(message = "Дата окончания обязательна")
        @Schema(description = "Дата окончания", example = "2026-10-14")
        private LocalDate dateTo;

        @Schema(description = "Причина или комментарий", example = "Ваш текст")
        private String comment;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "EmployeeAbsenceResponse")
    public static class Response {
        @Schema(description = "ID записи отсутствия", example = "1")
        private Long id;
        @Schema(description = "ID сотрудника", example = "1")
        private Long employeeId;
        @Schema(description = "ФИО сотрудника", example = "Ваш текст")
        private String employeeName;
        @Schema(description = "ID заявки", example = "1")
        private Long requestId;
        @Schema(example = "VACATION")
        private AbsenceType type;
        @Schema(example = "2026-10-01")
        private LocalDate dateFrom;
        @Schema(example = "2026-10-14")
        private LocalDate dateTo;
        @Schema(example = "Ваш текст")
        private String comment;
        private Instant createdAt;
    }
}
