package com.fincoach.core.healthv2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 体检v2-保险档案实体
 * UNIQUE(user_id)
 */
@Data
@TableName("fc_insurance_profile")
public class FcInsuranceProfileEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 年收入 */
    private BigDecimal annualIncome;

    /** 婚姻状况: SINGLE/MARRIED/DIVORCED */
    private String maritalStatus;

    /** 子女数 */
    private Integer childrenCount;

    /** 被赡养人数 */
    private Integer dependentsCount;

    /** 城市等级: TIER1/TIER2/TIER3/OTHER */
    private String cityTier;

    /** 已有保障信息(JSON) */
    private String existingCoverageJson;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public String getMaritalStatus() {
        return maritalStatus;
    }

    public Integer getDependentsCount() {
        return dependentsCount;
    }

    public Integer getChildrenCount() {
        return childrenCount;
    }
}
