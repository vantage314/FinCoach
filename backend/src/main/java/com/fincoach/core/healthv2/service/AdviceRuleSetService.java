package com.fincoach.core.healthv2.service;

import com.fincoach.core.healthv2.entity.FcAdviceRuleParamEntity;
import com.fincoach.core.healthv2.entity.FcAdviceRuleSetEntity;
import com.fincoach.core.healthv2.advice.rules.AdviceRuleSnapshot;

import java.util.List;

public interface AdviceRuleSetService {
    List<FcAdviceRuleSetEntity> list();
    Long create(FcAdviceRuleSetEntity entity);
    void updateMeta(Long id, FcAdviceRuleSetEntity entity);
    void enable(Long id);
    List<FcAdviceRuleParamEntity> listParams(String ruleSetCode);
    void upsertParams(String ruleSetCode, List<FcAdviceRuleParamEntity> params);
    AdviceRuleSnapshot getActiveSnapshot();
    void reload();
}
