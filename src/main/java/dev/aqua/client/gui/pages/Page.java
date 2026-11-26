package dev.aqua.client.gui.pages;

import dev.aqua.client.gui.PageHost;
import dev.aqua.client.gui.Ui;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.Glass;
import dev.aqua.client.render.R2D;
import dev.aqua.client.render.font.AquaFont;
import dev.aqua.client.theme.Theme;
import net.minecraft.client.gui.DrawContext;

/** A non-module content page of the Aqua menu. */
public interface Page {
    /** Renders starting at (x, y) and returns the y coordinate below the content. */
    float render(Ui ui, DrawContext ctx, float x, float y, float w, float alpha, PageHost menu);

    /** Card container used by pages. */
    static void card(DrawContext ctx, Theme t, float x, float y, float w, float h, float a) {
        Glass.card(ctx, x, y, w, h, 7, a, 0f, 0f);
    }

    static float heading(DrawContext ctx, Theme t, String title, String sub, float x, float y, float a) {
        R2D.text(ctx, title, x, y, 6.4f, AquaFont.Weight.SEMIBOLD, Colors.fade(t.text(), a));
        if (sub != null && !sub.isEmpty()) {
            R2D.text(ctx, sub, x, y + 9, 4.9f, AquaFont.Weight.REGULAR, Colors.fade(t.muted(), a));
            return y + 20;
        }
        return y + 11;
    }
}
