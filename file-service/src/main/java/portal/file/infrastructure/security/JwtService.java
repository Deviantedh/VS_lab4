package portal.file.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import portal.dto.RoleCode;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class JwtService {

    private final SecretKey signingKey;

    public JwtService(
            @Value("${jwt.secret:v9y$B&E)H@McQfTjWnZr4u7x!A%C*F-JaNdRgUkXp2s5v8y/B?E(G+KbPeShVmYq}") String secret
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public UserPrincipal extractUserPrincipal(String token) {
        Claims claims = parseClaims(token);

        Long userId = claims.get("userId", Long.class);
        if (userId == null && claims.get("userId") instanceof Integer) {
            userId = ((Integer) claims.get("userId")).longValue();
        }

        String roleStr = claims.get("role", String.class);
        RoleCode role = RoleCode.EMPLOYEE;
        if (roleStr != null) {
            try {
                role = RoleCode.valueOf(roleStr);
            } catch (IllegalArgumentException e) {
                log.warn("Unknown role: {}", roleStr);
            }
        }

        Long employeeId = claims.get("employeeId", Long.class);
        if (employeeId == null && claims.get("employeeId") instanceof Integer) {
            employeeId = ((Integer) claims.get("employeeId")).longValue();
        }

        return UserPrincipal.fromClaims(userId, claims.getSubject(), role, employeeId);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
