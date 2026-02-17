package com.fincoach.core.healthv2.service;

import com.fincoach.core.healthv2.entity.FcScoreRuleParamEntity;
import com.fincoach.core.healthv2.entity.FcScoreRuleSetEntity;
import com.fincoach.core.healthv2.rules.ScoreRuleSnapshot;

import java.util.List;

public interface ScoreRuleSetService {
    ScoreRuleSnapshot getActiveSnapshot();
    FcScoreRuleSetEntity getActiveRuleSet();
    List<FcScoreRuleParamEntity> listParams(Long ruleSetId);
    FcScoreRuleSetEntity draftNewVersion(Integer fromVersion);
    void upsertParams(Long ruleSetId, List<FcScoreRuleParamEntity> params);
    void publish(Long ruleSetId);
    void reload();
}
