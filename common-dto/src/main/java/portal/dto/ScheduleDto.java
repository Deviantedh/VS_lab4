package portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

public class ScheduleDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "ScheduleRequest")
    public static class Request {
        @NotNull(message = "ID филиала обязателен")
        @Schema(description = "ID филиала", example = "1")
        private Long branchId;

        @NotNull(message = "Дата начала обязательна")
        @Schema(description = "Дата начала расписания", example = "2026-10-01")
        private LocalDate dateFrom;

        @NotNull(message = "Дата окончания обязательна")
        @Schema(description = "Дата окончания расписания", example = "2026-10-31")
        private LocalDate dateTo;

        @Schema(description = "ID создателя (пользователя)", example = "1")
        private Long createdById;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "ScheduleResponse")
    public static class Response {
        @Schema(description = "ID расписания", example = "1")
        private Long id;
        @Schema(description = "ID филиала", example = "1")
        private Long branchId;
        @Schema(description = "Название филиала", example = "Ваш текст")
        private String branchName;
        @Schema(description = "Дата начала", example = "2026-10-01")
        private LocalDate dateFrom;
        @Schema(description = "Дата окончания", example = "2026-10-31")
        private LocalDate dateTo;
        @Schema(description = "ID создателя", example = "1")
        private Long createdById;
        @Schema(description = "Имя создателя", example = "Ваш текст")
        private String createdByName;
        private Instant createdAt;
        private Instant updatedAt;
    }
}
