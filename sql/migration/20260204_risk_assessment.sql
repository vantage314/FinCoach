-- 风险测评记录表
CREATE TABLE risk_assessment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    total_score INT NOT NULL COMMENT '总分',
    risk_level VARCHAR(20) NOT NULL COMMENT '风险等级枚举',
    assessment_json TEXT COMMENT '原始答卷快照',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险测评记录表';
