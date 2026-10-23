package portal.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;

public class CompanyDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "CompanyRequest")
    public static class Request {
        @NotBlank(message = "Название компании не может быть пустым")
        @Size(max = 255, message = "Название компании не может превышать 255 символов")
        @JsonAlias({"title"})
        @Schema(description = "Название компании", example = "Ваш текст")
        private String name;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "CompanyResponse")
    public static class Response {
        @Schema(description = "ID компании", example = "1")
        private Long id;

        @Schema(description = "Название компании", example = "Ваш текст")
        private String name;

        private Instant createdAt;
        private Instant updatedAt;
    }
}
