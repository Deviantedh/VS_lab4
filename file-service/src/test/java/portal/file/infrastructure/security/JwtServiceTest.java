package portal.file.infrastructure.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import portal.dto.RoleCode;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "v9y$B&E)H@McQfTjWnZr4u7x!A%C*F-JaNdRgUkXp2s5v8y/B?E(G+KbPeShVmYq";
    private JwtService jwtService;
    private SecretKey key;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET);
        key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Validate and extract valid user principal")
    void testValidToken() {
        String token = Jwts.builder()
                .subject("ivanov")
                .claim("userId", 42L)
                .claim("role", "ADMIN")
                .claim("employeeId", 99L)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();

        assertThat(jwtService.validateToken(token)).isTrue();

        UserPrincipal principal = jwtService.extractUserPrincipal(token);
        assertThat(principal).isNotNull();
        assertThat(principal.getUsername()).isEqualTo("ivanov");
        assertThat(principal.getId()).isEqualTo(42L);
        assertThat(principal.getRole()).isEqualTo(RoleCode.ADMIN);
        assertThat(principal.getEmployeeId()).isEqualTo(99L);
        assertThat(principal.getAuthorities()).isNotEmpty();
        assertThat(principal.getPassword()).isEmpty();
        assertThat(principal.isAccountNonExpired()).isTrue();
        assertThat(principal.isAccountNonLocked()).isTrue();
        assertThat(principal.isCredentialsNonExpired()).isTrue();
        assertThat(principal.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Token with integer IDs and invalid role falls back gracefully")
    void testIntegerClaimsAndInvalidRole() {
        String token = Jwts.builder()
                .subject("petrov")
                .claim("userId", 15)
                .claim("role", "UNKNOWN_ROLE")
                .claim("employeeId", 20)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();

        assertThat(jwtService.validateToken(token)).isTrue();

        UserPrincipal principal = jwtService.extractUserPrincipal(token);
        assertThat(principal.getId()).isEqualTo(15L);
        assertThat(principal.getRole()).isEqualTo(RoleCode.EMPLOYEE);
        assertThat(principal.getEmployeeId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("Invalid signature or expired token returns false")
    void testInvalidToken() {
        assertThat(jwtService.validateToken("invalid.token.here")).isFalse();
        assertThat(jwtService.validateToken(null)).isFalse();
    }
}
