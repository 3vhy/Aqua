package dev.aqua.client.module.perf;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.BoolSetting;

/**
 * Performance (reference video): lighter background work while keeping every effect and feature.
 * The glass blur is made every other frame with fewer passes, Aqua's particles are capped lower and
 * idle caches are trimmed. Turn it off to compare FPS.
 */
public final class PerformanceMode extends Module {
    private final BoolSetting lightBlur = add(new BoolSetting("light_blur", "Lighter glass blur", true)
            .describe("Glass blur every other frame, with at most two passes"));
    private final BoolSetting fewerParticles = add(new BoolSetting("fewer_particles", "Fewer Aqua particles", true)
            .describe("Aqua's own particles are capped at 75%"));

    public PerformanceMode() {
        super("performance_mode", "Performance", "Reduces background work and image memory while keeping effects and features. Disable to compare FPS.",
                Category.PERFORMANCE, false, "performance", "fps", "optimise", "optimize", "boost");
    }

    public boolean lightBlur() {
        return isEnabled() && lightBlur.on();
    }

    public float particleFactor() {
        return isEnabled() && fewerParticles.on() ? 0.75f : 1f;
    }
}
