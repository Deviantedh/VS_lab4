package portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

public class AuthDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "LoginRequest")
    public static class LoginRequest {
        @NotBlank(message = "Логин обязателен")
        @Schema(description = "Логин пользователя", example = "admin")
        private String login;

        @NotBlank(message = "Пароль обязателен")
        @Schema(description = "Пароль пользователя", example = "admin123")
        private String password;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "AuthResponse")
    public static class AuthResponse {
        @Schema(description = "JWT токен авторизации", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        private String token;

        @Schema(description = "Тип токена", example = "Bearer")
        @Builder.Default
        private String tokenType = "Bearer";

        @Schema(description = "ID пользователя", example = "1")
        private Long userId;

        @Schema(description = "Логин пользователя", example = "admin")
        private String login;

        @Schema(description = "Код роли", example = "ADMIN")
        private RoleCode role;

        @Schema(description = "ID привязанного сотрудника", example = "1")
        private Long employeeId;
    }
}
