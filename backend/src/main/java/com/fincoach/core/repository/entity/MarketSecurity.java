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
     * 今开价格
     */
    @TableField("open_price")
    private BigDecimal openPrice;

    /**
     * 最高价格
     */
    @TableField("high_price")
    private BigDecimal highPrice;

    /**
     * 最低价格
     */
    @TableField("low_price")
    private BigDecimal lowPrice;

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
    private BigDecimal marketCap;

    /**
     * 市盈率
     */
    @TableField("pe_ratio")
    private BigDecimal peRatio;

    /**
     * 成交量 (股)
     */
    private Long volume;

    /**
     * 成交额 (元)
     */
    private BigDecimal turnover;

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

    // ========== 五档盘口 ==========
    private BigDecimal bid1Price;
    private Integer bid1Vol;
    private BigDecimal bid2Price;
    private Integer bid2Vol;
    private BigDecimal bid3Price;
    private Integer bid3Vol;
    private BigDecimal bid4Price;
    private Integer bid4Vol;
    private BigDecimal bid5Price;
    private Integer bid5Vol;

    private BigDecimal ask1Price;
    private Integer ask1Vol;
    private BigDecimal ask2Price;
    private Integer ask2Vol;
    private BigDecimal ask3Price;
    private Integer ask3Vol;
    private BigDecimal ask4Price;
    private Integer ask4Vol;
    private BigDecimal ask5Price;
    private Integer ask5Vol;

    /** 市盈率 TTM */
    private BigDecimal peTtm;
}
