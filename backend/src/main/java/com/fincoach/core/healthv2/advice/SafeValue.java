package com.fincoach.core.healthv2.advice;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SafeValue {
    private static final String NA = "N/A";

    private SafeValue() {
    }

    public static <T> List<T> listOrEmpty(List<T> input) {
        return input == null ? new ArrayList<>() : input;
    }

    public static <K, V> Map<K, V> mapOrEmpty(Map<K, V> input) {
        return input == null ? new LinkedHashMap<>() : input;
    }

    public static Object valueOrNA(Object value) {
        return value == null ? NA : value;
    }
}
