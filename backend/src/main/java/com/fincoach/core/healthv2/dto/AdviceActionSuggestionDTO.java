package com.fincoach.core.healthv2.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "债务/现金流建议项")
public class AdviceActionSuggestionDTO {

    @Schema(description = "类型: DEBT_PRIORITY / EMERGENCY_FUND / CASHFLOW_STABILIZE / DTI_RISK / REBALANCE_ALLOCATE")
    private String type;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "详情")
    private String detail;

    @Schema(description = "优先级: HIGH/MEDIUM/LOW")
    private String priority;
}
