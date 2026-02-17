package com.fincoach.core.healthv2.debug;

import com.fincoach.core.rbac.RbacPermissionCodes;
import com.fincoach.core.security.AdminChecker;
import com.fincoach.core.security.PermissionChecker;
import com.fincoach.core.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(5)
public class PortfolioDebugGateFilter extends OncePerRequestFilter {

    private final AdminChecker adminChecker;
    private final PermissionChecker permissionChecker;
    private final JwtUtils jwtUtils;

    @Value("${fincoach.portfolio.debug.capture-enabled:false}")
    private boolean captureEnabled;

    public PortfolioDebugGateFilter(AdminChecker adminChecker, PermissionChecker permissionChecker, JwtUtils jwtUtils) {
        this.adminChecker = adminChecker;
        this.permissionChecker = permissionChecker;
        this.jwtUtils = jwtUtils;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("X-Debug-Market");
        boolean headerOn = "1".equals(header) || "true".equalsIgnoreCase(header);
        Long userId = resolveUserId(request);
        if (headerOn && captureEnabled && userId != null && adminChecker.isAdmin(userId)
                && permissionChecker.hasPermission(userId, RbacPermissionCodes.ADMIN_MARKET_DEBUG_CAPTURE)) {
            PortfolioDebugContextHolder.enableCapture();
        }
        filterChain.doFilter(request, response);
    }

    private Long resolveUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        return jwtUtils.getUserIdFromToken(token);
    }

    void setCaptureEnabled(boolean captureEnabled) {
        this.captureEnabled = captureEnabled;
    }
}
