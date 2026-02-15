package com.fincoach.core.healthv2.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资产类别相关性矩阵参数
 * 联合主键 (type_a, type_b)
 */
@Data
@TableName("fc_asset_corr_param")
public class FcAssetCorrParamEntity {

    private String typeA;

    private String typeB;

    private BigDecimal corr;

    private LocalDateTime updatedAt;

    public String getTypeA() {
        return typeA;
    }

    public String getTypeB() {
        return typeB;
    }

    public BigDecimal getCorr() {
        return corr;
    }
}
