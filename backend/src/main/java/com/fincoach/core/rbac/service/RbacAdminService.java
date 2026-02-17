package com.fincoach.core.rbac.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fincoach.core.rbac.dto.RbacMeDTO;
import com.fincoach.core.rbac.dto.RbacRoleSaveDTO;
import com.fincoach.core.rbac.dto.RbacUserDTO;
import com.fincoach.core.rbac.entity.FcPermissionEntity;
import com.fincoach.core.rbac.entity.FcRoleEntity;

import java.util.List;

public interface RbacAdminService {
    RbacMeDTO buildMe(Long userId);
    IPage<RbacUserDTO> pageUsers(int page, int size);
    void setUserRoles(Long userId, List<String> roleCodes);
    List<FcRoleEntity> listRoles();
    Long saveRole(RbacRoleSaveDTO dto);
    void setRolePermissions(Long roleId, List<String> permissionCodes);
    List<FcPermissionEntity> listPermissions();
    int seedPermissions();
}
