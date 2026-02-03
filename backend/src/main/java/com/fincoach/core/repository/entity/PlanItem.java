package com.fincoach.core.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 调仓建议明细实体
 */
@Data
@TableName("plan_item")
public class PlanItem {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long planId;
    
    private String action;
    
    private Integer categoryId;
    
    private String categoryName;
    
    private String subType;
    
    private BigDecimal amount;
    
    private BigDecimal currentRatio;
    
    private BigDecimal targetRatio;
    
    private String reason;
    
    private LocalDateTime createTime;
}
