package com.fincoach.core.controller.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Schema(description = "资产组合统计响应")
public class PortfolioSummaryVO {
    @Schema(description = "总资产金额")
    private BigDecimal totalAmount;

    @Schema(description = "各分类金额占比 (分类名称 -> 占比百分比)")
    private Map<String, BigDecimal> categoryDistribution;

    @Schema(description = "理性投资红线 (总额的20%)")
    private BigDecimal investmentLimit;

    // --- Phase 8: 小白保护机制字段 ---
    @Schema(description = "安全红线 (30000)")
    private BigDecimal safetyThreshold;

    @Schema(description = "资金缺口 (Threshold - Cash)")
    private BigDecimal liquidityGap;

    @Schema(description = "安全垫进度百分比 (0-100)")
    private Double safetyProgress;

    @Schema(description = "用户画像标签 (蓄力期/增值期)")
    private String personaTag;
}
