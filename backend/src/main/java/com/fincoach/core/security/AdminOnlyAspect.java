package com.fincoach.core.security;

import com.fincoach.core.common.UserContext;
import com.fincoach.core.repository.entity.User;
import com.fincoach.core.repository.mapper.UserMapper;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(10)
public class AdminOnlyAspect {

    private final UserMapper userMapper;

    public AdminOnlyAspect(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Before("@within(com.fincoach.core.security.AdminOnly) || @annotation(com.fincoach.core.security.AdminOnly)")
    public void checkAdmin() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new ForbiddenException("Forbidden");
        }
        User user = userMapper.selectById(userId);
        if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new ForbiddenException("Forbidden");
        }
    }
}
