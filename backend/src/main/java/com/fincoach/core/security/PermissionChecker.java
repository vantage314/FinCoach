package com.fincoach.core.security;

import com.fincoach.core.rbac.service.RbacQueryService;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Component
public class PermissionChecker {

    private final RbacQueryService rbacQueryService;

    public PermissionChecker(RbacQueryService rbacQueryService) {
        this.rbacQueryService = rbacQueryService;
    }

    public boolean hasPermission(Long userId, String code) {
        if (userId == null || code == null || code.isBlank()) {
            return false;
        }
        try {
            return rbacQueryService.getUserPermissions(userId).contains(code);
        } catch (Exception e) {
            return false;
        }
    }

    public Set<String> getPermissions(Long userId) {
        if (userId == null) return Collections.emptySet();
        return rbacQueryService.getUserPermissions(userId);
    }

    public List<String> getRoleCodes(Long userId) {
        if (userId == null) return Collections.emptyList();
        return rbacQueryService.getUserRoleCodes(userId);
    }

    public void invalidateUser(Long userId) {
        rbacQueryService.invalidateUser(userId);
    }

    public void invalidateAll() {
        rbacQueryService.invalidateAll();
    }
}
