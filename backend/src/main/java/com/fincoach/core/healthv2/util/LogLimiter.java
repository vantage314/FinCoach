package com.fincoach.core.healthv2.util;

public final class LogLimiter {
    private LogLimiter() {
    }

    public static String truncate(String value, int maxChars) {
        if (value == null) {
            return null;
        }
        if (maxChars <= 0) {
            return "";
        }
        if (value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, maxChars);
    }
}
