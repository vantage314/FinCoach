package com.fincoach.core.rbac.dto;

import java.util.ArrayList;
import java.util.List;

public class RbacAssignRolesDTO {
    private List<String> roleCodes = new ArrayList<>();

    public List<String> getRoleCodes() { return roleCodes; }
    public void setRoleCodes(List<String> roleCodes) { this.roleCodes = roleCodes; }
}
