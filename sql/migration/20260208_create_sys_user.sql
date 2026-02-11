-- 创建 sys_user 表
-- 日期：2026-02-08

-- 如果表已存在则跳过
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名（登录账号）',
    password VARCHAR(100) NOT NULL COMMENT '密码（加密存储）',
    nickname VARCHAR(50) COMMENT '昵称',
    avatar VARCHAR(255) COMMENT '头像URL',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- 创建索引
CREATE INDEX idx_sys_user_username ON sys_user(username);

-- 插入演示账号（密码为 123456 的 BCrypt 加密）
-- 明文密码也可用于演示阶段
INSERT INTO sys_user (username, password, nickname, status) VALUES 
    ('admin', '123456', '管理员', 1),
    ('demo', '123456', '演示用户', 1)
ON DUPLICATE KEY UPDATE update_time = NOW();
