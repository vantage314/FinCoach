package com.fincoach.core.healthv2.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新资产请求 DTO
 */
@Data
@Schema(description = "更新资产请求")
public class UpdateAssetDTO {

    @NotNull(message = "资产ID不能为空")
    @Schema(description = "资产ID")
    private Long id;

    @Schema(description = "资产类型")
    private String type;

    @Schema(description = "资产名称")
    private String name;

    @Schema(description = "资产金额")
    private BigDecimal amount;

    @Schema(description = "币种")
    private String currency;

    @Schema(description = "风险等级")
    private String riskLevel;

    @Schema(description = "估值日期 YYYY-MM-DD")
    private String asOfDate;

    @Schema(description = "扩展信息(JSON)")
    private String metaJson;
}
