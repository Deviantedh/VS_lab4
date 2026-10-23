package portal.employee.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import portal.dto.AuthDto;
import portal.dto.RoleCode;
import portal.dto.UserDto;
import portal.employee.security.UserPrincipal;
import portal.employee.service.AuthService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @Test
    @DisplayName("POST /api/auth/login возвращает 200 OK и AuthResponse")
    void testLogin() {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest("admin", "admin123");
        AuthDto.AuthResponse expected = AuthDto.AuthResponse.builder()
                .token("jwt.token")
                .tokenType("Bearer")
                .userId(1L)
                .login("admin")
                .role(RoleCode.ADMIN)
                .build();

        when(authService.login(request)).thenReturn(expected);

        ResponseEntity<AuthDto.AuthResponse> response = authController.login(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(authService).login(request);
    }

    @Test
    @DisplayName("GET /api/auth/me возвращает профиль текущего пользователя")
    void testGetMe() {
        UserPrincipal principal = UserPrincipal.builder()
                .id(1L)
                .login("admin")
                .role(RoleCode.ADMIN)
                .build();

        UserDto.Response expected = UserDto.Response.builder()
                .id(1L)
                .login("admin")
                .roleCode(RoleCode.ADMIN)
                .build();

        when(authService.getMe(principal)).thenReturn(expected);

        ResponseEntity<UserDto.Response> response = authController.getMe(principal);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(authService).getMe(principal);
    }
}
