package dev.aqua.client.module.qol;

import dev.aqua.client.Aqua;
import dev.aqua.client.hud.widgets.WatermarkWidget;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.R2D;
import dev.aqua.client.render.font.AquaFont;
import dev.aqua.client.setting.BoolSetting;
import net.minecraft.client.gui.DrawContext;

/**
 * Media Hider (R7): covers your hotbar (and optionally inventory screens) with an Aqua banner for
 * videos and streams. Your items still work; they are just not visible on screen.
 */
public final class MediaHider extends Module {
    private final BoolSetting inventories = add(new BoolSetting("inventories", "Also cover inventory screens", false));

    public MediaHider() {
        super("media_hider", "Media Hider", "Covers your hotbar with an Aqua banner for videos and streams.", Category.QOL, false,
                "media hider", "stream", "record", "hide hotbar", "hide inventory", "no inv leak");
        net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen<?>) {
                net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.afterRender(screen).register((s, ctx, mx, my, delta) -> {
                    if (coverInventories() && s instanceof dev.aqua.client.mixin.HandledScreenAccessor a) {
                        banner(ctx, a.aqua$x(), a.aqua$y(), a.aqua$width(), a.aqua$height());
                    }
                });
            }
        });
    }

    private long covered;

    /** Frames in which the hotbar was covered (used by the in-game tests). */
    public long covered() {
        return covered;
    }

    public boolean coverInventories() {
        return isEnabled() && inventories.on();
    }

    /** Covers the hotbar area (drawn just after the hotbar). */
    public void drawHotbarCover(DrawContext ctx) {
        if (!isEnabled() || mc.options.hudHidden || mc.player == null || mc.player.isSpectator()) {
            return;
        }
        covered++;
        float w = 184, h = 24;
        float x = ctx.getScaledWindowWidth() / 2f - w / 2f, y = ctx.getScaledWindowHeight() - h - 0.5f;
        banner(ctx, x, y, w, h);
    }

    public void banner(DrawContext ctx, float x, float y, float w, float h) {
        var t = Aqua.theme();
        R2D.roundGradientH(ctx, x, y, w, h, 6, Colors.darken(t.accent(), 0.55f), Colors.darken(t.accent2(), 0.55f));
        R2D.round(ctx, x + 1, y + 1, w - 2, h - 2, 5, Colors.withAlpha(t.background(), 0.82f));
        float s = Math.min(h - 8, 16);
        R2D.texture(ctx, WatermarkWidget.EMBLEM, x + w / 2f - 30 - s / 2f, y + h / 2f - s / 2f, s, s, 0xFFFFFFFF);
        R2D.textMid(ctx, Aqua.NAME + " Client", x + w / 2f - 18, y + h / 2f, 7f, AquaFont.Weight.BOLD, t.text());
    }
}
