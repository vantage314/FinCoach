package com.fincoach.core.interceptor;

import com.fincoach.core.common.UserContext;
import com.fincoach.core.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 鉴权拦截器
 * 拦截 /api/** 请求，校验 Token 有效性并注入 userId
 */
@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtils jwtUtils;

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
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录或 Token 缺失\"}");
            return false;
        }

        String token = authHeader.substring(7); // 去掉 "Bearer " 前缀
        Long userId = jwtUtils.getUserIdFromToken(token);

        if (userId == null) {
            log.warn("[JwtInterceptor] Token 无效或已过期: {}", uri);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Token 无效或已过期\"}");
            return false;
        }

        // 🔥 核心：将 userId 存入 UserContext
        UserContext.setUserId(userId);
        log.debug("[JwtInterceptor] 鉴权通过，userId={}", userId);
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserContext.clear();
        log.debug("[JwtInterceptor] 已清除 UserContext");
    }
}
