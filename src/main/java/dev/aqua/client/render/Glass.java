package dev.aqua.client.render;

import dev.aqua.client.Aqua;
import dev.aqua.client.theme.Theme;
import net.minecraft.client.gui.DrawContext;

/**
 * Aqua's glass design system. Every surface in the client is drawn through these helpers so the
 * Glass and Solid styles stay consistent.
 *
 * <p>A glass surface is built from four layers: the blurred frame behind it, a tint in the theme
 * colour (lighter at the top, darker at the bottom), a soft sheen across the top and a thin lit rim
 * that is bright at the top edge and fades towards the bottom. Surfaces that sit on top of another
 * glass surface (cards, rows, fields) skip the blur and only add a faint frosted layer and a rim.
 */
public final class Glass {
    private Glass() {}

    private static final int WHITE = 0xFFFFFFFF;

    /** True when the Glass style is active. */
    public static boolean on() {
        return Aqua.ui().glass();
    }

    /** True when HUD backgrounds should be glass. */
    public static boolean hud() {
        return on() && Aqua.ui().hudGlass.on();
    }

    private static float tint() {
        return Aqua.ui().glassTint.f();
    }

    /**
     * Call from a screen's background, after drawing whatever should show through the glass. With
     * the Glass style the screen keeps a sharp background and only its panels are frosted (unless
     * the whole-background blur is turned on). With the Solid style this is vanilla's menu blur.
     */
    public static void backdrop(DrawContext ctx, boolean fullBlur) {
        if (!on()) {
            if (fullBlur) {
                ctx.applyBlur();
            }
            return;
        }
        GlassBlur.setPasses(Math.round(Aqua.ui().glassBlur.f()));
        ctx.createNewRootLayer();
        ctx.applyBlur();
        GlassBlur.markBackdrop(!fullBlur);
    }

    // ------------------------------------------------------------------ frosted surfaces (with blur)

    /**
     * Frosted surface.
     *
     * @param a     overall opacity (used for fades)
     * @param tint  how strongly the theme colour covers the blur (0..1)
     * @param sheen strength of the light across the top
     * @param rim   strength of the lit border
     */
    public static void surface(DrawContext ctx, float x, float y, float w, float h, float r, int mask, float a,
                               float tint, float sheen, float rim) {
        if (a <= 0.003f || w <= 0 || h <= 0) {
            return;
        }
        Theme t = Aqua.theme();
        GlassBlur.setPasses(Math.round(Aqua.ui().glassBlur.f()));
        int base = Colors.lerp(t.background(), t.accent(), 0.06f);
        int fallback = Colors.withAlpha(base, Math.min(1f, (0.55f + tint * 0.4f) * a));
        R2D.glass(ctx, x, y, w, h, r, mask, 0.25f, Colors.fade(WHITE, a), fallback);
        R2D.roundGradientVCorners(ctx, x, y, w, h, r, mask,
                Colors.withAlpha(Colors.lerp(base, WHITE, 0.04f), Math.min(1f, tint * 0.82f) * a),
                Colors.withAlpha(Colors.darken(base, 0.18f), Math.min(1f, tint * 1.08f) * a));
        if (sheen > 0) {
            R2D.roundGradientVCorners(ctx, x, y, w, Math.min(h, Math.max(8, h * 0.45f)), r, mask & R2D.TOP,
                    Colors.withAlpha(WHITE, sheen * a), Colors.withAlpha(WHITE, 0f));
        }
        if (rim > 0) {
            R2D.outlineGradientV(ctx, x, y, w, h, r, 0.6f, mask, Colors.withAlpha(WHITE, rim * a), Colors.withAlpha(WHITE, rim * 0.28f * a));
        }
    }

    /** Large window (menus, editors). */
    public static void window(DrawContext ctx, float x, float y, float w, float h, float r, float a) {
        Theme t = Aqua.theme();
        R2D.shadow(ctx, x, y + 3, w, h, r, 26, Colors.withAlpha(0x000000, 0.42f * a));
        if (on()) {
            surface(ctx, x, y, w, h, r, R2D.ALL, a, tint(), 0.07f, 0.24f);
        } else {
            R2D.round(ctx, x, y, w, h, r, Colors.fade(t.windowBg(0.94f), a));
            R2D.outline(ctx, x, y, w, h, r, 0.7f, Colors.fade(t.border(), a));
        }
    }

    /** Popups, dropdown lists, colour pickers and tooltips: more solid so they read over anything. */
    public static void popup(DrawContext ctx, float x, float y, float w, float h, float r, float a) {
        Theme t = Aqua.theme();
        R2D.shadow(ctx, x, y + 2, w, h, r, 12, Colors.withAlpha(0x000000, 0.45f * a));
        if (on()) {
            surface(ctx, x, y, w, h, r, R2D.ALL, a, Math.min(0.92f, tint() + 0.35f), 0.06f, 0.26f);
        } else {
            R2D.round(ctx, x, y, w, h, r, Colors.withAlpha(t.panel(), 0.98f * a));
            R2D.outline(ctx, x, y, w, h, r, 0.6f, Colors.fade(t.border(), a));
        }
    }

    /** Toasts and notifications. */
    public static void toast(DrawContext ctx, float x, float y, float w, float h, float r, float a) {
        Theme t = Aqua.theme();
        R2D.shadow(ctx, x, y + 2, w, h, r, 12, Colors.withAlpha(0x000000, 0.35f * a));
        if (on()) {
            surface(ctx, x, y, w, h, r, R2D.ALL, a, Math.min(0.9f, tint() + 0.2f), 0.07f, 0.24f);
        } else {
            R2D.round(ctx, x, y, w, h, r, Colors.fade(t.windowBg(0.94f), a));
            R2D.outline(ctx, x, y, w, h, r, 0.6f, Colors.fade(t.border(), a));
        }
    }

    /**
     * HUD element background. {@code bgColor} is the widget's solid background (theme colour with
     * its opacity setting and fade already applied); glass uses its alpha as the frost strength.
     */
    public static void hud(DrawContext ctx, float x, float y, float w, float h, float r, int bgColor) {
        float a = Colors.a(bgColor);
        if (a <= 0.003f) {
            return;
        }
        if (hud()) {
            float strength = Math.min(1f, a * 1.7f);
            surface(ctx, x, y, w, h, r, R2D.ALL, strength, Math.min(0.9f, a * 0.62f), 0.045f, 0.16f);
        } else {
            R2D.round(ctx, x, y, w, h, r, bgColor);
        }
    }

    public static void hudCircle(DrawContext ctx, float cx, float cy, float radius, int bgColor) {
        hud(ctx, cx - radius, cy - radius, radius * 2, radius * 2, radius, bgColor);
    }

    // ------------------------------------------------------------------ inner surfaces (no extra blur)

    /**
     * A card or row sitting on a glass window: a faint frosted layer plus a rim. {@code hover} and
     * {@code active} (0..1) brighten it; active also tints it with the accent.
     */
    public static void card(DrawContext ctx, float x, float y, float w, float h, float r, float a, float hover, float active) {
        Theme t = Aqua.theme();
        if (on()) {
            int fill = Colors.withAlpha(WHITE, (0.035f + hover * 0.035f) * a);
            R2D.round(ctx, x, y, w, h, r, fill);
            if (active > 0.01f) {
                R2D.roundGradientH(ctx, x, y, w, h, r, Colors.withAlpha(t.accent(), 0.10f * active * a), Colors.withAlpha(t.accent(), 0.02f * active * a));
            }
            R2D.outlineGradientV(ctx, x, y, w, h, r, 0.55f, R2D.ALL,
                    Colors.withAlpha(Colors.lerp(WHITE, t.accent(), active * 0.5f), (0.12f + hover * 0.08f + active * 0.08f) * a),
                    Colors.withAlpha(WHITE, (0.035f + hover * 0.03f) * a));
        } else {
            int bg = Colors.lerp(Colors.withAlpha(t.panel(), 0.92f), Colors.withAlpha(Colors.brighten(t.panel(), 0.07f), 0.96f), hover);
            R2D.round(ctx, x, y, w, h, r, Colors.fade(bg, a));
            if (active > 0.01f) {
                R2D.roundGradientH(ctx, x, y, w, h, r, Colors.withAlpha(t.accent(), 0.07f * active * a), 0);
            }
            R2D.outline(ctx, x, y, w, h, r, 0.6f, Colors.fade(Colors.withAlpha(t.border(), 0.35f + hover * 0.25f), a));
        }
    }

    /** Recessed well for inputs (text fields, dropdown buttons, sliders' tracks). */
    public static void well(DrawContext ctx, float x, float y, float w, float h, float r, float a, float hover, float focus) {
        Theme t = Aqua.theme();
        if (on()) {
            R2D.round(ctx, x, y, w, h, r, Colors.withAlpha(0x000000, (0.20f + hover * 0.04f) * a));
            int top = Colors.lerp(Colors.withAlpha(0x000000, 0.25f), Colors.withAlpha(t.accent(), 0.9f), focus);
            int bottom = Colors.lerp(Colors.withAlpha(WHITE, 0.10f + hover * 0.06f), Colors.withAlpha(t.accent(), 0.6f), focus);
            R2D.outlineGradientV(ctx, x, y, w, h, r, 0.6f, R2D.ALL, Colors.fade(top, a), Colors.fade(bottom, a));
        } else {
            R2D.round(ctx, x, y, w, h, r, Colors.fade(Colors.lerp(Colors.withAlpha(t.background(), 0.85f), Colors.brighten(t.elevated(), 0.06f), hover * 0.5f), a));
            R2D.outline(ctx, x, y, w, h, r, 0.6f, Colors.fade(Colors.lerp(Colors.withAlpha(t.border(), 0.7f), t.accent(), focus), a));
        }
    }

    /** Raised glass chip (secondary buttons, key caps, badges). */
    public static void chip(DrawContext ctx, float x, float y, float w, float h, float r, float a, float hover) {
        Theme t = Aqua.theme();
        if (on()) {
            R2D.roundGradientV(ctx, x, y, w, h, r, Colors.withAlpha(WHITE, (0.10f + hover * 0.07f) * a), Colors.withAlpha(WHITE, (0.05f + hover * 0.05f) * a));
            R2D.outlineGradientV(ctx, x, y, w, h, r, 0.55f, R2D.ALL, Colors.withAlpha(WHITE, (0.22f + hover * 0.12f) * a),
                    Colors.withAlpha(WHITE, (0.06f + hover * 0.05f) * a));
        } else {
            R2D.round(ctx, x, y, w, h, r, Colors.fade(Colors.lerp(Colors.withAlpha(t.elevated(), 0.85f), Colors.brighten(t.elevated(), 0.12f), hover), a));
            R2D.outline(ctx, x, y, w, h, r, 0.6f, Colors.fade(Colors.withAlpha(t.border(), 0.6f + hover * 0.4f), a));
        }
    }

    /** Accent-filled primary button with a soft glow in glass mode. */
    public static void accent(DrawContext ctx, float x, float y, float w, float h, float r, float a, float hover) {
        Theme t = Aqua.theme();
        if (on()) {
            R2D.shadow(ctx, x, y + 1, w, h, r, 7 + hover * 3, Colors.withAlpha(t.accent(), (0.25f + hover * 0.15f) * a));
        }
        R2D.roundGradientV(ctx, x, y, w, h, r, Colors.fade(Colors.lerp(t.accent(), t.accent2(), 0.25f + hover * 0.2f), a),
                Colors.fade(Colors.lerp(t.accent(), Colors.darken(t.accent(), 0.2f), 0.5f - hover * 0.3f), a));
        if (on()) {
            R2D.outlineGradientV(ctx, x, y, w, h, r, 0.55f, R2D.ALL, Colors.withAlpha(WHITE, 0.45f * a), Colors.withAlpha(WHITE, 0.08f * a));
        }
    }

    /** Thin divider line that is lighter in glass mode. */
    public static void divider(DrawContext ctx, float x, float y, float w, float a) {
        if (on()) {
            R2D.roundGradientH(ctx, x, y, w / 2f, 0.5f, 0, Colors.withAlpha(WHITE, 0.02f * a), Colors.withAlpha(WHITE, 0.10f * a));
            R2D.roundGradientH(ctx, x + w / 2f, y, w / 2f, 0.5f, 0, Colors.withAlpha(WHITE, 0.10f * a), Colors.withAlpha(WHITE, 0.02f * a));
        } else {
            R2D.rect(ctx, x, y, w, 0.5f, Colors.fade(Aqua.theme().divider(), a));
        }
    }
}
