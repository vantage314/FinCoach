package com.fincoach.core.healthv2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 体检v2-体检报告实体
 */
@Data
@TableName("fc_health_report")
public class FcHealthReportEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 报告生成时间 */
    private LocalDateTime reportDate;

    /** 风险评分(0-100) */
    private Integer riskScore;

    /** 健康评分(0-100) */
    private Integer healthScore;

    /** 行为评分(0-100) */
    private Integer behaviorScore;

    /** 指标详情(JSON) */
    private String metricsJson;

    /** 建议详情(JSON) */
    private String adviceJson;

    /** 规则版本，默认 M1 */
    private String ruleVersion;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public LocalDateTime getReportDate() {
        return reportDate;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public Integer getHealthScore() {
        return healthScore;
    }

    public Integer getBehaviorScore() {
        return behaviorScore;
    }

    public String getAdviceJson() {
        return adviceJson;
    }

    public String getMetricsJson() {
        return metricsJson;
    }
}
