package dev.aqua.client.module.qol;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.NumberSetting;

/**
 * Slow Swing (reference video): controls how fast your hand swing animation plays. Only the
 * animation on your own screen changes; attack timing is unaffected.
 */
public final class SlowSwing extends Module {
    private final NumberSetting speed = add(new NumberSetting("speed", "Swing speed", 0.5, 0.2, 1.0, 0.05).unit("x")
            .describe("Lower is slower"));

    public SlowSwing() {
        super("slow_swing", "Slow Swing", "Control hand swing animation speed.", Category.QOL, false,
                "swing", "slow", "animation", "slow swing");
    }

    public int swingDuration(int vanilla) {
        return Math.max(1, Math.round(vanilla / speed.f()));
    }
}
