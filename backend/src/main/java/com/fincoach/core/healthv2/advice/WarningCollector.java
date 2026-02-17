package com.fincoach.core.healthv2.advice;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WarningCollector {
    private final List<WarningItem> items = new ArrayList<>();
    private final Set<String> codes = new LinkedHashSet<>();

    public void add(String code) {
        add(code, null);
    }

    public void add(String code, String detail) {
        if (code == null || code.isBlank()) return;
        if (codes.contains(code)) return;
        codes.add(code);
        items.add(new WarningItem(code, detail));
    }

    public void addAll(List<String> warnings) {
        if (warnings == null) return;
        for (String w : warnings) {
            add(w);
        }
    }

    public List<String> codes() {
        return new ArrayList<>(codes);
    }

    public List<Map<String, Object>> detailMaps() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (WarningItem item : items) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("code", item.code);
            map.put("detail", item.detail == null ? "" : item.detail);
            out.add(map);
        }
        return out;
    }

    public static final class WarningItem {
        private final String code;
        private final String detail;

        private WarningItem(String code, String detail) {
            this.code = code;
            this.detail = detail;
        }

        public String getCode() { return code; }
        public String getDetail() { return detail; }
    }
}
