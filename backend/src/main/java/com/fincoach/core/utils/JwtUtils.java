package com.fincoach.core.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

@Component
public class JwtUtils {
    private static final String SECRET = "fincoach_secret_key_for_jwt_token_auth_20260203";
    private static final long EXPIRE = 604800000; // 7 days

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId) {
        return generateToken(userId, List.of());
    }

    public String generateToken(Long userId, List<String> roles) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + EXPIRE);
        return Jwts.builder()
                .header().add("typ", "JWT").and()
                .subject(userId.toString())
                .claim("roles", roles == null ? List.of() : roles)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Long.parseLong(claims.getSubject());
        } catch (Exception e) {
            return null;
        }
    }

    public List<String> getRolesFromToken(String token) {
        if (token == null || token.isBlank()) {
            return List.of();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Object roles = claims.get("roles");
            return normalizeRoles(roles);
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<String> normalizeRoles(Object input) {
        if (input == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        if (input instanceof List<?> list) {
            for (Object item : list) {
                if (item != null) {
                    String value = String.valueOf(item).trim();
                    if (!value.isEmpty()) {
                        result.add(value);
                    }
                }
            }
            return result;
        }
        String raw = String.valueOf(input).trim();
        if (raw.isEmpty()) {
            return List.of();
        }
        for (String part : raw.split(",")) {
            String value = part.trim();
            if (!value.isEmpty()) {
                result.add(value);
            }
        }
        return result;
    }
}
