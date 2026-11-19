package dev.aqua.client.util;

/** Tracks the start of the current world session. */
public final class Session {
    private static long joined = -1;

    private Session() {}

    public static void join() {
        joined = System.currentTimeMillis();
    }

    public static void leave() {
        joined = -1;
    }

    public static long durationMs() {
        return joined < 0 ? -1 : System.currentTimeMillis() - joined;
    }

    public static String format(long ms) {
        if (ms < 0) {
            return "N/A";
        }
        long s = ms / 1000;
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, sec) : String.format("%02d:%02d", m, sec);
    }
}
