package com.fincoach.core.healthv2.rebalance;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RebalanceTemplateSnapshot {
    public static final String SOURCE_DB_ACTIVE = "DB_ACTIVE";
    public static final String SOURCE_FALLBACK_DEFAULT = "FALLBACK_DEFAULT";

    private final Long templateId;
    private final String code;
    private final int version;
    private final String source;
    private final Map<String, Double> targets;
    private final List<String> warnings;

    public RebalanceTemplateSnapshot(Long templateId,
                                     String code,
                                     int version,
                                     String source,
                                     Map<String, Double> targets,
                                     List<String> warnings) {
        this.templateId = templateId;
        this.code = code;
        this.version = version;
        this.source = source;
        this.targets = targets == null ? new LinkedHashMap<>() : targets;
        this.warnings = warnings == null ? new ArrayList<>() : warnings;
    }

    public Long getTemplateId() { return templateId; }
    public String getCode() { return code; }
    public int getVersion() { return version; }
    public String getSource() { return source; }
    public Map<String, Double> getTargets() { return targets; }
    public List<String> getWarnings() { return warnings; }
}
