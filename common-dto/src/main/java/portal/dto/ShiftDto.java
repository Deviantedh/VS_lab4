package portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class ShiftDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "ShiftRequest")
    public static class Request {
        @NotNull(message = "ID расписания обязателен")
        @Schema(description = "ID расписания", example = "1")
        private Long scheduleId;

        @NotNull(message = "Дата смены обязательна")
        @Schema(description = "Дата смены", example = "2026-10-01")
        private LocalDate date;

        @NotNull(message = "Время начала обязательно")
        @Schema(description = "Время начала (HH:mm:ss)", example = "09:00:00")
        private LocalTime timeFrom;

        @NotNull(message = "Время окончания обязательно")
        @Schema(description = "Время окончания (HH:mm:ss)", example = "18:00:00")
        private LocalTime timeTo;

        @Schema(description = "Перерыв в минутах", example = "60")
        private Integer breakMinutes;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "ShiftResponse")
    public static class Response {
        @Schema(description = "ID смены", example = "1")
        private Long id;
        @Schema(description = "ID расписания", example = "1")
        private Long scheduleId;
        @Schema(description = "Дата смены", example = "2026-10-01")
        private LocalDate date;
        @Schema(description = "Время начала", example = "09:00:00")
        private LocalTime timeFrom;
        @Schema(description = "Время окончания", example = "18:00:00")
        private LocalTime timeTo;
        @Schema(description = "Перерыв в минутах", example = "60")
        private Integer breakMinutes;
        private List<EmployeeDto.Response> assignedEmployees;
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "ShiftAssignEmployeeRequest")
    public static class AssignEmployeeRequest {
        @NotNull(message = "ID сотрудника обязателен")
        @Schema(description = "ID сотрудника", example = "1")
        private Long employeeId;

        @Schema(description = "ID назначившего пользователя", example = "1")
        private Long assignedById;
    }
}
