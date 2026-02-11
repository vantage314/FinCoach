package com.fincoach.core.common;

public class UserContext {
    private static final ThreadLocal<Long> userHolder = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        userHolder.set(userId);
    }

    public static Long getCurrentUserId() {
        return userHolder.get();
    }

    public static void clear() {
        userHolder.remove();
    }
}


