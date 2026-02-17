package com.fincoach.core.healthv2.dto.admin;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AdminScoreRuleSaveParamsRequest {
    private Long ruleSetId;
    private Integer fromVersion;
    private List<AdminScoreRuleParamDTO> params = new ArrayList<>();
}
