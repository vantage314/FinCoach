package com.fincoach.core.healthv2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 体检v2-行为事件实体
 */
@Data
@TableName("fc_behavior_event")
public class FcBehaviorEventEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 事件类型: ASSET_UPDATE / LIABILITY_UPDATE / CASHFLOW_UPDATE / GOAL_UPDATE / INSURANCE_UPDATE /
     *  REBALANCE_CONFIRM / REPORT_GENERATE / MANUAL_TRADE / IMPORT_DATA */
    private String eventType;

    /** 关联金额(可空) */
    private BigDecimal amount;

    /** 扩展信息(JSON) */
    private String metaJson;

    private LocalDateTime createdAt;
}
