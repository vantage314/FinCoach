package com.fincoach.core.healthv2.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建目标请求 DTO
 */
@Data
@Schema(description = "创建目标请求")
public class CreateGoalDTO {

    @NotBlank(message = "目标类型不能为空")
    @Schema(description = "目标类型: RETIREMENT/EDUCATION/HOUSE/CAR/EMERGENCY/OTHER", example = "EMERGENCY")
    private String type;

    @NotNull(message = "目标金额不能为空")
    @Schema(description = "目标金额", example = "100000.00")
    private BigDecimal targetAmount;

    @Schema(description = "目标日期 YYYY-MM-DD", example = "2027-12-31")
    private String targetDate;

    @Schema(description = "已储蓄金额", example = "20000.00")
    private BigDecimal currentSaved;

    @Schema(description = "每月计划储蓄", example = "5000.00")
    private BigDecimal monthlyPlan;

    @Schema(description = "建议风险等级")
    private String riskLevelSuggestion;
}
