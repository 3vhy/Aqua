package dev.aqua.client.module.visual;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.R2D;
import dev.aqua.client.render.ScreenOverlay;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.setting.NumberSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;

/**
 * Low Health Warning (reference video): a pulsing red edge around the screen and a soft heartbeat
 * when your own health is low. It only uses your own health, which the game already shows you.
 */
public final class LowHealthWarning extends Module implements ScreenOverlay {
    private final NumberSetting hearts = add(new NumberSetting("hearts", "Warn below", 4, 1, 10, 0.5).unit(" hearts"));
    private final ColorSetting color = add(new ColorSetting("color", "Colour", 0xFFFF3B3B, false, false));
    private final BoolSetting pulse = add(new BoolSetting("pulse", "Pulse", true));
    private final BoolSetting sound = add(new BoolSetting("sound", "Heartbeat sound", true));
    private boolean wasLow;
    private long lastBeat;
    private long shown;

    public LowHealthWarning() {
        super("low_health_warning", "Low Health Warning", "Red pulsing screen edges and a heartbeat when your health is low.",
                Category.VISUALS, false, "health", "low hp", "warning", "vignette");
    }

    /** True while your health is under the limit. */
    public boolean low() {
        return mc.player != null && !mc.player.isCreative() && !mc.player.isSpectator() && mc.player.isAlive()
                && mc.player.getHealth() <= hearts.f() * 2f;
    }

    /** Frames the warning was drawn (used by the in-game tests). */
    public long shown() {
        return shown;
    }

    @Override
    public void onTick() {
        boolean low = low();
        long now = System.currentTimeMillis();
        if (low && sound.on() && (!wasLow || now - lastBeat > 1400)) {
            lastBeat = now;
            if (mc.player != null) {
                mc.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASEDRUM.value(), 0.5f, 0.55f);
            }
        }
        wasLow = low;
    }

    @Override
    public void renderOverlay(DrawContext ctx, float tickDelta) {
        if (!low()) {
            return;
        }
        shown++;
        float t = (System.currentTimeMillis() % 1400L) / 1400f;
        float beat = pulse.on() ? (float) Math.pow(Math.max(0, Math.sin(t * Math.PI * 2)), 2) : 0.6f;
        float danger = 1f - Math.max(0f, Math.min(1f, mc.player.getHealth() / (hearts.f() * 2f)));
        float a = 0.28f + 0.22f * danger + 0.25f * beat;
        int c = color.color();
        float w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
        float e = Math.min(w, h) * 0.16f;
        R2D.roundGradientV(ctx, 0, 0, w, e, 0, Colors.withAlpha(c, a), Colors.withAlpha(c, 0f));
        R2D.roundGradientV(ctx, 0, h - e, w, e, 0, Colors.withAlpha(c, 0f), Colors.withAlpha(c, a));
        R2D.roundGradientH(ctx, 0, 0, e, h, 0, Colors.withAlpha(c, a), Colors.withAlpha(c, 0f));
        R2D.roundGradientH(ctx, w - e, 0, e, h, 0, Colors.withAlpha(c, 0f), Colors.withAlpha(c, a));
    }
}
