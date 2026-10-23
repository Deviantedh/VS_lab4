package portal.employee.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import portal.dto.RoleCode;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private static final String SECRET = "v9y$B&E)H@McQfTjWnZr4u7x!A%C*F-JaNdRgUkXp2s5v8y/B?E(G+KbPeShVmYq";
    private static final long EXPIRATION_MS = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, EXPIRATION_MS);
    }

    @Test
    @DisplayName("Генерация валидного JWT токена и извлечение claims")
    void shouldGenerateAndExtractClaims() {
        String token = jwtService.generateToken(1L, "admin", RoleCode.ADMIN, 10L);

        assertThat(token).isNotBlank();
        assertThat(jwtService.validateToken(token)).isTrue();
        assertThat(jwtService.extractUsername(token)).isEqualTo("admin");
        assertThat(jwtService.extractUserId(token)).isEqualTo(1L);
        assertThat(jwtService.extractRole(token)).isEqualTo(RoleCode.ADMIN);
        assertThat(jwtService.extractEmployeeId(token)).isEqualTo(10L);
    }

    @Test
    @DisplayName("Извлечение UserPrincipal из JWT токена")
    void shouldExtractUserPrincipal() {
        String token = jwtService.generateToken(2L, "barista_dmitry", RoleCode.EMPLOYEE, 5L);

        UserPrincipal principal = jwtService.extractUserPrincipal(token);

        assertThat(principal.getId()).isEqualTo(2L);
        assertThat(principal.getUsername()).isEqualTo("barista_dmitry");
        assertThat(principal.getRole()).isEqualTo(RoleCode.EMPLOYEE);
        assertThat(principal.getEmployeeId()).isEqualTo(5L);
        assertThat(principal.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_EMPLOYEE");
    }

    @Test
    @DisplayName("Валидация некорректного токена возвращает false")
    void shouldReturnFalseForInvalidToken() {
        assertThat(jwtService.validateToken("invalid.jwt.token")).isFalse();
        assertThat(jwtService.validateToken("")).isFalse();
    }

    @Test
    @DisplayName("Токен с истекшим сроком действия возвращает false")
    void shouldReturnFalseForExpiredToken() {
        JwtService expiredJwtService = new JwtService(SECRET, -1000); // expired 1s ago
        String expiredToken = expiredJwtService.generateToken(1L, "admin", RoleCode.ADMIN, null);
        assertThat(jwtService.validateToken(expiredToken)).isFalse();
    }

    @Test
    @DisplayName("Проверка хэширования паролей BCrypt")
    void testBCryptEncoding() {
        org.springframework.security.crypto.password.PasswordEncoder encoder =
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();

        String adminHash = encoder.encode("admin123");
        String passHash = encoder.encode("password123");

        System.out.println("HASH_ADMIN: " + adminHash);
        System.out.println("HASH_PASS: " + passHash);

        assertThat(encoder.matches("admin123", adminHash)).isTrue();
        assertThat(encoder.matches("password123", passHash)).isTrue();
    }
}
