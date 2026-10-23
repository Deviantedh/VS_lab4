package portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;


import java.time.Instant;
import java.time.LocalDate;

public class EmployeeDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "EmployeeRequest")
    public static class Request {
        @NotBlank(message = "ФИО сотрудника обязательно")
        @Size(max = 255, message = "ФИО не может быть длиннее 255 символов")
        @Schema(description = "ФИО сотрудника", example = "Ваш текст")
        private String name;

        @NotBlank(message = "Номер телефона обязателен")
        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Номер телефона должен быть в формате +7XXXXXXXXXX")
        @Schema(description = "Номер телефона", example = "+79991234567")
        private String phone;

        @Schema(description = "Дата рождения", example = "2000-01-01")
        private LocalDate birthDate;

        @Schema(description = "Дата приёма на работу", example = "2026-01-01")
        private LocalDate hireDate;

        @Schema(description = "Статус сотрудника", example = "ACTIVE")
        private EmployeeStatus status;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "EmployeeResponse")
    public static class Response {
        @Schema(description = "ID сотрудника", example = "1")
        private Long id;
        @Schema(description = "ФИО сотрудника", example = "Ваш текст")
        private String name;
        @Schema(description = "Номер телефона", example = "+79991234567")
        private String phone;
        @Schema(description = "Дата рождения", example = "2000-01-01")
        private LocalDate birthDate;
        @Schema(description = "Дата приёма на работу", example = "2026-01-01")
        private LocalDate hireDate;
        @Schema(description = "Дата увольнения", example = "2026-12-31")
        private LocalDate dismissalDate;
        @Schema(description = "Статус сотрудника", example = "ACTIVE")
        private EmployeeStatus status;
        private Instant createdAt;
        private Instant updatedAt;
    }
}
