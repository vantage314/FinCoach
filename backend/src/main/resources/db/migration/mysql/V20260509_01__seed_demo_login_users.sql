INSERT IGNORE INTO `user` (username, password, enabled, create_time, update_time)
VALUES
  ('vantage1', '$2a$10$.xqTtjrCsLunRd8Eh.WO9.Vo1aDQRP5grA/tElu2zfc5baMohVT4K', 1, NOW(), NOW()),
  ('admin_wave3', '$2a$10$cqRfs7BqhfwN1cr//nmTKuzVc5UMxSsNF7jI1pqc14NFcMzIy6JDi', 1, NOW(), NOW());

INSERT IGNORE INTO fc_role (code, name, enabled)
VALUES
  ('ADMIN', '管理员', 1),
  ('USER', '普通用户', 1);

INSERT IGNORE INTO fc_user_role (user_id, role_id)
SELECT u.id, r.id
FROM `user` u
JOIN fc_role r ON r.code = 'USER'
WHERE u.username IN ('vantage1', 'admin_wave3');

INSERT IGNORE INTO fc_user_role (user_id, role_id)
SELECT u.id, r.id
FROM `user` u
JOIN fc_role r ON r.code = 'ADMIN'
WHERE u.username = 'admin_wave3';
