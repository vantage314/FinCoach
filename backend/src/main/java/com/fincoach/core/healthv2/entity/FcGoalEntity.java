package com.fincoach.core.healthv2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 体检v2-目标账户实体
 */
@Data
@TableName("fc_goal")
public class FcGoalEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 目标类型: RETIREMENT/EDUCATION/HOUSE/CAR/EMERGENCY/OTHER */
    private String type;

    private BigDecimal targetAmount;

    private LocalDate targetDate;

    /** 已储蓄金额 */
    private BigDecimal currentSaved;

    /** 每月计划储蓄 */
    private BigDecimal monthlyPlan;

    /** 建议风险等级 */
    private String riskLevelSuggestion;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public BigDecimal getCurrentSaved() {
        return currentSaved;
    }

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }
}
