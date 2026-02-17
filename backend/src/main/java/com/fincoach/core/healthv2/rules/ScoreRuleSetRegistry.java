package com.fincoach.core.healthv2.rules;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fincoach.core.healthv2.entity.FcScoreRuleParamEntity;
import com.fincoach.core.healthv2.entity.FcScoreRuleSetEntity;
import com.fincoach.core.healthv2.mapper.FcScoreRuleParamMapper;
import com.fincoach.core.healthv2.mapper.FcScoreRuleSetMapper;
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
public class ScoreRuleSetRegistry implements InitializingBean {

    public static final String WARN_FALLBACK_DEFAULT = "SCORE_RULESET_FALLBACK_DEFAULT";
    public static final String WARN_PARAM_MISSING_PREFIX = "SCORE_RULE_PARAM_MISSING:";
    public static final String WARN_PARAM_INVALID_PREFIX = "SCORE_RULE_PARAM_INVALID:";

    private final FcScoreRuleSetMapper ruleSetMapper;
    private final FcScoreRuleParamMapper paramMapper;
    private final AtomicReference<ScoreRuleSnapshot> ref = new AtomicReference<>();

    public ScoreRuleSetRegistry(FcScoreRuleSetMapper ruleSetMapper,
                                FcScoreRuleParamMapper paramMapper) {
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

    public ScoreRuleSnapshot get() {
        ScoreRuleSnapshot snapshot = ref.get();
        if (snapshot == null) {
            snapshot = buildDefaultSnapshot(new ArrayList<>());
            ref.set(snapshot);
        }
        return snapshot;
    }

    private ScoreRuleSnapshot loadActive() {
        List<String> warnings = new ArrayList<>();
        try {
            FcScoreRuleSetEntity active = ruleSetMapper.selectActive();
            if (active == null) {
                warnings.add(WARN_FALLBACK_DEFAULT);
                return buildDefaultSnapshot(warnings);
            }
            List<FcScoreRuleParamEntity> params = paramMapper.selectList(
                    new LambdaQueryWrapper<FcScoreRuleParamEntity>()
                            .eq(FcScoreRuleParamEntity::getRuleSetId, active.getId()));
            return buildSnapshotFrom(active, params, ScoreRuleSnapshot.SOURCE_DB_ACTIVE, warnings);
        } catch (Exception e) {
            log.warn("[ScoreRuleSetRegistry] load active failed, fallback default", e);
            warnings.add(WARN_FALLBACK_DEFAULT);
            return buildDefaultSnapshot(warnings);
        }
    }

    private ScoreRuleSnapshot buildDefaultSnapshot(List<String> warnings) {
        Map<String, ScoreRuleParamValue> paramValues = new LinkedHashMap<>();
        List<String> missing = new ArrayList<>();
        for (ScoreRuleParamDefinition def : ScoreRuleDefaults.defaultParams().values()) {
            paramValues.put(def.getKey(), ScoreRuleParamValue.fromDefinition(def, "DEFAULT"));
            missing.add(def.getKey());
        }
        return new ScoreRuleSnapshot(null,
                ScoreRuleDefaults.DEFAULT_CODE,
                ScoreRuleDefaults.DEFAULT_VERSION,
                ScoreRuleSnapshot.SOURCE_FALLBACK_DEFAULT,
                paramValues,
                warnings,
                missing);
    }

    private ScoreRuleSnapshot buildSnapshotFrom(FcScoreRuleSetEntity active,
                                                List<FcScoreRuleParamEntity> params,
                                                String source,
                                                List<String> warnings) {
        Map<String, FcScoreRuleParamEntity> byKey = new LinkedHashMap<>();
        if (params != null) {
            for (FcScoreRuleParamEntity p : params) {
                if (p.getParamKey() != null) {
                    byKey.put(p.getParamKey(), p);
                }
            }
        }

        Map<String, ScoreRuleParamValue> values = new LinkedHashMap<>();
        List<String> missing = new ArrayList<>();

        for (ScoreRuleParamDefinition def : ScoreRuleDefaults.defaultParams().values()) {
            FcScoreRuleParamEntity entity = byKey.remove(def.getKey());
            if (entity == null) {
                values.put(def.getKey(), ScoreRuleParamValue.fromDefinition(def, "DEFAULT"));
                missing.add(def.getKey());
                warnings.add(WARN_PARAM_MISSING_PREFIX + def.getKey());
                continue;
            }
            String[] error = new String[1];
            ScoreRuleParamValue parsed = ScoreRuleParamValue.fromEntity(entity, def, "DB", error);
            if (parsed == null || error[0] != null) {
                warnings.add(WARN_PARAM_INVALID_PREFIX + def.getKey());
                values.put(def.getKey(), ScoreRuleParamValue.fromDefinition(def, "DEFAULT"));
            } else {
                values.put(def.getKey(), parsed);
            }
        }

        for (FcScoreRuleParamEntity entity : byKey.values()) {
            String[] error = new String[1];
            ScoreRuleParamValue parsed = ScoreRuleParamValue.fromEntity(entity, null, "DB", error);
            if (parsed != null) {
                values.put(entity.getParamKey(), parsed);
            } else {
                warnings.add(WARN_PARAM_INVALID_PREFIX + entity.getParamKey());
            }
        }

        return new ScoreRuleSnapshot(
                active.getId(),
                active.getCode(),
                active.getVersion() == null ? ScoreRuleDefaults.DEFAULT_VERSION : active.getVersion(),
                source,
                values,
                warnings,
                missing);
    }
}
