package dev.aqua.client.module.visual;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Colors;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.setting.NumberSetting;
import net.minecraft.client.gui.DrawContext;

/**
 * "World Lighting" (R1) adapted to colour grading: a light tint over the world. It never increases
 * brightness or gamma.
 */
public final class WorldTint extends Module {
    private final ColorSetting color = add(new ColorSetting("color", "Tint", 0xFF52E4F2, false, true));
    private final NumberSetting strength = add(new NumberSetting("strength", "Strength", 0.12, 0.02, 0.35, 0.01));

    public WorldTint() {
        super("world_tint", "World Tint", "Colour-grades the world with a subtle tint.", Category.VISUALS, false,
                "world lighting", "colour grading", "filter");
    }

    public void draw(DrawContext ctx) {
        if (mc.options.hudHidden && mc.currentScreen == null) {
            // Still tint when the HUD is hidden (F1) - it is part of the world look.
        }
        ctx.fill(0, 0, ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight(), Colors.withAlpha(color.color(), strength.f()));
    }
}
