package com.fincoach.core.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 调仓计划实体
 */
@Data
@TableName("investment_plan")
public class InvestmentPlan {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;

    private String planName;
    
    private String riskLevel;
    
    private BigDecimal totalAmount;
    
    private String planType;
    
    private BigDecimal investMoney;
    
    private String status;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}
