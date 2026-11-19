package dev.aqua.client.module.visual;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Colors;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.setting.NumberSetting;

/** Recoloured rain and snow streaks (R1 0:18-0:22). */
public final class CustomRain extends Module {
    private final ColorSetting color = add(new ColorSetting("color", "Colour", 0xFFFF8A4C, false, true));
    private final NumberSetting opacity = add(new NumberSetting("opacity", "Opacity", 1.0, 0.1, 1.0, 0.05));

    public CustomRain() {
        super("custom_rain", "Custom Rain", "Tints rain and snow in your theme colour.", Category.VISUALS, false,
                "rain", "snow", "weather colour");
    }

    public int tint(int original) {
        float a = Colors.a(original) * opacity.f();
        return Colors.withAlpha(color.color(), a);
    }
}
