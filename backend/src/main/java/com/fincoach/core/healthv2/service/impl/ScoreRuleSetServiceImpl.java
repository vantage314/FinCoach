package com.fincoach.core.healthv2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fincoach.core.healthv2.entity.FcScoreRuleParamEntity;
import com.fincoach.core.healthv2.entity.FcScoreRuleSetEntity;
import com.fincoach.core.healthv2.mapper.FcScoreRuleParamMapper;
import com.fincoach.core.healthv2.mapper.FcScoreRuleSetMapper;
import com.fincoach.core.healthv2.rules.ScoreRuleDefaults;
import com.fincoach.core.healthv2.rules.ScoreRuleParamDefinition;
import com.fincoach.core.healthv2.rules.ScoreRuleSetRegistry;
import com.fincoach.core.healthv2.rules.ScoreRuleSnapshot;
import com.fincoach.core.healthv2.service.ScoreRuleSetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ScoreRuleSetServiceImpl implements ScoreRuleSetService {

    private final FcScoreRuleSetMapper ruleSetMapper;
    private final FcScoreRuleParamMapper paramMapper;
    private final ScoreRuleSetRegistry registry;

    public ScoreRuleSetServiceImpl(FcScoreRuleSetMapper ruleSetMapper,
                                   FcScoreRuleParamMapper paramMapper,
                                   ScoreRuleSetRegistry registry) {
        this.ruleSetMapper = ruleSetMapper;
        this.paramMapper = paramMapper;
        this.registry = registry;
    }

    @Override
    public ScoreRuleSnapshot getActiveSnapshot() {
        return registry.get();
    }

    @Override
    public FcScoreRuleSetEntity getActiveRuleSet() {
        return ruleSetMapper.selectActive();
    }

    @Override
    public List<FcScoreRuleParamEntity> listParams(Long ruleSetId) {
        if (ruleSetId == null) return new ArrayList<>();
        return paramMapper.selectList(new LambdaQueryWrapper<FcScoreRuleParamEntity>()
                .eq(FcScoreRuleParamEntity::getRuleSetId, ruleSetId));
    }

    @Override
    @Transactional
    public FcScoreRuleSetEntity draftNewVersion(Integer fromVersion) {
        FcScoreRuleSetEntity base = null;
        if (fromVersion != null) {
            base = ruleSetMapper.selectOne(new LambdaQueryWrapper<FcScoreRuleSetEntity>()
                    .eq(FcScoreRuleSetEntity::getCode, ScoreRuleDefaults.DEFAULT_CODE)
                    .eq(FcScoreRuleSetEntity::getVersion, fromVersion)
                    .last("LIMIT 1"));
        }
        if (base == null) {
            base = ruleSetMapper.selectActive();
        }

        int nextVersion = 1;
        Integer maxVersion = ruleSetMapper.selectMaxVersion(ScoreRuleDefaults.DEFAULT_CODE);
        if (maxVersion != null && maxVersion > 0) {
            nextVersion = maxVersion + 1;
        }

        FcScoreRuleSetEntity draft = new FcScoreRuleSetEntity();
        draft.setCode(ScoreRuleDefaults.DEFAULT_CODE);
        draft.setName(ScoreRuleDefaults.DEFAULT_NAME + " v" + nextVersion);
        draft.setVersion(nextVersion);
        draft.setEnabled(0);
        draft.setPublishedAt(null);
        draft.setCreatedAt(LocalDateTime.now());
        draft.setUpdatedAt(LocalDateTime.now());
        ruleSetMapper.insert(draft);

        List<FcScoreRuleParamEntity> params = new ArrayList<>();
        if (base != null) {
            List<FcScoreRuleParamEntity> baseParams = listParams(base.getId());
            for (FcScoreRuleParamEntity p : baseParams) {
                FcScoreRuleParamEntity copy = new FcScoreRuleParamEntity();
                copy.setRuleSetId(draft.getId());
                copy.setParamKey(p.getParamKey());
                copy.setParamValue(p.getParamValue());
                copy.setValueType(p.getValueType());
                copy.setMinValue(p.getMinValue());
                copy.setMaxValue(p.getMaxValue());
                copy.setDescription(p.getDescription());
                copy.setUpdatedAt(LocalDateTime.now());
                params.add(copy);
            }
        } else {
            for (ScoreRuleParamDefinition def : ScoreRuleDefaults.defaultParams().values()) {
                FcScoreRuleParamEntity param = new FcScoreRuleParamEntity();
                param.setRuleSetId(draft.getId());
                param.setParamKey(def.getKey());
                param.setParamValue(def.getDefaultValue());
                param.setValueType(def.getValueType().name());
                param.setMinValue(def.getMinValue() == null ? null : def.getMinValue().toPlainString());
                param.setMaxValue(def.getMaxValue() == null ? null : def.getMaxValue().toPlainString());
                param.setDescription(def.getDescription());
                param.setUpdatedAt(LocalDateTime.now());
                params.add(param);
            }
        }
        for (FcScoreRuleParamEntity param : params) {
            paramMapper.insert(param);
        }
        log.info("[ScoreRuleSet] draft created: code={}, version={}", draft.getCode(), draft.getVersion());
        return draft;
    }

    @Override
    @Transactional
    public void upsertParams(Long ruleSetId, List<FcScoreRuleParamEntity> params) {
        if (ruleSetId == null || params == null || params.isEmpty()) return;
        for (FcScoreRuleParamEntity param : params) {
            if (param == null || param.getParamKey() == null) continue;
            FcScoreRuleParamEntity existing = paramMapper.selectOne(
                    new LambdaQueryWrapper<FcScoreRuleParamEntity>()
                            .eq(FcScoreRuleParamEntity::getRuleSetId, ruleSetId)
                            .eq(FcScoreRuleParamEntity::getParamKey, param.getParamKey())
                            .last("LIMIT 1"));
            param.setRuleSetId(ruleSetId);
            param.setUpdatedAt(LocalDateTime.now());
            if (existing == null) {
                paramMapper.insert(param);
            } else {
                param.setId(existing.getId());
                paramMapper.updateById(param);
            }
        }
        ruleSetMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<FcScoreRuleSetEntity>()
                .eq(FcScoreRuleSetEntity::getId, ruleSetId)
                .set(FcScoreRuleSetEntity::getUpdatedAt, LocalDateTime.now()));
    }

    @Override
    @Transactional
    public void publish(Long ruleSetId) {
        if (ruleSetId == null) return;
        FcScoreRuleSetEntity target = ruleSetMapper.selectById(ruleSetId);
        if (target == null) return;

        LocalDateTime now = LocalDateTime.now();
        ruleSetMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<FcScoreRuleSetEntity>()
                        .eq(FcScoreRuleSetEntity::getEnabled, 1)
                        .set(FcScoreRuleSetEntity::getEnabled, 0)
                        .set(FcScoreRuleSetEntity::getUpdatedAt, now));

        target.setEnabled(1);
        target.setPublishedAt(now);
        target.setUpdatedAt(now);
        ruleSetMapper.updateById(target);

        registry.reload();
        log.info("[ScoreRuleSet] published: code={}, version={}", target.getCode(), target.getVersion());
    }

    @Override
    public void reload() {
        registry.reload();
    }
}
