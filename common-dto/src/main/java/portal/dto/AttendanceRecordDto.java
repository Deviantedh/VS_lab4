package portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

public class AttendanceRecordDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "AttendanceRecordRequest")
    public static class Request {
        @NotNull(message = "ID сотрудника обязателен")
        @Schema(description = "ID сотрудника", example = "1")
        private Long employeeId;

        @Schema(description = "ID смены (опционально)", example = "1")
        private Long shiftId;

        @NotNull(message = "Плановое время начала обязательно")
        @Schema(description = "Плановое время начала (ISO-8601)", example = "2026-10-01T09:00:00Z")
        private Instant plannedStart;

        @NotNull(message = "Плановое время окончания обязательно")
        @Schema(description = "Плановое время окончания (ISO-8601)", example = "2026-10-01T18:00:00Z")
        private Instant plannedEnd;

        @Schema(description = "Фактическое время начала (ISO-8601)", example = "2026-10-01T09:05:00Z")
        private Instant actualStart;

        @Schema(description = "Фактическое время окончания (ISO-8601)", example = "2026-10-01T18:00:00Z")
        private Instant actualEnd;

        @Schema(description = "Перерыв в минутах", example = "60")
        private Integer breakMinutes;

        @Schema(description = "Комментарий", example = "Ваш текст")
        private String comment;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "AttendanceCheckInRequest")
    public static class CheckInRequest {
        @NotNull(message = "ID сотрудника обязателен")
        @Schema(description = "ID сотрудника", example = "1")
        private Long employeeId;

        @Schema(description = "ID смены", example = "1")
        private Long shiftId;

        @Schema(description = "Комментарий", example = "Ваш текст")
        private String comment;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "AttendanceCheckOutRequest")
    public static class CheckOutRequest {
        @Schema(description = "Перерыв в минутах", example = "60")
        private Integer breakMinutes;

        @Schema(description = "Комментарий", example = "Ваш текст")
        private String comment;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "AttendanceRecordResponse")
    public static class Response {
        @Schema(description = "ID записи посещаемости", example = "1")
        private Long id;
        @Schema(description = "ID сотрудника", example = "1")
        private Long employeeId;
        @Schema(description = "ФИО сотрудника", example = "Ваш текст")
        private String employeeName;
        @Schema(description = "ID смены", example = "1")
        private Long shiftId;
        @Schema(example = "2026-10-01T09:00:00Z")
        private Instant plannedStart;
        @Schema(example = "2026-10-01T18:00:00Z")
        private Instant plannedEnd;
        @Schema(example = "2026-10-01T09:05:00Z")
        private Instant actualStart;
        @Schema(example = "2026-10-01T18:00:00Z")
        private Instant actualEnd;
        @Schema(example = "60")
        private Integer breakMinutes;
        @Schema(example = "Ваш текст")
        private String comment;
        @Schema(description = "Минут опоздания", example = "5")
        private Long lateMinutes;
        @Schema(description = "Минут переработки", example = "0")
        private Long overtimeMinutes;
        private Instant createdAt;
        private Instant updatedAt;
    }
}
