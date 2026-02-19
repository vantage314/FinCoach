package com.fincoach.core.healthv2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("fc_insurance_profile")
public class FcInsuranceProfileEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private BigDecimal annualIncome;
    private BigDecimal annualPremiumTotal;
    private Integer dependents;
    private Integer age;
    private BigDecimal existingCoverMedical;
    private BigDecimal existingCoverAccident;
    private BigDecimal existingCoverCi;
    private BigDecimal existingCoverLife;
    private String maritalStatus;
    private Integer childrenCount;
    private Integer dependentsCount;
    private String cityTier;
    private String existingCoverageJson;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
