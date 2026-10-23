package portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

public class EmployeeAssignmentDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "EmployeeAssignmentRequest")
    public static class Request {
        @NotNull(message = "ID сотрудника обязателен")
        @Schema(description = "ID сотрудника", example = "1")
        private Long employeeId;

        @NotNull(message = "ID филиала обязателен")
        @Schema(description = "ID филиала", example = "1")
        private Long branchId;

        @NotNull(message = "ID должности обязателен")
        @Schema(description = "ID должности", example = "1")
        private Long positionId;

        @Schema(description = "Дата начала назначения", example = "2026-01-01")
        private LocalDate startedAt;

        @Schema(description = "Дата окончания назначения", example = "2026-12-31")
        private LocalDate endedAt;

        @Schema(description = "Основное ли назначение", example = "true")
        private Boolean isPrimary;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "EmployeeAssignmentResponse")
    public static class Response {
        @Schema(description = "ID назначения", example = "1")
        private Long id;
        @Schema(description = "ID сотрудника", example = "1")
        private Long employeeId;
        @Schema(description = "ФИО сотрудника", example = "Ваш текст")
        private String employeeName;
        @Schema(description = "ID филиала", example = "1")
        private Long branchId;
        @Schema(description = "Название филиала", example = "Ваш текст")
        private String branchName;
        @Schema(description = "ID должности", example = "1")
        private Long positionId;
        @Schema(description = "Название должности", example = "Ваш текст")
        private String positionTitle;
        @Schema(description = "Дата начала", example = "2026-01-01")
        private LocalDate startedAt;
        @Schema(description = "Дата окончания", example = "2026-12-31")
        private LocalDate endedAt;
        @Schema(description = "Основное ли назначение", example = "true")
        private Boolean isPrimary;
        private Instant createdAt;
    }
}
