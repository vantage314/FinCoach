package com.fincoach.core.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "资产录入请求参数")
public class AssetItemDTO {
    @NotNull(message = "资产分类不能为空")
    @Min(value = 1, message = "资产分类ID无效")
    @Max(value = 3, message = "资产分类ID无效")
    @Schema(description = "资产分类ID (1-现金, 2-金融投资, 3-固定资产)", example = "2")
    private Integer categoryId;

    @Schema(description = "子类型 (仅金融投资有效: 股票/基金/债券)", example = "基金")
    private String subType;

    @NotBlank(message = "资产名称不能为空")
    @Size(min = 2, max = 50, message = "资产名称长度必须在2-50之间")
    @Schema(description = "资产名称", example = "易方达蓝筹精选")
    private String assetName;

    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.01", message = "金额必须大于0")
    @Schema(description = "当前金额/市值", example = "50000.00")
    private BigDecimal currentValue;

    @Schema(description = "持有成本 (投资/固定资产可填)", example = "45000.00")
    private BigDecimal holdingCost;

    @Schema(description = "资产代码 (仅金融投资可填，如股票代码)", example = "600519")
    private String assetCode;
}
