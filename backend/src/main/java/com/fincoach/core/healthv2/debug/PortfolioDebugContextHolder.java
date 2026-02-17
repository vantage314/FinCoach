package com.fincoach.core.healthv2.debug;

import java.util.function.Consumer;

public class PortfolioDebugContextHolder {

    private static final ThreadLocal<Context> CONTEXT = new ThreadLocal<>();

    public static void enableCapture() {
        Context ctx = CONTEXT.get();
        if (ctx == null) {
            ctx = new Context();
        }
        ctx.captureEnabled = true;
        CONTEXT.set(ctx);
    }

    public static void disableCapture() {
        Context ctx = CONTEXT.get();
        if (ctx == null) {
            ctx = new Context();
        }
        ctx.captureEnabled = false;
        ctx.snapshot = null;
        CONTEXT.set(ctx);
    }

    public static boolean isCaptureEnabled() {
        Context ctx = CONTEXT.get();
        return ctx != null && ctx.captureEnabled;
    }

    public static void set(PortfolioMarketDebugSnapshot snapshot) {
        if (!isCaptureEnabled()) {
            return;
        }
        Context ctx = CONTEXT.get();
        if (ctx == null) {
            ctx = new Context();
            ctx.captureEnabled = true;
        }
        ctx.snapshot = snapshot;
        CONTEXT.set(ctx);
    }

    public static PortfolioMarketDebugSnapshot get() {
        Context ctx = CONTEXT.get();
        if (ctx == null || !ctx.captureEnabled) {
            return null;
        }
        return ctx.snapshot;
    }

    public static PortfolioMarketDebugSnapshot getOrCreate() {
        if (!isCaptureEnabled()) {
            return null;
        }
        Context ctx = CONTEXT.get();
        if (ctx == null) {
            ctx = new Context();
            ctx.captureEnabled = true;
        }
        if (ctx.snapshot == null) {
            ctx.snapshot = new PortfolioMarketDebugSnapshot();
        }
        CONTEXT.set(ctx);
        return ctx.snapshot;
    }

    public static void record(Consumer<PortfolioMarketDebugSnapshot> recorder) {
        if (recorder == null || !isCaptureEnabled()) {
            return;
        }
        PortfolioMarketDebugSnapshot snapshot = getOrCreate();
        if (snapshot == null) {
            return;
        }
        recorder.accept(snapshot);
    }

    public static void clear() {
        CONTEXT.remove();
    }

    private static class Context {
        private boolean captureEnabled;
        private PortfolioMarketDebugSnapshot snapshot;
    }
}
