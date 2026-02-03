package com.fincoach.core.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "资产查询参数")
public class AssetQueryDTO {
    @Schema(description = "资产分类ID (1-现金, 2-金融投资, 3-固定资产)", example = "2")
    private Integer categoryId;

    @Schema(description = "资产名称关键字", example = "茅台")
    private String assetName;
}
