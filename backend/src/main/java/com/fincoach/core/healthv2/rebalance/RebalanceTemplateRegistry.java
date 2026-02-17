package com.fincoach.core.healthv2.rebalance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fincoach.core.healthv2.entity.FcRebalanceTemplateEntity;
import com.fincoach.core.healthv2.mapper.FcRebalanceTemplateMapper;
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
public class RebalanceTemplateRegistry implements InitializingBean {

    public static final String WARN_TEMPLATE_FALLBACK_DEFAULT = "REBALANCE_TEMPLATE_FALLBACK_DEFAULT";
    public static final String WARN_TEMPLATE_JSON_INVALID = "REBALANCE_TEMPLATE_JSON_INVALID";

    private final FcRebalanceTemplateMapper templateMapper;
    private final ObjectMapper objectMapper;
    private final AtomicReference<RebalanceTemplateSnapshot> ref = new AtomicReference<>();

    public RebalanceTemplateRegistry(FcRebalanceTemplateMapper templateMapper,
                                     ObjectMapper objectMapper) {
        this.templateMapper = templateMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterPropertiesSet() {
        reload();
    }

    public void reload() {
        ref.set(loadActive());
    }

    public RebalanceTemplateSnapshot getActive() {
        RebalanceTemplateSnapshot snapshot = ref.get();
        if (snapshot == null) {
            snapshot = buildDefaultSnapshot(new ArrayList<>());
            ref.set(snapshot);
        }
        return snapshot;
    }

    public List<FcRebalanceTemplateEntity> listAll() {
        return templateMapper.selectList(
                new LambdaQueryWrapper<FcRebalanceTemplateEntity>()
                        .orderByDesc(FcRebalanceTemplateEntity::getCreatedAt));
    }

    private RebalanceTemplateSnapshot loadActive() {
        List<String> warnings = new ArrayList<>();
        try {
            FcRebalanceTemplateEntity active = templateMapper.selectActive();
            if (active == null) {
                warnings.add(WARN_TEMPLATE_FALLBACK_DEFAULT);
                return buildDefaultSnapshot(warnings);
            }
            Map<String, Double> targets = parseTemplate(active.getTemplateJson(), warnings);
            return new RebalanceTemplateSnapshot(
                    active.getId(),
                    active.getCode(),
                    active.getVersion() == null ? RebalanceTemplateDefaults.DEFAULT_VERSION : active.getVersion(),
                    RebalanceTemplateSnapshot.SOURCE_DB_ACTIVE,
                    targets,
                    warnings);
        } catch (Exception e) {
            log.warn("[RebalanceTemplateRegistry] load active failed, fallback default", e);
            warnings.add(WARN_TEMPLATE_FALLBACK_DEFAULT);
            return buildDefaultSnapshot(warnings);
        }
    }

    private Map<String, Double> parseTemplate(String raw, List<String> warnings) {
        if (raw == null || raw.isBlank()) {
            warnings.add(WARN_TEMPLATE_JSON_INVALID);
            return new LinkedHashMap<>(RebalanceTemplateDefaults.defaultTargets());
        }
        try {
            Map<String, Double> parsed = objectMapper.readValue(raw, new TypeReference<Map<String, Double>>() {});
            if (parsed == null || parsed.isEmpty()) {
                warnings.add(WARN_TEMPLATE_JSON_INVALID);
                return new LinkedHashMap<>(RebalanceTemplateDefaults.defaultTargets());
            }
            return new LinkedHashMap<>(parsed);
        } catch (Exception e) {
            warnings.add(WARN_TEMPLATE_JSON_INVALID);
            return new LinkedHashMap<>(RebalanceTemplateDefaults.defaultTargets());
        }
    }

    private RebalanceTemplateSnapshot buildDefaultSnapshot(List<String> warnings) {
        return new RebalanceTemplateSnapshot(
                null,
                RebalanceTemplateDefaults.DEFAULT_CODE,
                RebalanceTemplateDefaults.DEFAULT_VERSION,
                RebalanceTemplateSnapshot.SOURCE_FALLBACK_DEFAULT,
                new LinkedHashMap<>(RebalanceTemplateDefaults.defaultTargets()),
                warnings);
    }
}
