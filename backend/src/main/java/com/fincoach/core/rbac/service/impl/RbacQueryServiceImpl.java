package com.fincoach.core.rbac.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fincoach.core.rbac.entity.FcPermissionEntity;
import com.fincoach.core.rbac.entity.FcRoleEntity;
import com.fincoach.core.rbac.entity.FcRolePermissionEntity;
import com.fincoach.core.rbac.entity.FcUserRoleEntity;
import com.fincoach.core.rbac.mapper.FcPermissionMapper;
import com.fincoach.core.rbac.mapper.FcRoleMapper;
import com.fincoach.core.rbac.mapper.FcRolePermissionMapper;
import com.fincoach.core.rbac.mapper.FcUserRoleMapper;
import com.fincoach.core.rbac.service.RbacQueryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class RbacQueryServiceImpl implements RbacQueryService {

    private final FcUserRoleMapper userRoleMapper;
    private final FcRoleMapper roleMapper;
    private final FcRolePermissionMapper rolePermissionMapper;
    private final FcPermissionMapper permissionMapper;

    private final ConcurrentHashMap<Long, CacheEntry> cache = new ConcurrentHashMap<>();

    @Value("${fincoach.rbac.cache-ttl-seconds:60}")
    private long cacheTtlSeconds;

    public RbacQueryServiceImpl(FcUserRoleMapper userRoleMapper,
                                FcRoleMapper roleMapper,
                                FcRolePermissionMapper rolePermissionMapper,
                                FcPermissionMapper permissionMapper) {
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.permissionMapper = permissionMapper;
    }

    @Override
    public Set<String> getUserPermissions(Long userId) {
        if (userId == null) return Collections.emptySet();
        return getOrLoad(userId).permissions;
    }

    @Override
    public List<String> getUserRoleCodes(Long userId) {
        if (userId == null) return Collections.emptyList();
        return getOrLoad(userId).roleCodes;
    }

    @Override
    public void invalidateUser(Long userId) {
        if (userId != null) {
            cache.remove(userId);
        }
    }

    @Override
    public void invalidateAll() {
        cache.clear();
    }

    private CacheEntry getOrLoad(Long userId) {
        CacheEntry cached = cache.get(userId);
        long now = Instant.now().getEpochSecond();
        if (cached != null && cached.expiresAtEpochSec > now) {
            return cached;
        }
        CacheEntry loaded = loadFromDb(userId, now);
        cache.put(userId, loaded);
        return loaded;
    }

    private CacheEntry loadFromDb(Long userId, long nowEpochSec) {
        List<FcUserRoleEntity> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<FcUserRoleEntity>().eq(FcUserRoleEntity::getUserId, userId));
        if (userRoles == null || userRoles.isEmpty()) {
            return new CacheEntry(Collections.emptySet(), Collections.emptyList(), nowEpochSec + cacheTtlSeconds);
        }

        List<Long> roleIds = userRoles.stream().map(FcUserRoleEntity::getRoleId).distinct().collect(Collectors.toList());
        List<FcRoleEntity> roles = roleIds.isEmpty() ? Collections.emptyList() : roleMapper.selectBatchIds(roleIds);
        List<String> roleCodes = roles.stream().map(FcRoleEntity::getCode).collect(Collectors.toList());

        List<FcRolePermissionEntity> rolePermissions = roleIds.isEmpty()
                ? Collections.emptyList()
                : rolePermissionMapper.selectList(new LambdaQueryWrapper<FcRolePermissionEntity>()
                .in(FcRolePermissionEntity::getRoleId, roleIds));
        List<Long> permissionIds = rolePermissions.stream()
                .map(FcRolePermissionEntity::getPermissionId)
                .distinct()
                .collect(Collectors.toList());

        List<FcPermissionEntity> permissions = permissionIds.isEmpty()
                ? Collections.emptyList()
                : permissionMapper.selectBatchIds(permissionIds);
        Set<String> permissionCodes = permissions.stream()
                .map(FcPermissionEntity::getCode)
                .collect(Collectors.toSet());

        return new CacheEntry(permissionCodes, roleCodes, nowEpochSec + cacheTtlSeconds);
    }

    private static class CacheEntry {
        private final Set<String> permissions;
        private final List<String> roleCodes;
        private final long expiresAtEpochSec;

        private CacheEntry(Set<String> permissions, List<String> roleCodes, long expiresAtEpochSec) {
            this.permissions = permissions;
            this.roleCodes = roleCodes;
            this.expiresAtEpochSec = expiresAtEpochSec;
        }
    }
}
