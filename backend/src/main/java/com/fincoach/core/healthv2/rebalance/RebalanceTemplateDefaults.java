package com.fincoach.core.healthv2.rebalance;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RebalanceTemplateDefaults {
    private RebalanceTemplateDefaults() {}

    public static final String DEFAULT_CODE = "BALANCED";
    public static final String DEFAULT_NAME = "Balanced Template";
    public static final int DEFAULT_VERSION = 1;

    private static final Map<String, Double> DEFAULT_TARGETS;

    static {
        Map<String, Double> targets = new LinkedHashMap<>();
        targets.put("CASH", 0.15);
        targets.put("BOND", 0.30);
        targets.put("STOCK", 0.35);
        targets.put("GOLD", 0.10);
        targets.put("ETF", 0.05);
        targets.put("OTHER", 0.05);
        DEFAULT_TARGETS = Collections.unmodifiableMap(targets);
    }

    public static Map<String, Double> defaultTargets() {
        return DEFAULT_TARGETS;
    }
}
