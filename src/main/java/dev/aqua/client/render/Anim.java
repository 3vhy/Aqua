package dev.aqua.client.render;

import dev.aqua.client.Aqua;

/**
 * Frame-rate independent animated value. {@link #tick()} is driven by the global frame clock;
 * the value approaches its target exponentially with a configurable duration (~95% settled).
 */
public final class Anim {
    private static long lastNanos = System.nanoTime();
    private static float frameDelta = 0.016f;
    private static long frameId;

    private float value;
    private float target;
    private float durationMs;
    private long lastFrame = -1;

    public Anim(float initial, float durationMs) {
        this.value = initial;
        this.target = initial;
        this.durationMs = durationMs;
    }

    public Anim(float durationMs) {
        this(0f, durationMs);
    }

    /** Advances the global frame clock. Called once per rendered frame. */
    public static void frame() {
        long now = System.nanoTime();
        frameDelta = Math.min(0.1f, Math.max(0f, (now - lastNanos) / 1.0e9f));
        lastNanos = now;
        frameId++;
    }

    public static float delta() {
        return frameDelta;
    }

    public Anim target(float t) {
        this.target = t;
        return this;
    }

    public Anim target(boolean on) {
        return target(on ? 1f : 0f);
    }

    public Anim snap(float v) {
        this.value = v;
        this.target = v;
        return this;
    }

    public Anim duration(float ms) {
        this.durationMs = ms;
        return this;
    }

    public float target() {
        return target;
    }

    /** Current value (updates at most once per frame). */
    public float get() {
        if (lastFrame != frameId) {
            lastFrame = frameId;
            float dur = durationMs * Aqua.ui().animationScale();
            if (dur <= 1f) {
                value = target;
            } else {
                // 3 time constants ~= 95% of the way within the duration.
                float k = 1f - (float) Math.exp(-frameDelta * 3000f / dur);
                value += (target - value) * k;
                if (Math.abs(target - value) < 0.0005f) {
                    value = target;
                }
            }
        }
        return value;
    }

    public boolean settled() {
        return Math.abs(get() - target) < 0.001f;
    }

    public static float easeOutCubic(float t) {
        t = clamp01(t);
        float f = 1f - t;
        return 1f - f * f * f;
    }

    public static float easeInOutCubic(float t) {
        t = clamp01(t);
        return t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
    }

    public static float easeOutBack(float t) {
        t = clamp01(t);
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        return 1f + c3 * (float) Math.pow(t - 1f, 3) + c1 * (float) Math.pow(t - 1f, 2);
    }

    public static float clamp01(float t) {
        return Math.max(0f, Math.min(1f, t));
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
