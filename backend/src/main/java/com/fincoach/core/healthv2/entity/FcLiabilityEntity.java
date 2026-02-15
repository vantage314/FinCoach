package com.fincoach.core.healthv2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 体检v2-负债实体
 */
@Data
@TableName("fc_liability")
public class FcLiabilityEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 负债类型: MORTGAGE/CAR_LOAN/CREDIT_CARD/CONSUMER_LOAN/OTHER */
    private String type;

    /** 本金/余额 */
    private BigDecimal principal;
    private BigDecimal interestRate;
    private BigDecimal monthlyPayment;

    /** 剩余还款月数 */
    private Integer remainingMonths;


    /** 提前还款罚金信息(JSON) */
    private String prepayPenaltyJson;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public BigDecimal getPrincipal() {
        return principal;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }
}
