package com.fincoach.core.healthv2.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 保险档案请求 DTO（upsert 语义：一个用户仅一条）
 */
@Data
@Schema(description = "保险档案请求")
public class InsuranceProfileDTO {

    @Schema(description = "年收入", example = "200000.00")
    private BigDecimal annualIncome;

    @Schema(description = "婚姻状况: SINGLE/MARRIED/DIVORCED", example = "MARRIED")
    private String maritalStatus;

    @Schema(description = "子女数", example = "1")
    private Integer childrenCount;

    @Schema(description = "被赡养人数", example = "2")
    private Integer dependentsCount;

    @Schema(description = "城市等级: TIER1/TIER2/TIER3/OTHER", example = "TIER1")
    private String cityTier;

    @Schema(description = "已有保障信息(JSON)")
    private String existingCoverageJson;
}
