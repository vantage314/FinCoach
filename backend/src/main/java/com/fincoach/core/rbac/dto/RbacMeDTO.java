package com.fincoach.core.rbac.dto;

import com.fincoach.core.rbac.menu.AdminMenuRegistry;

import java.util.ArrayList;
import java.util.List;

public class RbacMeDTO {
    private Long userId;
    private List<String> roles = new ArrayList<>();
    private List<String> permissions = new ArrayList<>();
    private List<AdminMenuRegistry.MenuItem> menuTree = new ArrayList<>();

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
    public List<String> getPermissions() { return permissions; }
    public void setPermissions(List<String> permissions) { this.permissions = permissions; }
    public List<AdminMenuRegistry.MenuItem> getMenuTree() { return menuTree; }
    public void setMenuTree(List<AdminMenuRegistry.MenuItem> menuTree) { this.menuTree = menuTree; }
}
