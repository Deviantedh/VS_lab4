package portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;

public class PositionDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "PositionRequest")
    public static class Request {
        @NotBlank(message = "Название должности обязательно")
        @Size(max = 255, message = "Название должности не может превышать 255 символов")
        @Schema(description = "Название должности", example = "Ваш текст")
        private String title;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "PositionResponse")
    public static class Response {
        @Schema(description = "ID должности", example = "1")
        private Long id;
        @Schema(description = "Название должности", example = "Ваш текст")
        private String title;
        private Instant createdAt;
        private Instant updatedAt;
    }
}
