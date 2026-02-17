package com.fincoach.core.healthv2.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fincoach.core.common.Result;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.rbac.RbacPermissionCodes;
import com.fincoach.core.rbac.dto.RbacAssignPermissionsDTO;
import com.fincoach.core.rbac.dto.RbacAssignRolesDTO;
import com.fincoach.core.rbac.dto.RbacMeDTO;
import com.fincoach.core.rbac.dto.RbacRoleSaveDTO;
import com.fincoach.core.rbac.dto.RbacUserDTO;
import com.fincoach.core.rbac.entity.FcPermissionEntity;
import com.fincoach.core.rbac.entity.FcRoleEntity;
import com.fincoach.core.rbac.service.RbacAdminService;
import com.fincoach.core.security.AdminOnly;
import com.fincoach.core.security.Permission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/rbac")
@Tag(name = "Admin-RBAC", description = "RBAC 管理")
@AdminOnly
public class RbacAdminController {

    private final RbacAdminService rbacAdminService;

    public RbacAdminController(RbacAdminService rbacAdminService) {
        this.rbacAdminService = rbacAdminService;
    }

    @GetMapping("/me")
    @Operation(summary = "当前用户权限与菜单")
    @Permission(RbacPermissionCodes.ADMIN_MENU_VIEW)
    public Result<RbacMeDTO> me() {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(rbacAdminService.buildMe(userId));
    }

    @GetMapping("/users/page")
    @Operation(summary = "用户列表（分页）")
    @Permission(RbacPermissionCodes.ADMIN_RBAC_VIEW)
    public Result<IPage<RbacUserDTO>> pageUsers(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return Result.success(rbacAdminService.pageUsers(page, size));
    }

    @PostMapping("/users/{userId}/roles")
    @Operation(summary = "设置用户角色（覆盖）")
    @Permission(RbacPermissionCodes.ADMIN_RBAC_EDIT)
    public Result<String> setUserRoles(@PathVariable Long userId, @RequestBody RbacAssignRolesDTO dto) {
        rbacAdminService.setUserRoles(userId, dto == null ? null : dto.getRoleCodes());
        return Result.success("ok");
    }

    @GetMapping("/roles")
    @Operation(summary = "角色列表")
    @Permission(RbacPermissionCodes.ADMIN_RBAC_VIEW)
    public Result<List<FcRoleEntity>> listRoles() {
        return Result.success(rbacAdminService.listRoles());
    }

    @PostMapping("/roles/save")
    @Operation(summary = "角色新增/更新")
    @Permission(RbacPermissionCodes.ADMIN_RBAC_EDIT)
    public Result<Long> saveRole(@RequestBody RbacRoleSaveDTO dto) {
        return Result.success(rbacAdminService.saveRole(dto));
    }

    @PostMapping("/roles/{roleId}/permissions")
    @Operation(summary = "设置角色权限（覆盖）")
    @Permission(RbacPermissionCodes.ADMIN_RBAC_EDIT)
    public Result<String> setRolePermissions(@PathVariable Long roleId, @RequestBody RbacAssignPermissionsDTO dto) {
        rbacAdminService.setRolePermissions(roleId, dto == null ? null : dto.getPermissionCodes());
        return Result.success("ok");
    }

    @GetMapping("/permissions")
    @Operation(summary = "权限点列表")
    @Permission(RbacPermissionCodes.ADMIN_RBAC_VIEW)
    public Result<List<FcPermissionEntity>> listPermissions() {
        return Result.success(rbacAdminService.listPermissions());
    }

    @PostMapping("/permissions/seed")
    @Operation(summary = "初始化权限点（幂等）")
    @Permission(RbacPermissionCodes.ADMIN_RBAC_EDIT)
    public Result<Integer> seedPermissions() {
        return Result.success(rbacAdminService.seedPermissions());
    }
}
