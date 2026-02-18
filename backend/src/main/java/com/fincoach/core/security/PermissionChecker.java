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
            // Dev fallback: treat userId=1 as admin when RBAC tables are unavailable
            return userId == 1L;
        }
    }

    public Set<String> getPermissions(Long userId) {
        if (userId == null) return Collections.emptySet();
        try {
            return rbacQueryService.getUserPermissions(userId);
        } catch (Exception e) {
            if (userId == 1L) {
                return Set.of("ADMIN_FALLBACK");
            }
            return Collections.emptySet();
        }
    }

    public List<String> getRoleCodes(Long userId) {
        if (userId == null) return Collections.emptyList();
        try {
            return rbacQueryService.getUserRoleCodes(userId);
        } catch (Exception e) {
            if (userId == 1L) {
                return List.of("ADMIN");
            }
            return Collections.emptyList();
        }
    }

    public void invalidateUser(Long userId) {
        rbacQueryService.invalidateUser(userId);
    }

    public void invalidateAll() {
        rbacQueryService.invalidateAll();
    }
}
