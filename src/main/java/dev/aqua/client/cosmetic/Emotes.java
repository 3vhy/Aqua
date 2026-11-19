package dev.aqua.client.cosmetic;

import org.jetbrains.annotations.Nullable;

/** Local-only emote animations applied to your own player model. */
public final class Emotes {
    public enum Emote {
        WAVE("Wave", 2400), CHEER("Cheer", 2400), DANCE("Dance", 4800), CLAP("Clap", 2400), BOW("Bow", 2000), POINT("Point", 2200);

        public final String label;
        public final long durationMs;

        Emote(String label, long durationMs) {
            this.label = label;
            this.durationMs = durationMs;
        }
    }

    @Nullable private static Emote current;
    private static long started;

    private Emotes() {}

    public static void play(Emote e) {
        current = e;
        started = System.currentTimeMillis();
    }

    public static void stop() {
        current = null;
    }

    @Nullable
    public static Emote current() {
        if (current != null && System.currentTimeMillis() - started > current.durationMs) {
            current = null;
        }
        return current;
    }

    /** Seconds since the emote started. */
    public static float time() {
        return (System.currentTimeMillis() - started) / 1000f;
    }

    /** 0..1 blend in/out envelope. */
    public static float weight() {
        Emote e = current();
        if (e == null) {
            return 0f;
        }
        float t = (System.currentTimeMillis() - started) / (float) e.durationMs;
        float in = Math.min(1f, t * e.durationMs / 250f);
        float out = Math.min(1f, (1f - t) * e.durationMs / 250f);
        return Math.max(0f, Math.min(in, out));
    }
}
