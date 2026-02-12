package com.fincoach.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fincoach.core.common.Result;
import com.fincoach.core.common.UserContext;
import com.fincoach.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


/**
 * JWT 鉴权拦截器
 * 拦截 /api/** 请求，校验 Token 有效性并注入 UserContext
 */
@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 🔥 OPTIONS 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String uri = request.getRequestURI();
        log.debug("[JwtInterceptor] 拦截请求: {}", uri);

        // 从 Header 获取 Token
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("[JwtInterceptor] 未携带 Token: {}", uri);
            sendUnauthorizedResponse(response, "未登录");
            return false;
        }

        // 解析 Token
        String token = authHeader.substring(7); // 去掉 "Bearer " 前缀
        Claims claims = jwtUtil.parseToken(token);

        if (claims == null) {
            log.warn("[JwtInterceptor] Token 无效或已过期: {}", uri);
            sendUnauthorizedResponse(response, "Token 无效或已过期");
            return false;
        }

        // 🔥 核心：将 userId 存入 UserContext
        Long userId = Long.parseLong(claims.getSubject());
        UserContext.setUserId(userId);
        
        log.debug("[JwtInterceptor] 鉴权通过，userId={}", userId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 🔥 必须清除 UserContext，防止内存泄漏
        UserContext.clear();
        log.debug("[JwtInterceptor] 已清除 UserContext");
    }

    /**
     * 发送 401 未授权响应
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        Result<Object> result = Result.error(401, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
