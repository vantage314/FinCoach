package com.fincoach.core.rbac.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.service.AuditService;
import com.fincoach.core.rbac.RbacPermissionCodes;
import com.fincoach.core.rbac.dto.RbacMeDTO;
import com.fincoach.core.rbac.dto.RbacRoleSaveDTO;
import com.fincoach.core.rbac.dto.RbacUserDTO;
import com.fincoach.core.rbac.entity.FcPermissionEntity;
import com.fincoach.core.rbac.entity.FcRoleEntity;
import com.fincoach.core.rbac.entity.FcRolePermissionEntity;
import com.fincoach.core.rbac.entity.FcUserRoleEntity;
import com.fincoach.core.rbac.mapper.FcPermissionMapper;
import com.fincoach.core.rbac.mapper.FcRoleMapper;
import com.fincoach.core.rbac.mapper.FcRolePermissionMapper;
import com.fincoach.core.rbac.mapper.FcUserRoleMapper;
import com.fincoach.core.rbac.menu.AdminMenuRegistry;
import com.fincoach.core.rbac.service.RbacAdminService;
import com.fincoach.core.rbac.service.RbacQueryService;
import com.fincoach.core.repository.entity.User;
import com.fincoach.core.repository.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RbacAdminServiceImpl implements RbacAdminService {

    private final UserMapper userMapper;
    private final FcRoleMapper roleMapper;
    private final FcPermissionMapper permissionMapper;
    private final FcUserRoleMapper userRoleMapper;
    private final FcRolePermissionMapper rolePermissionMapper;
    private final RbacQueryService rbacQueryService;
    private final AuditService auditService;
    private final AdminMenuRegistry menuRegistry = new AdminMenuRegistry();

    public RbacAdminServiceImpl(UserMapper userMapper,
                                FcRoleMapper roleMapper,
                                FcPermissionMapper permissionMapper,
                                FcUserRoleMapper userRoleMapper,
                                FcRolePermissionMapper rolePermissionMapper,
                                RbacQueryService rbacQueryService,
                                AuditService auditService) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.userRoleMapper = userRoleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.rbacQueryService = rbacQueryService;
        this.auditService = auditService;
    }

    @Override
    public RbacMeDTO buildMe(Long userId) {
        RbacMeDTO dto = new RbacMeDTO();
        dto.setUserId(userId);
        if (userId == null) {
            return dto;
        }
        List<String> roles = rbacQueryService.getUserRoleCodes(userId);
        Set<String> perms = rbacQueryService.getUserPermissions(userId);
        dto.setRoles(new ArrayList<>(roles));
        dto.setPermissions(new ArrayList<>(perms));
        dto.setMenuTree(menuRegistry.getMenuTree(perms));
        return dto;
    }

    @Override
    public IPage<RbacUserDTO> pageUsers(int page, int size) {
        IPage<User> userPage = userMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<User>().orderByDesc(User::getCreateTime));

        List<Long> userIds = userPage.getRecords().stream().map(User::getId).collect(Collectors.toList());
        Map<Long, List<String>> roleMap = fetchRoleCodesByUserIds(userIds);

        return userPage.convert(user -> {
            RbacUserDTO dto = new RbacUserDTO();
            dto.setUserId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setRoles(roleMap.getOrDefault(user.getId(), new ArrayList<>()));
            return dto;
        });
    }

    @Override
    @Transactional
    public void setUserRoles(Long userId, List<String> roleCodes) {
        if (userId == null) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        List<String> codes = roleCodes == null ? new ArrayList<>() : roleCodes;
        List<FcRoleEntity> roles = codes.isEmpty()
                ? new ArrayList<>()
                : roleMapper.selectList(new LambdaQueryWrapper<FcRoleEntity>().in(FcRoleEntity::getCode, codes));
        if (roles.size() != codes.size()) {
            throw new IllegalArgumentException("存在无效的 role code");
        }

        List<FcUserRoleEntity> before = userRoleMapper.selectList(
                new LambdaQueryWrapper<FcUserRoleEntity>().eq(FcUserRoleEntity::getUserId, userId));

        userRoleMapper.delete(new LambdaQueryWrapper<FcUserRoleEntity>().eq(FcUserRoleEntity::getUserId, userId));
        LocalDateTime now = LocalDateTime.now();
        for (FcRoleEntity role : roles) {
            FcUserRoleEntity ur = new FcUserRoleEntity();
            ur.setUserId(userId);
            ur.setRoleId(role.getId());
            ur.setCreateTime(now);
            userRoleMapper.insert(ur);
        }

        rbacQueryService.invalidateUser(userId);
        auditService.log(UserContext.getCurrentUserId(), "RBAC_SET_USER_ROLES", "USER", userId, before, roles);
    }

    @Override
    public List<FcRoleEntity> listRoles() {
        return roleMapper.selectList(new LambdaQueryWrapper<>());
    }

    @Override
    public Long saveRole(RbacRoleSaveDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        String code = dto.getCode() != null ? dto.getCode().trim() : "";
        String name = dto.getName() != null ? dto.getName().trim() : "";
        if (code.isEmpty()) throw new IllegalArgumentException("code 不能为空");
        if (name.isEmpty()) throw new IllegalArgumentException("name 不能为空");

        LocalDateTime now = LocalDateTime.now();
        FcRoleEntity entity;
        FcRoleEntity before = null;
        if (dto.getId() != null) {
            entity = roleMapper.selectById(dto.getId());
            if (entity == null) throw new IllegalArgumentException("角色不存在");
            before = entity;
        } else {
            entity = new FcRoleEntity();
            entity.setCreateTime(now);
        }
        entity.setCode(code);
        entity.setName(name);
        entity.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : 1);
        entity.setUpdateTime(now);

        if (dto.getId() == null) {
            roleMapper.insert(entity);
        } else {
            roleMapper.updateById(entity);
        }
        auditService.log(UserContext.getCurrentUserId(), "RBAC_SAVE_ROLE", "ROLE", entity.getId(), before, entity);
        return entity.getId();
    }

    @Override
    @Transactional
    public void setRolePermissions(Long roleId, List<String> permissionCodes) {
        if (roleId == null) throw new IllegalArgumentException("roleId 不能为空");
        List<String> codes = permissionCodes == null ? new ArrayList<>() : permissionCodes;
        List<FcPermissionEntity> permissions = codes.isEmpty()
                ? new ArrayList<>()
                : permissionMapper.selectList(new LambdaQueryWrapper<FcPermissionEntity>().in(FcPermissionEntity::getCode, codes));
        if (permissions.size() != codes.size()) {
            throw new IllegalArgumentException("存在无效的 permission code");
        }

        List<FcRolePermissionEntity> before = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<FcRolePermissionEntity>().eq(FcRolePermissionEntity::getRoleId, roleId));

        rolePermissionMapper.delete(new LambdaQueryWrapper<FcRolePermissionEntity>().eq(FcRolePermissionEntity::getRoleId, roleId));
        LocalDateTime now = LocalDateTime.now();
        for (FcPermissionEntity perm : permissions) {
            FcRolePermissionEntity rp = new FcRolePermissionEntity();
            rp.setRoleId(roleId);
            rp.setPermissionId(perm.getId());
            rp.setCreateTime(now);
            rolePermissionMapper.insert(rp);
        }
        rbacQueryService.invalidateAll();
        auditService.log(UserContext.getCurrentUserId(), "RBAC_SET_ROLE_PERMISSIONS", "ROLE", roleId, before, permissions);
    }

    @Override
    public List<FcPermissionEntity> listPermissions() {
        return permissionMapper.selectList(new LambdaQueryWrapper<>());
    }

    @Override
    public int seedPermissions() {
        List<FcPermissionEntity> existing = permissionMapper.selectList(new LambdaQueryWrapper<>());
        Map<String, FcPermissionEntity> existingByCode = existing.stream()
                .collect(Collectors.toMap(FcPermissionEntity::getCode, p -> p, (a, b) -> a));

        List<FcPermissionEntity> seeds = defaultPermissions();
        int created = 0;
        LocalDateTime now = LocalDateTime.now();
        for (FcPermissionEntity p : seeds) {
            if (existingByCode.containsKey(p.getCode())) {
                continue;
            }
            p.setCreateTime(now);
            p.setUpdateTime(now);
            permissionMapper.insert(p);
            created++;
        }
        if (created > 0) {
            rbacQueryService.invalidateAll();
        }
        auditService.log(UserContext.getCurrentUserId(), "RBAC_SEED_PERMISSION", "PERMISSION", null, null, seeds);
        return created;
    }

    private List<FcPermissionEntity> defaultPermissions() {
        List<FcPermissionEntity> list = new ArrayList<>();
        list.add(buildPerm(RbacPermissionCodes.ADMIN_RBAC_VIEW, "RBAC 查看", "RBAC"));
        list.add(buildPerm(RbacPermissionCodes.ADMIN_RBAC_EDIT, "RBAC 编辑", "RBAC"));
        list.add(buildPerm(RbacPermissionCodes.ADMIN_MENU_VIEW, "后台菜单查看", "RBAC"));
        list.add(buildPerm(RbacPermissionCodes.ADMIN_TICKER_MAPPING_VIEW, "Ticker 映射查看", "M7"));
        list.add(buildPerm(RbacPermissionCodes.ADMIN_TICKER_MAPPING_EDIT, "Ticker 映射编辑", "M7"));
        list.add(buildPerm(RbacPermissionCodes.ADMIN_MARKET_DEBUG_VIEW, "市场调试查看", "M7"));
        list.add(buildPerm(RbacPermissionCodes.ADMIN_MARKET_DEBUG_CAPTURE, "市场调试采集", "M7"));
        list.add(buildPerm(RbacPermissionCodes.ADMIN_SCORE_RULE_VIEW, "评分规则查看", "M8"));
        list.add(buildPerm(RbacPermissionCodes.ADMIN_SCORE_RULE_EDIT, "评分规则编辑", "M8"));
        list.add(buildPerm(RbacPermissionCodes.ADMIN_SCORE_RULE_PUBLISH, "评分规则发布", "M8"));
        list.add(buildPerm(RbacPermissionCodes.ADMIN_REBALANCE_TEMPLATE_VIEW, "再平衡模板查看", "M8"));
        list.add(buildPerm(RbacPermissionCodes.ADMIN_REBALANCE_TEMPLATE_EDIT, "再平衡模板编辑", "M8"));
        list.add(buildPerm(RbacPermissionCodes.ADMIN_REBALANCE_TEMPLATE_PUBLISH, "再平衡模板发布", "M8"));
        return list;
    }

    private FcPermissionEntity buildPerm(String code, String name, String module) {
        FcPermissionEntity entity = new FcPermissionEntity();
        entity.setCode(code);
        entity.setName(name);
        entity.setModule(module);
        entity.setEnabled(1);
        return entity;
    }

    private Map<Long, List<String>> fetchRoleCodesByUserIds(List<Long> userIds) {
        Map<Long, List<String>> result = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return result;
        }
        List<FcUserRoleEntity> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<FcUserRoleEntity>().in(FcUserRoleEntity::getUserId, userIds));
        if (userRoles.isEmpty()) {
            return result;
        }
        List<Long> roleIds = userRoles.stream().map(FcUserRoleEntity::getRoleId).distinct().collect(Collectors.toList());
        List<FcRoleEntity> roles = roleMapper.selectBatchIds(roleIds);
        Map<Long, String> roleIdToCode = roles.stream()
                .collect(Collectors.toMap(FcRoleEntity::getId, FcRoleEntity::getCode, (a, b) -> a));

        for (FcUserRoleEntity ur : userRoles) {
            String code = roleIdToCode.get(ur.getRoleId());
            if (code == null) continue;
            result.computeIfAbsent(ur.getUserId(), k -> new ArrayList<>()).add(code);
        }
        return result;
    }
}
