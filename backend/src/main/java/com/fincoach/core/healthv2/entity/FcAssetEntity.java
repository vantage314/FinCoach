package com.fincoach.core.healthv2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 体检v2-资产实体
 */
@Data
@TableName("fc_asset")
public class FcAssetEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 资产类型: CASH/STOCK/FUND/BOND/REAL_ESTATE/OTHER */
    private String type;

    private String name;

    private BigDecimal amount;

    /** 币种，默认 CNY */
    private String currency;

    /** 风险等级: LOW/MEDIUM/HIGH */
    private String riskLevel;

    /** 估值日期 */
    private LocalDate asOfDate;

    /** 扩展信息(JSON) */
    private String metaJson;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
