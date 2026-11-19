package dev.aqua.client.gui.pages;

import dev.aqua.client.Aqua;
import dev.aqua.client.cosmetic.Emotes;
import dev.aqua.client.gui.AquaMenuScreen;
import dev.aqua.client.gui.Ui;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.Icon;
import dev.aqua.client.render.R2D;
import dev.aqua.client.render.font.AquaFont;
import dev.aqua.client.theme.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;

/**
 * Live preview of your own player with Aqua's original cosmetics (R4 0:57 wardrobe, adapted: no
 * third-party character models). Emotes can be previewed here and in third person.
 */
public final class WardrobePage implements Page {
    @Override
    public float render(Ui ui, DrawContext ctx, float x, float y, float w, float a, AquaMenuScreen menu) {
        Theme t = Aqua.theme();
        MinecraftClient mc = MinecraftClient.getInstance();
        float pw = 110;
        float ph = 150;
        Page.card(ctx, t, x, y, pw, ph, a);
        R2D.roundGradientV(ctx, x + 3, y + 3, pw - 6, ph - 6, 5, Colors.fade(Colors.withAlpha(t.accent(), 0.10f), a),
                Colors.fade(Colors.withAlpha(t.background(), 0.0f), a));
        if (mc.player != null) {
            int x1 = Math.round(x + 5);
            int y1 = Math.round(y + 8);
            InventoryScreen.drawEntity(ctx, x1, y1, Math.round(x + pw - 5), Math.round(y + ph - 18), 52, 0.0625f,
                    ui.mx, ui.my, mc.player);
        } else {
            R2D.textCentered(ctx, "Join a world", x + pw / 2f, y + ph / 2f - 5, 6f, AquaFont.Weight.SEMIBOLD, Colors.fade(t.text(), a));
            R2D.textCentered(ctx, "to preview your player", x + pw / 2f, y + ph / 2f + 5, 5f, AquaFont.Weight.MEDIUM, Colors.fade(t.muted(), a));
        }
        R2D.textCentered(ctx, "Only you see Aqua cosmetics", x + pw / 2f, y + ph - 9, 4.4f, AquaFont.Weight.MEDIUM, Colors.fade(t.muted(), a));

        // Cosmetic toggles
        float lx = x + pw + 6;
        float lw = w - pw - 6;
        float cy = y;
        for (Module m : Aqua.modules().inCategory(Category.COSMETICS)) {
            if (!m.id().equals("wings") && !m.id().equals("halo") && !m.id().equals("cape") && !m.id().equals("dash_trail")
                    && !m.id().equals("jump_circles") && !m.id().equals("emotes")) {
                continue;
            }
            float h = 22;
            Page.card(ctx, t, lx, cy, lw, h, a);
            R2D.text(ctx, m.name(), lx + 8, cy + 4.5f, 6f, AquaFont.Weight.SEMIBOLD, Colors.fade(t.text(), a));
            R2D.text(ctx, AquaFont.ellipsize(m.description(), lw - 40, 4.6f, AquaFont.Weight.REGULAR), lx + 8, cy + 13, 4.6f,
                    AquaFont.Weight.REGULAR, Colors.fade(t.muted(), a));
            boolean on = ui.toggle("wd" + m.id(), lx + lw - 25, cy + h / 2f - 4.75f, m.isEnabled());
            if (on != m.isEnabled()) {
                m.setEnabled(on, false);
            }
            cy += h + 3;
        }
        // Emote preview buttons
        float rowsW = 0;
        int rows = 1;
        for (Emotes.Emote e : Emotes.Emote.values()) {
            float bw = R2D.width(e.label, 5.4f, AquaFont.Weight.SEMIBOLD) + 12;
            if (rowsW + bw > lw - 14) {
                rows++;
                rowsW = 0;
            }
            rowsW += bw + 3;
        }
        float eh = 24 + rows * 14;
        Page.card(ctx, t, lx, cy, lw, eh, a);
        R2D.text(ctx, "Emotes", lx + 8, cy + 4.5f, 6f, AquaFont.Weight.SEMIBOLD, Colors.fade(t.text(), a));
        R2D.text(ctx, "Preview here, or hold the emote wheel key in game", lx + 8, cy + 13, 4.6f, AquaFont.Weight.REGULAR,
                Colors.fade(t.muted(), a));
        float bx = lx + 8;
        float by = cy + 22;
        for (Emotes.Emote e : Emotes.Emote.values()) {
            float bw = R2D.width(e.label, 5.4f, AquaFont.Weight.SEMIBOLD) + 12;
            if (bx + bw > lx + lw - 6) {
                bx = lx + 8;
                by += 14;
            }
            if (ui.button("em" + e, bx, by, bw, 11, e.label, null, Emotes.current() == e)) {
                Emotes.play(e);
            }
            bx += bw + 3;
        }
        cy += eh + 3;
        R2D.icon(ctx, Icon.INFO, lx, cy + 1, 6, Colors.fade(t.muted(), a));
        R2D.text(ctx, "More cosmetics and their options are on the Cosmetics page.", lx + 9, cy + 1.5f, 4.6f, AquaFont.Weight.REGULAR,
                Colors.fade(t.muted(), a));
        return Math.max(y + ph, cy + 10) + 4;
    }
}
