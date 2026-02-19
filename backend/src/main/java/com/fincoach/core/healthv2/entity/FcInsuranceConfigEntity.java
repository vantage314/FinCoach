package com.fincoach.core.healthv2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("fc_insurance_config")
public class FcInsuranceConfigEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private BigDecimal targetMedical;
    private BigDecimal targetAccident;
    private BigDecimal targetCi;
    private BigDecimal targetLifeMultiplier;
    private BigDecimal premiumRatioWarn;
    private BigDecimal premiumRatioDanger;
    private LocalDateTime updatedAt;
}
