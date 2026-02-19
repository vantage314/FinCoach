package com.fincoach.core.healthv2.dto.admin;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminAdviceRuleParamDTO {
    private Long id;
    private String ruleSetCode;
    private String paramKey;
    private String paramValue;
    private String valueType;
    private LocalDateTime updatedAt;
}
