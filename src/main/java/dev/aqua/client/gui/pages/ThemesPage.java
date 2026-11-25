package dev.aqua.client.gui.pages;

import dev.aqua.client.Aqua;
import dev.aqua.client.gui.AquaMenuScreen;
import dev.aqua.client.gui.Ui;
import dev.aqua.client.gui.UiSounds;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.Glass;
import dev.aqua.client.render.Icon;
import dev.aqua.client.render.R2D;
import dev.aqua.client.render.font.AquaFont;
import dev.aqua.client.theme.Theme;
import net.minecraft.client.gui.DrawContext;

/** Preset palettes and the custom palette editor. */
public final class ThemesPage implements Page {
    private static final String[] FIELDS = {"background", "panel", "elevated", "accent", "accent2", "text", "muted"};
    private static final String[] LABELS = {"Background", "Panel", "Elevated panel", "Primary accent", "Secondary accent", "Primary text", "Muted text"};

    @Override
    public float render(Ui ui, DrawContext ctx, float x, float y, float w, float a, AquaMenuScreen menu) {
        Theme cur = Aqua.theme();
        int cols = 3;
        float gap = 5;
        float cw = (w - gap * (cols - 1)) / cols;
        float ch = 44;
        Theme[] all = new Theme[Theme.PRESETS.length + 1];
        System.arraycopy(Theme.PRESETS, 0, all, 0, Theme.PRESETS.length);
        all[all.length - 1] = Aqua.themes().custom();
        for (int i = 0; i < all.length; i++) {
            Theme th = all[i];
            float cx = x + (i % cols) * (cw + gap);
            float cy = y + (i / cols) * (ch + gap);
            boolean sel = Aqua.themes().presetName().equals(th.name());
            boolean hov = ui.hover(cx, cy, cw, ch);
            float ha = ui.anim("th" + i, 120, 0).target(hov).get();
            if (sel) {
                R2D.shadow(ctx, cx, cy, cw, ch, 7, 9, Colors.fade(Colors.withAlpha(th.accent(), 0.3f), a));
            }
            R2D.roundGradientV(ctx, cx, cy, cw, ch, 7, Colors.fade(Colors.withAlpha(Colors.lerp(th.background(), th.accent(), 0.08f), 0.92f), a),
                    Colors.fade(Colors.withAlpha(th.background(), 0.96f), a));
            R2D.outlineGradientV(ctx, cx, cy, cw, ch, 7, sel ? 1f : 0.6f, R2D.ALL,
                    Colors.fade(sel ? th.accent() : Colors.withAlpha(0xFFFFFF, 0.16f + ha * 0.12f), a),
                    Colors.fade(sel ? Colors.withAlpha(th.accent(), 0.5f) : Colors.withAlpha(0xFFFFFF, 0.05f + ha * 0.05f), a));
            // Mini preview: panel, accent pill, text lines
            R2D.round(ctx, cx + 6, cy + 6, cw * 0.42f, ch - 12, 4, Colors.fade(th.panel(), a));
            R2D.round(ctx, cx + 10, cy + 11, cw * 0.3f, 3, 1.5f, Colors.fade(th.text(), a));
            R2D.round(ctx, cx + 10, cy + 17, cw * 0.22f, 2.5f, 1.25f, Colors.fade(th.muted(), a));
            R2D.round(ctx, cx + 10, cy + ch - 15, 13, 7, 3.5f, Colors.fade(th.accent(), a));
            R2D.circle(ctx, cx + 19.5f, cy + ch - 11.5f, 2.5f, Colors.fade(0xFFFFFFFF, a));
            R2D.text(ctx, th.name(), cx + cw * 0.42f + 12, cy + 9, 6.2f, AquaFont.Weight.SEMIBOLD, Colors.fade(th.text(), a));
            for (int k = 0; k < 4; k++) {
                int c = switch (k) {
                    case 0 -> th.accent();
                    case 1 -> th.accent2();
                    case 2 -> th.elevated();
                    default -> th.muted();
                };
                R2D.circle(ctx, cx + cw * 0.42f + 15 + k * 8, cy + ch - 12, 3, Colors.fade(c, a));
            }
            if (sel) {
                R2D.icon(ctx, Icon.CHECK, cx + cw - 12, cy + 6, 7, Colors.fade(cur.accent(), a));
            }
            if (ui.clickedIn(cx, cy, cw, ch)) {
                Aqua.themes().select(th.name());
                UiSounds.click(1.1f);
            }
        }
        int rows = (all.length + cols - 1) / cols;
        y += rows * (ch + gap) + 2;

        // Custom palette editor
        float h = 26 + FIELDS.length * 14 + 20;
        Page.card(ctx, cur, x, y, w, h, a);
        Page.heading(ctx, cur, "Custom palette", "Edit the colours of the Custom theme", x + 9, y + 6, a);
        float ry = y + 26;
        Theme custom = Aqua.themes().custom();
        for (int i = 0; i < FIELDS.length; i++) {
            String f = FIELDS[i];
            R2D.textMid(ctx, LABELS[i], x + 9, ry + 6, 5.5f, AquaFont.Weight.MEDIUM, Colors.fade(cur.text(), a));
            R2D.textRight(ctx, Colors.hex(custom.get(f), false), x + w - 36, ry + 6, 5f, AquaFont.Weight.MEDIUM, Colors.fade(cur.muted(), a));
            ui.colorButton("tc" + f, x + w - 31, ry + 0.5f, custom.get(f), c -> Aqua.themes().setCustom(Aqua.themes().custom().with(f, c)));
            ry += 14;
        }
        if (ui.button("tc_copy", x + 9, ry + 2, 92, 11, "Copy current theme", Icon.COPY, false)) {
            Aqua.themes().setCustom(cur);
            Aqua.themes().select("Custom");
        }
        if (ui.button("tc_reset", x + w - 9 - 62, ry + 2, 62, 11, "Reset custom", Icon.RESET, false)) {
            Aqua.themes().setCustom(Theme.AQUA);
        }
        return y + h + 4;
    }
}
