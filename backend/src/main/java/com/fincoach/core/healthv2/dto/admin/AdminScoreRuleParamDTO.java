package com.fincoach.core.healthv2.dto.admin;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminScoreRuleParamDTO {
    private Long id;
    private Long ruleSetId;
    private String paramKey;
    private String paramValue;
    private String valueType;
    private String minValue;
    private String maxValue;
    private String description;
    private String source;
    private LocalDateTime updatedAt;
}
