package com.fincoach.core.security;

import com.fincoach.core.rbac.service.RbacQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Component
public class PermissionChecker {

    private static final Logger log = LoggerFactory.getLogger(PermissionChecker.class);

    private final RbacQueryService rbacQueryService;
    private final boolean rbacFallbackEnabled;

    public PermissionChecker(RbacQueryService rbacQueryService,
                             Environment environment,
                             @Value("${security.rbacFallbackEnabled:false}") boolean fallbackEnabled) {
        this.rbacQueryService = rbacQueryService;
        boolean devProfile = false;
        if (environment != null) {
            for (String profile : environment.getActiveProfiles()) {
                if ("dev".equalsIgnoreCase(profile)
                        || "local".equalsIgnoreCase(profile)
                        || "development".equalsIgnoreCase(profile)) {
                    devProfile = true;
                    break;
                }
            }
        }
        this.rbacFallbackEnabled = devProfile || fallbackEnabled;
        if (this.rbacFallbackEnabled) {
            log.warn("RBAC_FALLBACK_ENABLED");
        }
    }

    public boolean hasPermission(Long userId, String code) {
        if (userId == null || code == null || code.isBlank()) {
            return false;
        }
        try {
            Set<String> permissions = rbacQueryService.getUserPermissions(userId);
            if (permissions.contains(code)) {
                return true;
            }
            if (code.startsWith("ADMIN_") && isAdminRole(userId)) {
                return true;
            }
            return false;
        } catch (Exception e) {
            // Dev/config fallback only
            return rbacFallbackEnabled && userId == 1L;
        }
    }

    public Set<String> getPermissions(Long userId) {
        if (userId == null) return Collections.emptySet();
        try {
            return rbacQueryService.getUserPermissions(userId);
        } catch (Exception e) {
            if (rbacFallbackEnabled && userId == 1L) {
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
            if (rbacFallbackEnabled && userId == 1L) {
                return List.of("ADMIN");
            }
            return Collections.emptyList();
        }
    }

    public boolean isFallbackEnabled() {
        return rbacFallbackEnabled;
    }

    public void invalidateUser(Long userId) {
        rbacQueryService.invalidateUser(userId);
    }

    public void invalidateAll() {
        rbacQueryService.invalidateAll();
    }

    private boolean isAdminRole(Long userId) {
        List<String> roleCodes = getRoleCodes(userId);
        if (roleCodes == null || roleCodes.isEmpty()) {
            return false;
        }
        for (String code : roleCodes) {
            String normalized = code == null ? "" : code.trim().toUpperCase();
            if ("ADMIN".equals(normalized) || "ROLE_ADMIN".equals(normalized)) {
                return true;
            }
        }
        return false;
    }
}
