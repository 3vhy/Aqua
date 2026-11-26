package dev.aqua.client.module.qol;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;

/**
 * "Sprint" (R1) adapted to a plain toggle: while enabled it holds the vanilla sprint key for you,
 * exactly like holding it yourself. Vanilla still decides whether sprinting is allowed (hunger,
 * sneaking, using items, direction). No omni-directional sprinting.
 */
public final class ToggleSprint extends Module {
    public ToggleSprint() {
        super("toggle_sprint", "Sprint", "Keeps the sprint key held so you do not have to.", Category.QOL, false,
                "sprint", "auto sprint", "toggle sprint");
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.currentScreen != null) {
            return;
        }
        if (mc.options.forwardKey.isPressed() && !mc.options.sneakKey.isPressed()) {
            mc.options.sprintKey.setPressed(true);
        }
    }

    @Override
    protected void onDisable() {
        mc.options.sprintKey.setPressed(false);
    }
}
