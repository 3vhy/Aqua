package dev.aqua.client.util;

/** Smoothed frame time from the render loop. */
public final class FrameStats {
    private static long last = System.nanoTime();
    private static double avgMs = 16.6;

    private FrameStats() {}

    public static void frame() {
        long now = System.nanoTime();
        double ms = (now - last) / 1.0e6;
        last = now;
        if (ms > 0 && ms < 1000) {
            avgMs = avgMs * 0.92 + ms * 0.08;
        }
    }

    public static double frameTimeMs() {
        return avgMs;
    }
}
