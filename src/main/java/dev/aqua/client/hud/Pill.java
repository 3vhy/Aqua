package dev.aqua.client.hud;

import dev.aqua.client.Aqua;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.Glass;
import dev.aqua.client.render.Icon;
import dev.aqua.client.render.R2D;
import dev.aqua.client.render.font.AquaFont;
import dev.aqua.client.theme.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Fern-style capsule: [icon] value unit • value unit … on a dark translucent rounded background.
 * Values are bright SemiBold, units are smaller and muted, separators are muted dots.
 */
public final class Pill {
    public static final float HEIGHT = 13f;
    public static final float RADIUS = 4.6f;
    public static final float PAD_X = 5.5f;
    public static final float VALUE = 6.2f;
    public static final float UNIT = 5.0f;
    public static final float ICON = 7f;
    public static final float GAP = 3f;

    private record Part(int kind, String text, Icon icon, Identifier tex, int color) {}

    private final List<Part> parts = new ArrayList<>();

    public static Pill create() {
        return new Pill();
    }

    public Pill icon(Icon icon) {
        parts.add(new Part(0, null, icon, null, 0));
        return this;
    }

    public Pill logo(Identifier tex) {
        parts.add(new Part(5, null, null, tex, 0));
        return this;
    }

    public Pill value(String s) {
        parts.add(new Part(1, s, null, null, 0));
        return this;
    }

    public Pill value(String s, int color) {
        parts.add(new Part(1, s, null, null, color));
        return this;
    }

    public Pill unit(String s) {
        parts.add(new Part(2, s, null, null, 0));
        return this;
    }

    public Pill muted(String s) {
        parts.add(new Part(4, s, null, null, 0));
        return this;
    }

    public Pill dot() {
        parts.add(new Part(3, null, null, null, 0));
        return this;
    }

    public float width() {
        float w = PAD_X * 2;
        Part prev = null;
        for (Part p : parts) {
            if (prev != null) {
                w += spacing(prev, p);
            }
            w += partWidth(p);
            prev = p;
        }
        return w;
    }

    private static float spacing(Part a, Part b) {
        if (b.kind == 2) {
            return 1.6f;
        }
        if (a.kind == 3 || b.kind == 3) {
            return 3.2f;
        }
        if (a.kind == 0 || a.kind == 5) {
            return 3.6f;
        }
        return GAP;
    }

    private static float partWidth(Part p) {
        return switch (p.kind) {
            case 0 -> ICON;
            case 5 -> ICON + 0.5f;
            case 1 -> AquaFont.width(p.text, VALUE, AquaFont.Weight.SEMIBOLD);
            case 2 -> AquaFont.width(p.text, UNIT, AquaFont.Weight.MEDIUM);
            case 4 -> AquaFont.width(p.text, VALUE, AquaFont.Weight.MEDIUM);
            default -> 1.6f;
        };
    }

    /** Draws the capsule at (x, y) and returns its width. */
    public float draw(DrawContext ctx, float x, float y, boolean bg, int bgColor, float alpha) {
        Theme t = Aqua.theme();
        float w = width();
        if (bg) {
            Glass.hud(ctx, x, y, w, HEIGHT, RADIUS, bgColor);
        }
        float cx = x + PAD_X;
        float cy = y + HEIGHT / 2f;
        Part prev = null;
        for (Part p : parts) {
            if (prev != null) {
                cx += spacing(prev, p);
            }
            switch (p.kind) {
                case 0 -> R2D.icon(ctx, p.icon, cx, cy - ICON / 2f, ICON, Colors.fade(t.text(), alpha * 0.92f));
                case 5 -> R2D.texture(ctx, p.tex, cx, cy - ICON / 2f - 0.5f, ICON + 0.5f, ICON + 0.5f, Colors.fade(0xFFFFFFFF, alpha));
                case 1 -> R2D.textMid(ctx, p.text, cx, cy, VALUE, AquaFont.Weight.SEMIBOLD,
                        Colors.fade(p.color != 0 ? p.color : t.text(), alpha));
                case 2 -> R2D.textMid(ctx, p.text, cx, cy + 0.5f, UNIT, AquaFont.Weight.MEDIUM, Colors.fade(t.muted(), alpha * 0.9f));
                case 4 -> R2D.textMid(ctx, p.text, cx, cy, VALUE, AquaFont.Weight.MEDIUM, Colors.fade(t.text(), alpha * 0.92f));
                default -> R2D.circle(ctx, cx + 0.8f, cy, 0.8f, Colors.fade(t.muted(), alpha * 0.8f));
            }
            cx += partWidth(p);
            prev = p;
        }
        return w;
    }
}
