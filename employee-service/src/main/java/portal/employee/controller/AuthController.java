package portal.employee.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import portal.dto.AuthDto;
import portal.dto.UserDto;
import portal.employee.security.UserPrincipal;
import portal.employee.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Аутентификация пользователей, выдача JWT токенов и получение профиля текущего пользователя")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Вход в систему и получение JWT токена", description = "Проверяет логин и пароль (хэш BCrypt). В ответе возвращает Bearer JWT токен с claims (userId, role, employeeId).")
    public ResponseEntity<AuthDto.AuthResponse> login(@Valid @RequestBody AuthDto.LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Получить профиль текущего аутентифицированного пользователя", description = "Извлекает данные пользователя по переданному JWT токену из хедера Authorization.")
    public ResponseEntity<UserDto.Response> getMe(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(authService.getMe(principal));
    }
}
