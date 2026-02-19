package com.fincoach.core.healthv2.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Schema(description = "再平衡建议响应")
public class AdviceRebalanceResponseDTO {

    @Schema(description = "风险评分")
    private Integer riskScore;

    @Schema(description = "健康评分")
    private Integer healthScore;

    @Schema(description = "行为评分")
    private Integer behaviorScore;

    @Schema(description = "风险分档")
    private String riskBand;

    @Schema(description = "再平衡建议")
    private List<AdviceRebalanceSuggestionDTO> rebalanceSuggestions;

    @Schema(description = "元信息")
    private Map<String, Object> meta;
}
