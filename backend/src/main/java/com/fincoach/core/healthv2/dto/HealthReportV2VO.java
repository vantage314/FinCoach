package com.fincoach.core.healthv2.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 体检报告响应 VO
 */
@Data
@Schema(description = "体检报告响应")
public class HealthReportV2VO {

    @Schema(description = "报告ID")
    private Long reportId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "报告生成时间")
    private LocalDateTime reportDate;

    @Schema(description = "风险评分(0-100)")
    private Integer riskScore;

    @Schema(description = "健康评分(0-100)")
    private Integer healthScore;

    @Schema(description = "行为评分(0-100)")
    private Integer behaviorScore;

    @Schema(description = "指标详情")
    private Map<String, Object> metrics;

    @Schema(description = "建议详情")
    private Map<String, Object> advice;

    @Schema(description = "规则版本")
    private String ruleVersion;
}
