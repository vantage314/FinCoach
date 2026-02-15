package com.fincoach.core.healthv2.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新目标请求 DTO
 */
@Data
@Schema(description = "更新目标请求")
public class UpdateGoalDTO {

    @NotNull(message = "目标ID不能为空")
    @Schema(description = "目标ID")
    private Long id;

    @Schema(description = "目标类型")
    private String type;

    @Schema(description = "目标金额")
    private BigDecimal targetAmount;

    @Schema(description = "目标日期 YYYY-MM-DD")
    private String targetDate;

    @Schema(description = "已储蓄金额")
    private BigDecimal currentSaved;

    @Schema(description = "每月计划储蓄")
    private BigDecimal monthlyPlan;

    @Schema(description = "建议风险等级")
    private String riskLevelSuggestion;
}
