package dev.aqua.client.module.visual;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.R2D;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.setting.EnumSetting;
import net.minecraft.client.gui.DrawContext;

/**
 * Attack Indicator (R8): your attack cooldown as a smooth ring or bar under the crosshair. It shows the
 * same cooldown as the vanilla indicator, just restyled.
 */
public final class AttackIndicator extends Module {
    public enum Style implements EnumSetting.Displayable {
        RING("Ring"), BAR("Bar");
        final String l;

        Style(String l) {
            this.l = l;
        }

        public String displayName() {
            return l;
        }
    }

    private final EnumSetting<Style> style = add(new EnumSetting<>("style", "Style", Style.RING));
    private final ColorSetting color = add(new ColorSetting("color", "Colour", 0xFF52E4F2, false, true));

    public AttackIndicator() {
        super("attack_indicator", "Attack Indicator", "Your attack cooldown as a smooth ring or bar at the crosshair.", Category.VISUALS, false,
                "attack indicator", "cooldown", "attack cooldown");
    }

    public void draw(DrawContext ctx) {
        if (!isEnabled() || mc.player == null || mc.options.hudHidden || !mc.options.getPerspective().isFirstPerson()) {
            return;
        }
        float p = mc.player.getAttackCooldownProgress(0f);
        if (p >= 1f) {
            return;
        }
        float cx = ctx.getScaledWindowWidth() / 2f, cy = ctx.getScaledWindowHeight() / 2f;
        int c = color.color();
        if (style.is(Style.BAR)) {
            float w = 16, y = cy + 9;
            R2D.round(ctx, cx - w / 2, y, w, 2f, 1f, Colors.fade(Aqua.theme().background(), 0.7f));
            R2D.round(ctx, cx - w / 2, y, w * p, 2f, 1f, c);
            return;
        }
        int segs = 40;
        float r = 7.5f;
        int filled = Math.round(segs * p);
        for (int i = 0; i < segs; i++) {
            double a0 = -Math.PI / 2 + Math.PI * 2 * i / segs, a1 = -Math.PI / 2 + Math.PI * 2 * (i + 1) / segs;
            int col = i < filled ? c : Colors.fade(Aqua.theme().background(), 0.55f);
            R2D.line(ctx, cx + (float) Math.cos(a0) * r, cy + (float) Math.sin(a0) * r, cx + (float) Math.cos(a1) * r, cy + (float) Math.sin(a1) * r,
                    1.4f, col);
        }
    }
}
