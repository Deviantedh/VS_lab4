package portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;



import java.time.Instant;

public class RequestDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "RequestCreateRequest")
    public static class Create {
        @NotNull(message = "ID сотрудника обязателен")
        @Schema(description = "ID сотрудника", example = "1")
        private Long employeeId;

        @NotNull(message = "Тип заявки обязателен")
        @Schema(description = "Тип заявки (VACATION, DAY_OFF, CERTIFICATE, MEDICAL_EXAM)", example = "VACATION")
        private RequestType type;

        @Schema(description = "Параметры заявки в формате JSON", example = "{\"dateFrom\": \"2026-10-01\", \"dateTo\": \"2026-10-14\"}")
        private String requestData;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "RequestProcessRequest")
    public static class Process {
        @NotNull(message = "Статус решения обязателен (APPROVED или REJECTED)")
        @Schema(description = "Статус решения", example = "APPROVED")
        private RequestStatus status;

        @Schema(description = "ID пользователя, принявшего решение", example = "1")
        private Long processedById;

        @Schema(description = "Комментарий к решению", example = "Ваш текст")
        private String resolutionComment;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "RequestResponse")
    public static class Response {
        @Schema(description = "ID заявки", example = "1")
        private Long id;
        @Schema(description = "ID сотрудника", example = "1")
        private Long employeeId;
        @Schema(description = "ФИО сотрудника", example = "Ваш текст")
        private String employeeName;
        @Schema(example = "VACATION")
        private RequestType type;
        @Schema(example = "PENDING")
        private RequestStatus status;
        @Schema(example = "{\"dateFrom\": \"2026-10-01\", \"dateTo\": \"2026-10-14\"}")
        private String requestData;
        @Schema(example = "1")
        private Long processedById;
        @Schema(example = "Ваш текст")
        private String processedByName;
        private Instant processedAt;
        @Schema(example = "Ваш текст")
        private String resolutionComment;
        private Instant createdAt;
        private Instant updatedAt;
    }
}
