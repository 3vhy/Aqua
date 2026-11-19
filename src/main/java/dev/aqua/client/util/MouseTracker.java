package dev.aqua.client.util;

/** Smoothed in-game mouse movement (real input only) for the keystrokes mouse indicator. */
public final class MouseTracker {
    private static double vx;
    private static double vy;

    private MouseTracker() {}

    /** Called with the raw cursor delta consumed by the camera this frame. */
    public static void onDelta(double dx, double dy, double seconds) {
        if (seconds <= 0) {
            return;
        }
        double ix = dx / Math.max(seconds, 1e-3);
        double iy = dy / Math.max(seconds, 1e-3);
        double k = Math.min(1.0, seconds * 18.0);
        vx += (ix - vx) * k;
        vy += (iy - vy) * k;
    }

    /** Decays towards rest when no movement arrives. */
    public static void decay(double seconds) {
        double k = Math.exp(-seconds * 10.0);
        vx *= k;
        vy *= k;
    }

    /** Normalised direction offset in [-1, 1] for each axis. */
    public static float x() {
        return (float) Math.tanh(vx / 900.0);
    }

    public static float y() {
        return (float) Math.tanh(vy / 900.0);
    }
}
