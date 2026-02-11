package com.fincoach.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 * 负责 Token 的生成与解析
 */
@Slf4j
@Component
public class JwtUtil {
    
    // 🔥 密钥（生产环境应从配置文件读取）
    private static final String SECRET = "fincoach_secure_jwt_secret_key_2026_must_be_at_least_256_bits";
    
    // Token 有效期：24小时
    private static final long EXPIRE_TIME = 24 * 60 * 60 * 1000L;
    
    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * 生成 JWT Token
     * @param userId 用户ID
     * @param username 用户名
     * @return JWT Token 字符串
     */
    public String createToken(Long userId, String username) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + EXPIRE_TIME);
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        
        String token = Jwts.builder()
                .header().add("typ", "JWT").and()
                .claims(claims)
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
        
        log.debug("[JwtUtil] 生成 Token，userId={}, username={}", userId, username);
        return token;
    }
    
    /**
     * 解析 JWT Token
     * @param token JWT Token 字符串
     * @return Claims 对象，解析失败返回 null
     */
    public Claims parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            log.debug("[JwtUtil] Token 解析成功，userId={}", claims.getSubject());
            return claims;
        } catch (ExpiredJwtException e) {
            log.warn("[JwtUtil] Token 已过期");
            return null;
        } catch (MalformedJwtException e) {
            log.warn("[JwtUtil] Token 格式错误");
            return null;
        } catch (SignatureException e) {
            log.warn("[JwtUtil] Token 签名无效");
            return null;
        } catch (Exception e) {
            log.warn("[JwtUtil] Token 解析失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 从 Token 中获取用户ID
     * @param token JWT Token
     * @return 用户ID，解析失败返回 null
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims != null) {
            return Long.parseLong(claims.getSubject());
        }
        return null;
    }
    
    /**
     * 从 Token 中获取用户名
     * @param token JWT Token
     * @return 用户名，解析失败返回 null
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims != null) {
            return claims.get("username", String.class);
        }
        return null;
    }
}
