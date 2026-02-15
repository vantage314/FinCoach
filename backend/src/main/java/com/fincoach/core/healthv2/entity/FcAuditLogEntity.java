package com.fincoach.core.healthv2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 体检v2-审计日志实体
 */
@Data
@TableName("fc_audit_log")
public class FcAuditLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作用户ID */
    private Long actorUserId;

    /** 操作类型: CREATE/UPDATE/DELETE/GENERATE_REPORT */
    private String action;

    /** 目标类型: FC_ASSET/FC_LIABILITY/FC_CASHFLOW/FC_GOAL/FC_INSURANCE_PROFILE/HEALTH_REPORT */
    private String targetType;

    /** 目标ID */
    private Long targetId;

    /** 变更前数据(JSON) */
    private String beforeJson;

    /** 变更后数据(JSON) */
    private String afterJson;

    private LocalDateTime createdAt;
}
