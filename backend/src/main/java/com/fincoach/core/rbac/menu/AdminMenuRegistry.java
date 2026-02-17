package com.fincoach.core.rbac.menu;

import com.fincoach.core.rbac.RbacPermissionCodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class AdminMenuRegistry {

    private static final List<MenuItem> MENU = buildMenu();

    public List<MenuItem> getMenuTree(Set<String> permissions) {
        if (permissions == null) {
            permissions = Collections.emptySet();
        }
        List<MenuItem> result = new ArrayList<>();
        for (MenuItem item : MENU) {
            MenuItem filtered = filter(item, permissions);
            if (filtered != null) {
                result.add(filtered);
            }
        }
        return result;
    }

    private MenuItem filter(MenuItem item, Set<String> permissions) {
        boolean allowed = item.getPermissionCode() == null || permissions.contains(item.getPermissionCode());
        List<MenuItem> filteredChildren = new ArrayList<>();
        if (item.getChildren() != null) {
            for (MenuItem child : item.getChildren()) {
                MenuItem filtered = filter(child, permissions);
                if (filtered != null) {
                    filteredChildren.add(filtered);
                }
            }
        }
        if (!allowed && filteredChildren.isEmpty()) {
            return null;
        }
        MenuItem copy = new MenuItem();
        copy.setKey(item.getKey());
        copy.setName(item.getName());
        copy.setPath(item.getPath());
        copy.setPermissionCode(item.getPermissionCode());
        copy.setChildren(filteredChildren);
        return copy;
    }

    private static List<MenuItem> buildMenu() {
        List<MenuItem> roots = new ArrayList<>();

        MenuItem rbac = new MenuItem();
        rbac.setKey("rbac");
        rbac.setName("权限管理");
        rbac.setPath("/admin/rbac");
        rbac.setPermissionCode(RbacPermissionCodes.ADMIN_RBAC_VIEW);
        roots.add(rbac);

        MenuItem ticker = new MenuItem();
        ticker.setKey("ticker-mapping");
        ticker.setName("Ticker 映射");
        ticker.setPath("/admin/ticker-mapping");
        ticker.setPermissionCode(RbacPermissionCodes.ADMIN_TICKER_MAPPING_VIEW);
        roots.add(ticker);

        MenuItem debug = new MenuItem();
        debug.setKey("market-debug");
        debug.setName("市场调试");
        debug.setPath("/admin/market-debug");
        debug.setPermissionCode(RbacPermissionCodes.ADMIN_MARKET_DEBUG_VIEW);
        roots.add(debug);

        return roots;
    }

    public static class MenuItem {
        private String key;
        private String name;
        private String path;
        private String permissionCode;
        private List<MenuItem> children = new ArrayList<>();

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getPermissionCode() { return permissionCode; }
        public void setPermissionCode(String permissionCode) { this.permissionCode = permissionCode; }
        public List<MenuItem> getChildren() { return children; }
        public void setChildren(List<MenuItem> children) { this.children = children; }
    }
}
