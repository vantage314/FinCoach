package com.fincoach.core.rbac.dto;

import java.util.ArrayList;
import java.util.List;

public class RbacAssignPermissionsDTO {
    private List<String> permissionCodes = new ArrayList<>();

    public List<String> getPermissionCodes() { return permissionCodes; }
    public void setPermissionCodes(List<String> permissionCodes) { this.permissionCodes = permissionCodes; }
}
