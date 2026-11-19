package dev.aqua.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.server.integrated.IntegratedServer;

/**
 * Server tick rate. Exact in singleplayer (integrated server tick time). On remote servers it is an
 * estimate derived from the interval between world-time packets (sent every 20 server ticks).
 */
public final class TpsTracker {
    private static long lastPacket;
    private static double estimate = -1;
    private static int samples;

    private TpsTracker() {}

    public static void onWorldTimePacket() {
        long now = System.nanoTime();
        if (lastPacket != 0) {
            double seconds = (now - lastPacket) / 1.0e9;
            if (seconds > 0.2 && seconds < 30) {
                double tps = Math.min(20.0, 20.0 / seconds);
                estimate = estimate < 0 ? tps : estimate * 0.7 + tps * 0.3;
                samples++;
            }
        }
        lastPacket = now;
    }

    public static void reset() {
        lastPacket = 0;
        estimate = -1;
        samples = 0;
    }

    /** True when the value is measured exactly (integrated server). */
    public static boolean exact() {
        IntegratedServer s = MinecraftClient.getInstance().getServer();
        return s != null;
    }

    /** TPS or -1 when unknown. */
    public static double tps() {
        IntegratedServer s = MinecraftClient.getInstance().getServer();
        if (s != null) {
            float mspt = s.getAverageTickTime();
            if (mspt <= 0) {
                return 20.0;
            }
            return Math.min(20.0, 1000.0 / Math.max(50.0, mspt));
        }
        return samples >= 2 ? estimate : -1;
    }
}
