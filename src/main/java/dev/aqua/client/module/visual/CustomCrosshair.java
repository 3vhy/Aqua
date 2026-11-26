package dev.aqua.client.module.visual;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.R2D;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.setting.EnumSetting;
import dev.aqua.client.setting.NumberSetting;
import net.minecraft.client.gui.DrawContext;

/** Crisp, customisable crosshair (R4 0:54 "Crosshair"). Purely cosmetic - no aiming aid. */
public final class CustomCrosshair extends Module {
    public enum Style implements EnumSetting.Displayable {
        CROSS("Cross"), CROSS_DOT("Cross + dot"), DOT("Dot"), CIRCLE("Circle"), T("T-shape");
        final String l;

        Style(String l) {
            this.l = l;
        }

        public String displayName() {
            return l;
        }
    }

    private final EnumSetting<Style> style = add(new EnumSetting<>("style", "Style", Style.CROSS));
    private final ColorSetting color = add(new ColorSetting("color", "Colour", 0xFFFFFFFF, true, false));
    private final NumberSetting size = add(new NumberSetting("size", "Size", 5, 2, 12, 0.5));
    private final NumberSetting gap = add(new NumberSetting("gap", "Gap", 2, 0, 8, 0.5));
    private final NumberSetting thickness = add(new NumberSetting("thickness", "Thickness", 1, 0.5, 3, 0.25));
    private final BoolSetting outline = add(new BoolSetting("outline", "Outline", true));

    public CustomCrosshair() {
        super("crosshair", "Custom Crosshair", "A crisp, fully customisable crosshair.", Category.HUD, false, "crosshair", "reticle", "aim");
    }

    public void draw(DrawContext ctx) {
        if (!mc.options.getPerspective().isFirstPerson() || mc.options.hudHidden) {
            return;
        }
        float cx = ctx.getScaledWindowWidth() / 2f;
        float cy = ctx.getScaledWindowHeight() / 2f;
        int c = color.color();
        int o = Colors.withAlpha(0x000000, Colors.a(c) * 0.55f);
        float s = size.f(), g = gap.f(), t = thickness.f();
        if (outline.on()) {
            shape(ctx, cx, cy, s, g, t + 1f, o, true);
        }
        shape(ctx, cx, cy, s, g, t, c, false);
    }

    private void shape(DrawContext ctx, float cx, float cy, float s, float g, float t, int c, boolean outlinePass) {
        float pad = outlinePass ? 0.5f : 0f;
        switch (style.get()) {
            case DOT -> R2D.circle(ctx, cx, cy, t + pad + 0.3f, c);
            case CIRCLE -> R2D.ring(ctx, cx, cy, s, t, c);
            default -> {
                if (style.get() != Style.T) {
                    R2D.round(ctx, cx - t / 2f, cy - g - s - pad, t, s + pad * 2, t / 2f, c);
                }
                R2D.round(ctx, cx - t / 2f, cy + g - pad, t, s + pad * 2, t / 2f, c);
                R2D.round(ctx, cx - g - s - pad, cy - t / 2f, s + pad * 2, t, t / 2f, c);
                R2D.round(ctx, cx + g - pad, cy - t / 2f, s + pad * 2, t, t / 2f, c);
                if (style.get() == Style.CROSS_DOT) {
                    R2D.circle(ctx, cx, cy, t * 0.7f + pad, c);
                }
            }
        }
    }
}
