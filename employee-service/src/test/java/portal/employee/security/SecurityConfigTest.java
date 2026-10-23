package portal.employee.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SecurityConfigTest {

    private SecurityConfig securityConfig;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        JwtAuthenticationFilter jwtFilter = mock(JwtAuthenticationFilter.class);
        securityConfig = new SecurityConfig(jwtFilter, objectMapper);
    }

    @Test
    @DisplayName("PasswordEncoder бин возвращает рабочий BCryptPasswordEncoder")
    void shouldReturnBCryptPasswordEncoder() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        assertThat(encoder).isNotNull();

        String hashed = encoder.encode("testPassword");
        assertThat(encoder.matches("testPassword", hashed)).isTrue();
    }

    @Test
    @DisplayName("unauthorizedEntryPoint формирует JSON-ответ со статусом 401 Unauthorized")
    void shouldHandleUnauthorized() throws Exception {
        AuthenticationEntryPoint entryPoint = securityConfig.unauthorizedEntryPoint();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException authException = new AuthenticationException("Unauthorized access") {};

        entryPoint.commence(request, response, authException);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getContentAsString()).contains("\"status\":401");
        assertThat(response.getContentAsString()).contains("\"error\":\"Unauthorized\"");
    }

    @Test
    @DisplayName("accessDeniedHandler формирует JSON-ответ со статусом 403 Forbidden")
    void shouldHandleAccessDenied() throws Exception {
        AccessDeniedHandler handler = securityConfig.accessDeniedHandler();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AccessDeniedException accessDeniedException = new AccessDeniedException("Forbidden access");

        handler.handle(request, response, accessDeniedException);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getContentAsString()).contains("\"status\":403");
        assertThat(response.getContentAsString()).contains("\"error\":\"Forbidden\"");
    }
}
