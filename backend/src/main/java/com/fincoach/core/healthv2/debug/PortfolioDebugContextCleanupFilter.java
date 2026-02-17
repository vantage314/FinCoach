package com.fincoach.core.healthv2.debug;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class PortfolioDebugContextCleanupFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        boolean relevant = uri != null && (uri.startsWith("/api/app/") || uri.startsWith("/api/admin/"));
        try {
            filterChain.doFilter(request, response);
        } finally {
            if (relevant) {
                PortfolioDebugContextHolder.clear();
            }
        }
    }
}
