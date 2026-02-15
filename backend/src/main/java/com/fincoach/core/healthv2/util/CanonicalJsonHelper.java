package com.fincoach.core.healthv2.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class CanonicalJsonHelper {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CanonicalJsonHelper() {}

    public static String toCanonicalJson(Object input) {
        try {
            Object jsonLike = toJsonCompatible(input);
            Object canonical = canonicalize(jsonLike);
            return MAPPER.writeValueAsString(canonical);
        } catch (Exception e) {
            return null;
        }
    }

    private static Object toJsonCompatible(Object input) {
        if (input == null) {
            return null;
        }
        if (input instanceof Map<?, ?> || input instanceof List<?> || input instanceof String
                || input instanceof Number || input instanceof Boolean) {
            return input;
        }
        if (input.getClass().isArray()) {
            int len = java.lang.reflect.Array.getLength(input);
            List<Object> list = new ArrayList<>(len);
            for (int i = 0; i < len; i++) {
                list.add(java.lang.reflect.Array.get(input, i));
            }
            return list;
        }
        return MAPPER.convertValue(input, Object.class);
    }

    @SuppressWarnings("unchecked")
    private static Object canonicalize(Object input) {
        if (input == null) {
            return null;
        }
        if (input instanceof Map<?, ?> map) {
            TreeMap<String, Object> sorted = new TreeMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = entry.getKey() == null ? "null" : entry.getKey().toString();
                sorted.put(key, canonicalize(entry.getValue()));
            }
            return sorted;
        }
        if (input instanceof List<?> list) {
            List<Object> out = new ArrayList<>(list.size());
            for (Object item : list) {
                out.add(canonicalize(item));
            }
            return out;
        }
        return input;
    }
}
