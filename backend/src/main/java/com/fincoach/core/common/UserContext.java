package com.fincoach.core.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class UserContext {
    public static Long getCurrentUserId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new RuntimeException("Current request context is null");
        }
        HttpServletRequest request = attributes.getRequest();
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new RuntimeException("Current user not logged in");
        }
        return userId;
    }
}
