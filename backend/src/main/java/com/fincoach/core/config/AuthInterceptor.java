package com.fincoach.core.config;

import com.fincoach.core.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AuthInterceptor implements HandlerInterceptor, WebMvcConfigurer {

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        System.out.println(">>> 拦截器检查 - URI: " + uri + ", Method: " + request.getMethod());
        
        // 放行 OPTIONS 请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        
        // 白名单路径已经在 addInterceptors 中排除
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            Long userId = jwtUtils.getUserIdFromToken(token);
            if (userId != null) {
                request.setAttribute("userId", userId);
                return true;
            }
        }
        
        System.out.println(">>> 拦截器拒绝访问 - URI: " + uri);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("Unauthorized");
        return false;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/auth/**", 
                        "/api/asset/categories",
                        "/error", 
                        "/swagger-ui/**", 
                        "/v3/api-docs/**", 
                        "/swagger-ui.html", 
                        "/webjars/**"
                );
    }
}
