package com.fincoach.core.security;

import com.fincoach.core.common.UserContext;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(10)
public class AdminOnlyAspect {

    private final AdminChecker adminChecker;

    public AdminOnlyAspect(AdminChecker adminChecker) {
        this.adminChecker = adminChecker;
    }

    @Before("@within(com.fincoach.core.security.AdminOnly) || @annotation(com.fincoach.core.security.AdminOnly)")
    public void checkAdmin() {
        Long userId = UserContext.getCurrentUserId();
        if (!adminChecker.isAdmin(userId)) {
            throw new ForbiddenException("Forbidden");
        }
    }
}
