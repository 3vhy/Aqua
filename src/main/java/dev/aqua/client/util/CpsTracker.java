package dev.aqua.client.util;


/** Counts real mouse clicks (fed by the mouse mixin) over a sliding one-second window. */
public final class CpsTracker {
    private static final long[] left = new long[64];
    private static final long[] right = new long[64];
    private static int li;
    private static int ri;

    private CpsTracker() {}

    public static void click(int button) {
        long now = System.currentTimeMillis();
        if (button == 0) {
            left[li++ & 63] = now;
        } else if (button == 1) {
            right[ri++ & 63] = now;
        }
    }

    private static int count(long[] arr) {
        long now = System.currentTimeMillis();
        int c = 0;
        for (long t : arr) {
            if (t != 0 && now - t <= 1000) {
                c++;
            }
        }
        return c;
    }

    public static int left() {
        return count(left);
    }

    public static int right() {
        return count(right);
    }
}
