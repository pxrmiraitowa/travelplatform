package com.travelplatform.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtClaimsProvider {

    private final SecretKey secretKey;

    public JwtClaimsProvider(JwtGatewayProperties properties) {
        this.secretKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public GatewayUser parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (claims.getExpiration() == null || !claims.getExpiration().after(new Date())) {
            throw new IllegalArgumentException("Token expired");
        }
        return new GatewayUser(Long.valueOf(claims.getSubject()), claims.get("roleCodes", List.class));
    }

    public record GatewayUser(Long userId, List<?> roleCodes) {
        public boolean isAdmin() {
            return roleCodes != null && roleCodes.stream().anyMatch(role -> "ADMIN".equals(role) || "ROLE_ADMIN".equals(role));
        }
    }
}
