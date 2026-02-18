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
        return value.substring(value.length() - maxChars);
    }

    public static String appendAndTruncate(String oldLog, String newLine, int maxChars) {
        if (newLine == null || newLine.isBlank()) {
            return truncate(oldLog, maxChars);
        }
        String combined;
        if (oldLog == null || oldLog.isBlank()) {
            combined = newLine;
        } else {
            combined = oldLog + "\n" + newLine;
        }
        return truncate(combined, maxChars);
    }
}
