package dev.aqua.client.hud;

import dev.aqua.client.Aqua;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.Glass;
import dev.aqua.client.render.Icon;
import dev.aqua.client.render.R2D;
import dev.aqua.client.render.font.AquaFont;
import dev.aqua.client.theme.Theme;
import net.minecraft.client.gui.DrawContext;

/** Fern-style list panel (title + icon header, rows of label/value). */
public final class Panel {
    public static final float PAD = 6f;
    public static final float HEADER = 14f;
    public static final float ROW = 11f;
    public static final float RADIUS = 5f;
    public static final float TITLE = 6.2f;
    public static final float TEXT = 5.8f;

    private Panel() {}

    public static float height(int rows) {
        return HEADER + rows * ROW + (rows > 0 ? PAD * 0.6f : 0);
    }

    public static void header(DrawContext ctx, float w, String title, Icon icon, boolean bg, int bgColor, float alpha, int rows) {
        Theme t = Aqua.theme();
        if (bg) {
            Glass.hud(ctx, 0, 0, w, height(rows), RADIUS, bgColor);
        }
        R2D.textMid(ctx, title, PAD, HEADER / 2f + 0.5f, TITLE, AquaFont.Weight.SEMIBOLD, Colors.fade(t.text(), alpha));
        R2D.icon(ctx, icon, w - PAD - 7, HEADER / 2f - 3f, 7, Colors.fade(t.text(), alpha * 0.85f));
    }

    public static float rowY(int i) {
        return HEADER + i * ROW + ROW / 2f;
    }
}
