package dev.aqua.client.module.perf;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.NumberSetting;

/** Lowers the frame rate while the game window is not focused, saving power and heat. */
public final class UnfocusedFps extends Module {
    private final NumberSetting fps = add(new NumberSetting("fps", "Unfocused FPS", 30, 5, 120, 5));

    public UnfocusedFps() {
        super("unfocused_fps", "Background FPS Limit", "Caps the frame rate when the window is in the background.",
                Category.PERFORMANCE, false, "fps", "background", "battery", "focus");
    }

    public int limit(int current) {
        if (isEnabled() && !mc.isWindowFocused()) {
            return Math.min(current, fps.i());
        }
        return current;
    }
}
