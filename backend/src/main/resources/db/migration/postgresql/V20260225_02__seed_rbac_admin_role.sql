-- 初始化 RBAC 角色和管理员角色分配
-- ADMIN 角色
INSERT INTO fc_role (code, name, enabled) VALUES ('ADMIN', '管理员', 1)
ON CONFLICT (code) DO NOTHING;

-- USER 角色
INSERT INTO fc_role (code, name, enabled) VALUES ('USER', '普通用户', 1)
ON CONFLICT (code) DO NOTHING;

-- OPS 角色
INSERT INTO fc_role (code, name, enabled) VALUES ('OPS', '运维', 1)
ON CONFLICT (code) DO NOTHING;

-- 为 admin_wave3 分配 ADMIN 角色（user_id 取自 user 表）
INSERT INTO fc_user_role (user_id, role_id)
SELECT u.id, r.id
FROM "user" u, fc_role r
WHERE u.username = 'admin_wave3' AND r.code = 'ADMIN'
ON CONFLICT (user_id, role_id) DO NOTHING;

-- 同时也给 admin_wave3 分配 USER 角色
INSERT INTO fc_user_role (user_id, role_id)
SELECT u.id, r.id
FROM "user" u, fc_role r
WHERE u.username = 'admin_wave3' AND r.code = 'USER'
ON CONFLICT (user_id, role_id) DO NOTHING;
