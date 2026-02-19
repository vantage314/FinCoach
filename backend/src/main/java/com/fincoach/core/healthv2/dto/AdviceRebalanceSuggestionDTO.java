package com.fincoach.core.healthv2.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "再平衡建议项")
public class AdviceRebalanceSuggestionDTO {

    @Schema(description = "资产类型")
    private String assetType;

    @Schema(description = "当前占比")
    private Double currentWeight;

    @Schema(description = "目标占比")
    private Double targetWeight;

    @Schema(description = "偏离值")
    private Double diffWeight;

    @Schema(description = "动作: BUY/SELL")
    private String action;

    @Schema(description = "建议调整比例")
    private Double actionWeight;

    @Schema(description = "金额提示")
    private BigDecimal amountHint;
}
