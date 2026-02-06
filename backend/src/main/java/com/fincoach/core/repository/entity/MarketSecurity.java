package com.fincoach.core.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.io.Serializable;

/**
 * 市场证券实体类
 */
@Data
@TableName("market_security")
public class MarketSecurity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 证券名称
     */
    private String name;

    /**
     * 证券代码
     */
    private String code;

    /**
     * 类型 (stock, fund, bond)
     */
    private String type;

    /**
     * 当前价格
     */
    @TableField("current_price")
    private BigDecimal currentPrice;

    /**
     * 涨跌幅 (%)
     */
    @TableField("change_percent")
    private BigDecimal changePercent;

    /**
     * 风险等级 (R1-R5)
     */
    @TableField("risk_level")
    private String riskLevel;

    /**
     * 所属板块
     */
    private String sector;

    /**
     * 描述
     */
    private String description;

    /**
     * 市值
     */
    @TableField("market_cap")
    private String marketCap;

    /**
     * 市盈率
     */
    @TableField("pe_ratio")
    private BigDecimal peRatio;

    /**
     * 成交量
     */
    private String volume;

    /**
     * 52周最高
     */
    @TableField("high52w")
    private BigDecimal high52w;

    /**
     * 52周最低
     */
    @TableField("low52w")
    private BigDecimal low52w;
}
