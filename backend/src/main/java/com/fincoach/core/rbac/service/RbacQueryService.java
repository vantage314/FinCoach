package com.fincoach.core.rbac.service;

import java.util.List;
import java.util.Set;

public interface RbacQueryService {
    Set<String> getUserPermissions(Long userId);
    List<String> getUserRoleCodes(Long userId);
    void invalidateUser(Long userId);
    void invalidateAll();
}
