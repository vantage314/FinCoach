package com.fincoach.core.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("asset_item")
public class AssetItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer categoryId;
    private String assetName;
    private BigDecimal currentValue;
    private BigDecimal holdingCost;
    private String assetCode;
    private String subType;
    private LocalDateTime updateTime;
    
    // Phase 14: 持仓相关字段
    private String stockCode;       // 关联证券代码
    private BigDecimal quantity;    // 持仓数量
    private BigDecimal costPrice;   // 持仓成本价
    private BigDecimal marketValue; // 实时市值
}
