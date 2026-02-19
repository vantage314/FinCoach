package com.fincoach.core.healthv2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("fc_advice_rule_param")
public class FcAdviceRuleParamEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String ruleSetCode;
    private String paramKey;
    private String paramValue;
    private String valueType;
    private LocalDateTime updatedAt;
}
