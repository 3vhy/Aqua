package dev.aqua.client.module.visual;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Colors;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.setting.NumberSetting;

/**
 * Fog colour (R1/R2 "Custom Fog"), adapted: it can only make fog thicker, never thinner than vanilla,
 * so it cannot reveal anything vanilla fog would hide.
 */
public final class CustomFog extends Module {
    private final ColorSetting color = add(new ColorSetting("color", "Colour", 0xFF7FD6E3, false, true));
    private final NumberSetting blend = add(new NumberSetting("blend", "Colour strength", 0.6, 0.0, 1.0, 0.05));
    private final NumberSetting density = add(new NumberSetting("density", "Extra thickness", 0.0, 0.0, 1.0, 0.05)
            .describe("0 keeps vanilla distance; higher values bring fog closer"));

    public CustomFog() {
        super("custom_fog", "Custom Fog", "Changes fog colour and can make it thicker.", Category.VISUALS, false,
                "fog", "atmosphere", "haze");
    }

    public int apply(int original) {
        return Colors.lerp(original, Colors.withAlpha(color.color(), Colors.alpha(original)), blend.f());
    }

    /** Never returns a value further than the original. */
    public float end(float original) {
        float target = Math.max(8f, original * (1f - 0.85f * density.f()));
        return Math.min(original, target);
    }
}
