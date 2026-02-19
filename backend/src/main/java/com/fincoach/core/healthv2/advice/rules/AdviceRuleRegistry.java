package com.fincoach.core.healthv2.advice.rules;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fincoach.core.healthv2.entity.FcAdviceRuleParamEntity;
import com.fincoach.core.healthv2.entity.FcAdviceRuleSetEntity;
import com.fincoach.core.healthv2.mapper.FcAdviceRuleParamMapper;
import com.fincoach.core.healthv2.mapper.FcAdviceRuleSetMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
public class AdviceRuleRegistry implements InitializingBean {

    public static final String WARN_FALLBACK_DEFAULT = "ADVICE_RULESET_FALLBACK_DEFAULT";
    public static final String WARN_PARAM_MISSING_PREFIX = "ADVICE_RULE_PARAM_MISSING:";
    public static final String WARN_PARAM_INVALID_PREFIX = "ADVICE_RULE_PARAM_INVALID:";

    private final FcAdviceRuleSetMapper ruleSetMapper;
    private final FcAdviceRuleParamMapper paramMapper;
    private final AtomicReference<AdviceRuleSnapshot> ref = new AtomicReference<>();

    public AdviceRuleRegistry(FcAdviceRuleSetMapper ruleSetMapper,
                              FcAdviceRuleParamMapper paramMapper) {
        this.ruleSetMapper = ruleSetMapper;
        this.paramMapper = paramMapper;
    }

    @Override
    public void afterPropertiesSet() {
        reload();
    }

    public void reload() {
        ref.set(loadActive());
    }

    public AdviceRuleSnapshot get() {
        AdviceRuleSnapshot snapshot = ref.get();
        if (snapshot == null) {
            snapshot = buildDefaultSnapshot(new ArrayList<>());
            ref.set(snapshot);
        }
        return snapshot;
    }

    private AdviceRuleSnapshot loadActive() {
        List<String> warnings = new ArrayList<>();
        try {
            FcAdviceRuleSetEntity active = ruleSetMapper.selectActive();
            if (active == null) {
                warnings.add(WARN_FALLBACK_DEFAULT);
                return buildDefaultSnapshot(warnings);
            }
            List<FcAdviceRuleParamEntity> params = paramMapper.selectList(
                    new LambdaQueryWrapper<FcAdviceRuleParamEntity>()
                            .eq(FcAdviceRuleParamEntity::getRuleSetCode, active.getCode()));
            return buildSnapshotFrom(active, params, AdviceRuleSnapshot.SOURCE_DB_ACTIVE, warnings);
        } catch (Exception e) {
            log.warn("[AdviceRuleRegistry] load active failed, fallback default", e);
            warnings.add(WARN_FALLBACK_DEFAULT);
            return buildDefaultSnapshot(warnings);
        }
    }

    private AdviceRuleSnapshot buildDefaultSnapshot(List<String> warnings) {
        Map<String, AdviceRuleParamValue> values = new LinkedHashMap<>();
        List<String> missing = new ArrayList<>();
        for (AdviceRuleParamDefinition def : AdviceRuleDefaults.defaultParams().values()) {
            values.put(def.getKey(), AdviceRuleParamValue.fromDefinition(def, "DEFAULT"));
            missing.add(def.getKey());
        }
        return new AdviceRuleSnapshot(null,
                AdviceRuleDefaults.DEFAULT_CODE,
                AdviceRuleDefaults.DEFAULT_VERSION,
                AdviceRuleSnapshot.SOURCE_FALLBACK_DEFAULT,
                values,
                warnings,
                missing);
    }

    private AdviceRuleSnapshot buildSnapshotFrom(FcAdviceRuleSetEntity active,
                                                 List<FcAdviceRuleParamEntity> params,
                                                 String source,
                                                 List<String> warnings) {
        Map<String, FcAdviceRuleParamEntity> byKey = new LinkedHashMap<>();
        if (params != null) {
            for (FcAdviceRuleParamEntity p : params) {
                if (p.getParamKey() != null) {
                    byKey.put(p.getParamKey(), p);
                }
            }
        }

        Map<String, AdviceRuleParamValue> values = new LinkedHashMap<>();
        List<String> missing = new ArrayList<>();

        for (AdviceRuleParamDefinition def : AdviceRuleDefaults.defaultParams().values()) {
            FcAdviceRuleParamEntity entity = byKey.remove(def.getKey());
            if (entity == null) {
                values.put(def.getKey(), AdviceRuleParamValue.fromDefinition(def, "DEFAULT"));
                missing.add(def.getKey());
                warnings.add(WARN_PARAM_MISSING_PREFIX + def.getKey());
                continue;
            }
            AdviceRuleValueType type = AdviceRuleValueType.from(entity.getValueType());
            if (type == null) {
                type = def.getValueType();
            }
            AdviceRuleParamValue parsed = AdviceRuleParamValue.parse(entity.getParamKey(), type, entity.getParamValue(), "DB");
            if (parsed == null) {
                warnings.add(WARN_PARAM_INVALID_PREFIX + def.getKey());
                values.put(def.getKey(), AdviceRuleParamValue.fromDefinition(def, "DEFAULT"));
            } else {
                values.put(def.getKey(), parsed);
            }
        }

        for (FcAdviceRuleParamEntity entity : byKey.values()) {
            AdviceRuleValueType type = AdviceRuleValueType.from(entity.getValueType());
            AdviceRuleParamValue parsed = AdviceRuleParamValue.parse(entity.getParamKey(), type, entity.getParamValue(), "DB");
            if (parsed != null) {
                values.put(entity.getParamKey(), parsed);
            } else {
                warnings.add(WARN_PARAM_INVALID_PREFIX + entity.getParamKey());
            }
        }

        return new AdviceRuleSnapshot(
                active.getId(),
                active.getCode(),
                active.getVersion() == null ? AdviceRuleDefaults.DEFAULT_VERSION : active.getVersion(),
                source,
                values,
                warnings,
                missing);
    }
}
