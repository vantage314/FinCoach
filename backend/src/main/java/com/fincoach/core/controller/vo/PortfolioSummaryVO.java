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
}
