package com.fincoach.core.healthv2.debug;

public class PortfolioDebugContextHolder {

    private static final ThreadLocal<PortfolioMarketDebugSnapshot> HOLDER = new ThreadLocal<>();

    public static void set(PortfolioMarketDebugSnapshot snapshot) {
        HOLDER.set(snapshot);
    }

    public static PortfolioMarketDebugSnapshot get() {
        return HOLDER.get();
    }

    public static PortfolioMarketDebugSnapshot getOrCreate() {
        PortfolioMarketDebugSnapshot snapshot = HOLDER.get();
        if (snapshot == null) {
            snapshot = new PortfolioMarketDebugSnapshot();
            HOLDER.set(snapshot);
        }
        return snapshot;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
