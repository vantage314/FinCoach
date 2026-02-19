package com.fincoach.core.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fincoach.core.common.Result;
import com.fincoach.core.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

@Slf4j
@Component
public class AppUserRoleInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeUnauthorized(response, "未登录或 Token 缺失");
            return false;
        }

        String token = authHeader.substring(7);
        List<String> roles = jwtUtils.getRolesFromToken(token);
        boolean hasUserRole = hasRole(roles, "USER");
        boolean hasAdminRole = hasRole(roles, "ADMIN");

        if (!roles.isEmpty() && !hasUserRole) {
            log.warn("[AppUserRoleInterceptor] user role required, roles={}", roles);
            writeForbidden(response, "Forbidden");
            return false;
        }

        return true;
    }

    private boolean hasRole(List<String> roles, String target) {
        if (roles == null || roles.isEmpty() || target == null) {
            return false;
        }
        String normalizedTarget = target.toUpperCase();
        String alias = "ROLE_" + normalizedTarget;
        for (String role : roles) {
            if (role == null) continue;
            String normalized = role.trim().toUpperCase();
            if (normalized.equals(normalizedTarget) || normalized.equals(alias)) {
                return true;
            }
        }
        return false;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        Result<Object> result = Result.error(401, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

    private void writeForbidden(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        Result<Object> result = Result.error(403, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
