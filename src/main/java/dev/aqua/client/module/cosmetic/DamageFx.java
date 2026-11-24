package dev.aqua.client.module.cosmetic;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.R2D;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.setting.NumberSetting;
import net.minecraft.client.gui.DrawContext;

/**
 * Damage FX: a coloured vignette pulses in from the screen edges when you take damage, and a slow
 * heartbeat glow while your health is low. Only your own health is used.
 */
public final class DamageFx extends Module {
    private final ColorSetting color = add(new ColorSetting("color", "Colour", 0xFFE0243C, false, false));
    private final NumberSetting strength = add(new NumberSetting("strength", "Strength", 0.7, 0.2, 1.0, 0.05));
    private final BoolSetting lowHealth = add(new BoolSetting("low_health", "Heartbeat glow at low health", true));
    private final NumberSetting threshold = add(new NumberSetting("threshold", "Low health below", 6, 2, 12, 1).unit(" HP")
            .visibleWhen(lowHealth::on));

    private float lastHealth = -1;
    private long hitAt = -1;
    private float hitAmount;

    public DamageFx() {
        super("damage_fx", "Damage FX", "A red pulse at the screen edges when you are hurt, and a heartbeat glow at low health.",
                Category.COSMETICS, false, "damage fx", "damage", "hurt", "vignette", "low health", "blood");
    }

    @Override
    public void onTick() {
        if (mc.player == null) {
            lastHealth = -1;
            return;
        }
        float h = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        if (lastHealth >= 0 && h < lastHealth - 0.01f) {
            hitAt = System.currentTimeMillis();
            hitAmount = Math.min(1f, 0.35f + (lastHealth - h) / 8f);
        }
        lastHealth = h;
    }

    /** True for a moment after taking damage (used by the in-game tests). */
    public boolean pulsing() {
        return hitAt >= 0 && System.currentTimeMillis() - hitAt < 600;
    }

    public void draw(DrawContext ctx) {
        if (!isEnabled() || mc.player == null || mc.options.hudHidden || mc.player.isSpectator() || mc.player.isCreative()) {
            return;
        }
        float a = 0f;
        if (hitAt >= 0) {
            float t = (System.currentTimeMillis() - hitAt) / 600f;
            if (t >= 1f) {
                hitAt = -1;
            } else {
                a = hitAmount * (t < 0.12f ? t / 0.12f : 1f - (t - 0.12f) / 0.88f);
            }
        }
        if (lowHealth.on() && mc.player.getHealth() > 0 && mc.player.getHealth() <= threshold.f()) {
            double beat = (System.currentTimeMillis() % 1100L) / 1100.0;
            float pulse = (float) (Math.exp(-beat * 9) + 0.6 * Math.exp(-Math.max(0, beat - 0.22) * 9) * (beat > 0.22 ? 1 : 0));
            float low = 1f - mc.player.getHealth() / threshold.f();
            a = Math.max(a, (0.18f + 0.35f * low) * Math.min(1f, pulse + 0.25f));
        }
        a *= strength.f() * Math.min(1.2f, Aqua.ui().effectIntensity.f() + 0.2f);
        if (a <= 0.01f) {
            return;
        }
        float w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
        float e = Math.min(w, h) * 0.22f;
        int c = Colors.fade(color.color(), Math.min(1f, a));
        int clear = Colors.withAlpha(color.color(), 0f);
        R2D.roundGradientV(ctx, 0, 0, w, e, 0, c, clear);
        R2D.roundGradientV(ctx, 0, h - e, w, e, 0, clear, c);
        R2D.roundGradientH(ctx, 0, 0, e, h, 0, c, clear);
        R2D.roundGradientH(ctx, w - e, 0, e, h, 0, clear, c);
    }
}
