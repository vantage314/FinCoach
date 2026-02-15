package com.fincoach.core.healthv2.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资产类别参数配置
 */
@Data
@TableName("fc_asset_class_param")
public class FcAssetClassParamEntity {

    @TableId(type = IdType.INPUT)
    private String assetType;

    private BigDecimal expectedReturn;

    private BigDecimal volatility;

    private Integer riskFreeFlag;

    private LocalDateTime updatedAt;

    public BigDecimal getExpectedReturn() {
        return expectedReturn;
    }

    public BigDecimal getVolatility() {
        return volatility;
    }

    public Integer getRiskFreeFlag() {
        return riskFreeFlag;
    }
}
