package portal.employee.security;

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
import java.time.Instant;
import java.util.Date;

@Slf4j
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${jwt.secret:v9y$B&E)H@McQfTjWnZr4u7x!A%C*F-JaNdRgUkXp2s5v8y/B?E(G+KbPeShVmYq}") String secret,
            @Value("${jwt.expiration-ms:86400000}") long expirationMs
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(Long userId, String login, RoleCode role, Long employeeId) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationMs);

        var builder = Jwts.builder()
                .subject(login)
                .claim("userId", userId)
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey);

        if (employeeId != null) {
            builder.claim("employeeId", employeeId);
        }

        return builder.compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        Number id = extractClaims(token).get("userId", Number.class);
        return id != null ? id.longValue() : null;
    }

    public RoleCode extractRole(String token) {
        String roleStr = extractClaims(token).get("role", String.class);
        return roleStr != null ? RoleCode.valueOf(roleStr) : RoleCode.EMPLOYEE;
    }

    public Long extractEmployeeId(String token) {
        Number empId = extractClaims(token).get("employeeId", Number.class);
        return empId != null ? empId.longValue() : null;
    }

    public UserPrincipal extractUserPrincipal(String token) {
        Claims claims = extractClaims(token);
        String username = claims.getSubject();
        Number userIdNum = claims.get("userId", Number.class);
        Long userId = userIdNum != null ? userIdNum.longValue() : null;
        String roleStr = claims.get("role", String.class);
        RoleCode role = roleStr != null ? RoleCode.valueOf(roleStr) : RoleCode.EMPLOYEE;
        Number empIdNum = claims.get("employeeId", Number.class);
        Long empId = empIdNum != null ? empIdNum.longValue() : null;

        return UserPrincipal.fromClaims(userId, username, role, empId);
    }
}
