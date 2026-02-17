package com.fincoach.core.healthv2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("fc_score_rule_param")
public class FcScoreRuleParamEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ruleSetId;
    private String paramKey;
    private String paramValue;
    private String valueType;
    private String minValue;
    private String maxValue;
    private String description;
    private LocalDateTime updatedAt;
}
