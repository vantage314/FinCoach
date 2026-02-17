package com.fincoach.core.security;

import com.fincoach.core.repository.entity.User;
import com.fincoach.core.repository.mapper.UserMapper;
import org.springframework.stereotype.Component;

@Component
public class AdminChecker {

    private final UserMapper userMapper;

    public AdminChecker(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public boolean isAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        User user = userMapper.selectById(userId);
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }
}
