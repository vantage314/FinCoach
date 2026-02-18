package com.fincoach.core.security;

import com.fincoach.core.repository.entity.User;
import com.fincoach.core.repository.mapper.UserMapper;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AdminChecker {

    private final UserMapper userMapper;
    private final PermissionChecker permissionChecker;

    public AdminChecker(UserMapper userMapper) {
        this.userMapper = userMapper;
        this.permissionChecker = null;
    }

    @Autowired
    public AdminChecker(UserMapper userMapper, PermissionChecker permissionChecker) {
        this.userMapper = userMapper;
        this.permissionChecker = permissionChecker;
    }

    public boolean isAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        try {
            User user = userMapper.selectById(userId);
            if (user != null && user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole())) {
                return true;
            }
        } catch (Exception ignored) {
            // ignore and fallback to RBAC role codes
        }
        if (permissionChecker != null) {
            try {
                List<String> roleCodes = permissionChecker.getRoleCodes(userId);
                if (roleCodes != null) {
                    for (String code : roleCodes) {
                        if (code != null && ("ADMIN".equalsIgnoreCase(code) || "ROLE_ADMIN".equalsIgnoreCase(code))) {
                            return true;
                        }
                    }
                }
            } catch (Exception ignored) {
                // ignore and deny by default
            }
        }
        return false;
    }
}
