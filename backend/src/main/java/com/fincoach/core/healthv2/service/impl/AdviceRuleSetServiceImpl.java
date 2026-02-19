package com.fincoach.core.healthv2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fincoach.core.healthv2.advice.rules.AdviceRuleDefaults;
import com.fincoach.core.healthv2.advice.rules.AdviceRuleRegistry;
import com.fincoach.core.healthv2.advice.rules.AdviceRuleSnapshot;
import com.fincoach.core.healthv2.entity.FcAdviceRuleParamEntity;
import com.fincoach.core.healthv2.entity.FcAdviceRuleSetEntity;
import com.fincoach.core.healthv2.mapper.FcAdviceRuleParamMapper;
import com.fincoach.core.healthv2.mapper.FcAdviceRuleSetMapper;
import com.fincoach.core.healthv2.service.AdviceRuleSetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class AdviceRuleSetServiceImpl implements AdviceRuleSetService {

    private final FcAdviceRuleSetMapper ruleSetMapper;
    private final FcAdviceRuleParamMapper paramMapper;
    private final AdviceRuleRegistry registry;

    public AdviceRuleSetServiceImpl(FcAdviceRuleSetMapper ruleSetMapper,
                                    FcAdviceRuleParamMapper paramMapper,
                                    AdviceRuleRegistry registry) {
        this.ruleSetMapper = ruleSetMapper;
        this.paramMapper = paramMapper;
        this.registry = registry;
    }

    @Override
    public List<FcAdviceRuleSetEntity> list() {
        return ruleSetMapper.selectList(
                new LambdaQueryWrapper<FcAdviceRuleSetEntity>()
                        .orderByDesc(FcAdviceRuleSetEntity::getCreatedAt));
    }

    @Override
    @Transactional
    public Long create(FcAdviceRuleSetEntity entity) {
        if (entity == null) return null;
        String code = entity.getCode() == null || entity.getCode().isBlank()
                ? AdviceRuleDefaults.DEFAULT_CODE
                : entity.getCode().trim();
        Integer maxVersion = ruleSetMapper.selectMaxVersion(code);
        int nextVersion = maxVersion == null ? 1 : maxVersion + 1;
        LocalDateTime now = LocalDateTime.now();
        FcAdviceRuleSetEntity draft = new FcAdviceRuleSetEntity();
        draft.setCode(code);
        draft.setVersion(nextVersion);
        draft.setEnabled(0);
        draft.setDescription(entity.getDescription());
        draft.setCreatedAt(now);
        draft.setUpdatedAt(now);
        ruleSetMapper.insert(draft);
        return draft.getId();
    }

    @Override
    @Transactional
    public void updateMeta(Long id, FcAdviceRuleSetEntity entity) {
        if (id == null || entity == null) return;
        FcAdviceRuleSetEntity existing = ruleSetMapper.selectById(id);
        if (existing == null) return;
        String oldCode = existing.getCode();
        String newCode = entity.getCode();
        if (newCode != null && !newCode.isBlank() && !newCode.equals(oldCode)) {
            ruleSetMapper.update(null, new LambdaUpdateWrapper<FcAdviceRuleSetEntity>()
                    .eq(FcAdviceRuleSetEntity::getCode, oldCode)
                    .set(FcAdviceRuleSetEntity::getCode, newCode));
            paramMapper.update(null, new LambdaUpdateWrapper<FcAdviceRuleParamEntity>()
                    .eq(FcAdviceRuleParamEntity::getRuleSetCode, oldCode)
                    .set(FcAdviceRuleParamEntity::getRuleSetCode, newCode));
            existing.setCode(newCode);
        }
        if (entity.getDescription() != null) {
            existing.setDescription(entity.getDescription());
        }
        if (entity.getVersion() != null) {
            existing.setVersion(entity.getVersion());
        }
        existing.setUpdatedAt(LocalDateTime.now());
        ruleSetMapper.updateById(existing);
    }

    @Override
    @Transactional
    public void enable(Long id) {
        if (id == null) return;
        FcAdviceRuleSetEntity target = ruleSetMapper.selectById(id);
        if (target == null) return;
        LocalDateTime now = LocalDateTime.now();
        ruleSetMapper.update(null, new LambdaUpdateWrapper<FcAdviceRuleSetEntity>()
                .eq(FcAdviceRuleSetEntity::getEnabled, 1)
                .set(FcAdviceRuleSetEntity::getEnabled, 0)
                .set(FcAdviceRuleSetEntity::getUpdatedAt, now));

        target.setEnabled(1);
        target.setUpdatedAt(now);
        ruleSetMapper.updateById(target);
        registry.reload();
        log.info("[AdviceRuleSet] enabled: code={}, version={}", target.getCode(), target.getVersion());
    }

    @Override
    public List<FcAdviceRuleParamEntity> listParams(String ruleSetCode) {
        if (ruleSetCode == null || ruleSetCode.isBlank()) return new ArrayList<>();
        return paramMapper.selectList(new LambdaQueryWrapper<FcAdviceRuleParamEntity>()
                .eq(FcAdviceRuleParamEntity::getRuleSetCode, ruleSetCode));
    }

    @Override
    @Transactional
    public void upsertParams(String ruleSetCode, List<FcAdviceRuleParamEntity> params) {
        if (ruleSetCode == null || ruleSetCode.isBlank() || params == null) return;
        for (FcAdviceRuleParamEntity param : params) {
            if (param == null || param.getParamKey() == null) continue;
            FcAdviceRuleParamEntity existing = paramMapper.selectOne(
                    new LambdaQueryWrapper<FcAdviceRuleParamEntity>()
                            .eq(FcAdviceRuleParamEntity::getRuleSetCode, ruleSetCode)
                            .eq(FcAdviceRuleParamEntity::getParamKey, param.getParamKey())
                            .last("LIMIT 1"));
            param.setRuleSetCode(ruleSetCode);
            param.setUpdatedAt(LocalDateTime.now());
            if (existing == null) {
                paramMapper.insert(param);
            } else {
                param.setId(existing.getId());
                paramMapper.updateById(param);
            }
        }
        ruleSetMapper.update(null, new LambdaUpdateWrapper<FcAdviceRuleSetEntity>()
                .eq(FcAdviceRuleSetEntity::getCode, ruleSetCode)
                .set(FcAdviceRuleSetEntity::getUpdatedAt, LocalDateTime.now()));
        registry.reload();
    }

    @Override
    public AdviceRuleSnapshot getActiveSnapshot() {
        return registry.get();
    }

    @Override
    public void reload() {
        registry.reload();
    }
}
