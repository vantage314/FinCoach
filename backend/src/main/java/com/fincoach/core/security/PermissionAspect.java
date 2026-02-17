package com.fincoach.core.security;

import com.fincoach.core.common.UserContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@Order(20)
public class PermissionAspect {

    private final PermissionChecker permissionChecker;

    public PermissionAspect(PermissionChecker permissionChecker) {
        this.permissionChecker = permissionChecker;
    }

    @Around("@within(com.fincoach.core.security.Permission) || @annotation(com.fincoach.core.security.Permission)")
    public Object checkPermission(ProceedingJoinPoint pjp) throws Throwable {
        Permission permission = resolvePermission(pjp);
        if (permission == null || permission.value() == null || permission.value().isBlank()) {
            return pjp.proceed();
        }
        Long userId = UserContext.getCurrentUserId();
        if (!permissionChecker.hasPermission(userId, permission.value())) {
            throw new ForbiddenException("Forbidden");
        }
        return pjp.proceed();
    }

    private Permission resolvePermission(ProceedingJoinPoint pjp) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        Permission perm = method.getAnnotation(Permission.class);
        if (perm != null) {
            return perm;
        }
        Class<?> targetClass = pjp.getTarget() != null ? pjp.getTarget().getClass() : signature.getDeclaringType();
        return targetClass.getAnnotation(Permission.class);
    }
}
