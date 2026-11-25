package dev.aqua.client.gui;

import dev.aqua.client.Aqua;
import dev.aqua.client.render.Anim;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.Glass;
import dev.aqua.client.render.R2D;
import dev.aqua.client.theme.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Aqua's look for Minecraft's own widgets (option screens, world list, server list and every other
 * menu, including other mods' screens that use the standard widgets). Only the backgrounds are
 * replaced; labels, layout and behaviour stay vanilla.
 */
public final class VanillaStyle {
    private VanillaStyle() {}

    private static final Map<ClickableWidget, Anim> HOVER = new WeakHashMap<>();
    private static long drawn;

    private static long tabPanes;

    public static void noteTabPane() {
        tabPanes++;
    }

    /** Tab list backgrounds drawn as glass (used by the in-game tests). */
    public static long tabPanes() {
        return tabPanes;
    }

    public static boolean on() {
        return Aqua.ui() != null && Aqua.ui().vanillaStyle.on();
    }

    /** Widgets drawn in Aqua's style (used by the in-game tests). */
    public static long drawn() {
        return drawn;
    }

    private static float hover(ClickableWidget w, boolean hov) {
        return HOVER.computeIfAbsent(w, k -> new Anim(hov ? 1f : 0f, 110f)).target(hov).get();
    }

    /** A standalone button: tinted glass (or a solid chip) with a lit rim and an accent ring when focused. */
    public static void button(DrawContext ctx, ClickableWidget w, float alpha, boolean selected) {
        drawn++;
        boolean active = w.active;
        float h = hover(w, active && w.isHovered());
        boolean focus = active && w.isFocused() || selected && !w.isHovered() && active;
        surface(ctx, w.getX(), w.getY(), w.getWidth(), w.getHeight(), alpha * (active ? 1f : 0.55f), h, focus);
    }

    public static void surface(DrawContext ctx, float x, float y, float w, float h, float a, float hover, boolean focus) {
        Theme t = Aqua.theme();
        float r = Math.min(5f, h / 3.2f);
        if (Glass.on()) {
            int base = Colors.lerp(t.background(), t.accent(), 0.05f + hover * 0.06f);
            R2D.roundGradientV(ctx, x, y, w, h, r, Colors.withAlpha(Colors.lerp(base, 0xFFFFFFFF, 0.07f + hover * 0.08f), 0.66f * a),
                    Colors.withAlpha(Colors.darken(base, 0.12f), 0.78f * a));
            R2D.roundGradientVCorners(ctx, x, y, w, Math.max(4, h * 0.5f), r, R2D.TOP, Colors.withAlpha(0xFFFFFF, (0.07f + hover * 0.05f) * a), 0);
            R2D.outlineGradientV(ctx, x, y, w, h, r, 0.6f, R2D.ALL, Colors.withAlpha(0xFFFFFF, (0.22f + hover * 0.16f) * a),
                    Colors.withAlpha(0xFFFFFF, (0.06f + hover * 0.05f) * a));
        } else {
            R2D.round(ctx, x, y, w, h, r, Colors.withAlpha(Colors.lerp(t.elevated(), Colors.brighten(t.elevated(), 0.14f), hover), 0.95f * a));
            R2D.outline(ctx, x, y, w, h, r, 0.6f, Colors.withAlpha(t.border(), (0.6f + hover * 0.4f) * a));
        }
        if (focus) {
            R2D.outline(ctx, x, y, w, h, r, 0.8f, Colors.withAlpha(t.accent(), 0.9f * a));
        }
    }

    /** Slider track with the part left of the handle filled in the accent colour. */
    public static void sliderTrack(DrawContext ctx, ClickableWidget w, double value, float alpha) {
        drawn++;
        Theme t = Aqua.theme();
        float x = w.getX(), y = w.getY(), ww = w.getWidth(), hh = w.getHeight();
        float h = hover(w, w.active && w.isHovered());
        well(ctx, x, y, ww, hh, alpha * (w.active ? 1f : 0.55f), h, w.isFocused());
        float r = Math.min(5f, hh / 3.2f);
        float fill = (float) (value * (ww - 8)) + 4;
        if (fill > 1) {
            R2D.roundGradientH(ctx, x + 1, y + 1, Math.min(ww - 2, fill), hh - 2, r - 1, Colors.withAlpha(t.accent(), 0.10f * alpha),
                    Colors.withAlpha(t.accent(), (0.32f + h * 0.08f) * alpha));
        }
    }

    public static void sliderHandle(DrawContext ctx, ClickableWidget w, float hx, float alpha) {
        Theme t = Aqua.theme();
        float y = w.getY(), hh = w.getHeight();
        float h = hover(w, w.active && w.isHovered());
        float cx = hx + 4;
        float kw = 4 + h;
        if (w.active) {
            R2D.shadow(ctx, cx - kw / 2f, y + 3, kw, hh - 6, kw / 2f, 5, Colors.withAlpha(t.accent(), (0.45f + h * 0.25f) * alpha));
        }
        R2D.round(ctx, cx - kw / 2f, y + 3, kw, hh - 6, kw / 2f, Colors.withAlpha(0xFFFFFF, (w.active ? 0.95f : 0.5f) * alpha));
    }

    /** Recessed box for text input. */
    public static void textBox(DrawContext ctx, ClickableWidget w) {
        drawn++;
        float h = hover(w, w.isHovered());
        well(ctx, w.getX(), w.getY(), w.getWidth(), w.getHeight(), 1f, h, w.isFocused());
    }

    private static void well(DrawContext ctx, float x, float y, float w, float h, float a, float hover, boolean focus) {
        Theme t = Aqua.theme();
        float r = Math.min(5f, h / 3.2f);
        if (Glass.on()) {
            R2D.round(ctx, x, y, w, h, r, Colors.withAlpha(Colors.darken(t.background(), 0.3f), (0.62f + hover * 0.05f) * a));
            R2D.outlineGradientV(ctx, x, y, w, h, r, 0.6f, R2D.ALL, Colors.withAlpha(0x000000, 0.35f * a),
                    Colors.withAlpha(0xFFFFFF, (0.12f + hover * 0.08f) * a));
        } else {
            R2D.round(ctx, x, y, w, h, r, Colors.withAlpha(t.background(), 0.92f * a));
            R2D.outline(ctx, x, y, w, h, r, 0.6f, Colors.withAlpha(t.border(), (0.7f + hover * 0.3f) * a));
        }
        if (focus) {
            R2D.outline(ctx, x, y, w, h, r, 0.8f, Colors.withAlpha(t.accent(), 0.9f * a));
        }
    }

    /** Item and widget tooltips: a dark tinted card with a lit, accent-tinted rim. */
    public static void tooltip(DrawContext ctx, float x, float y, float w, float h) {
        drawn++;
        Theme t = Aqua.theme();
        float r = 4.5f;
        R2D.shadow(ctx, x, y + 1.5f, w, h, r, 8, 0x60000000);
        R2D.roundGradientV(ctx, x, y, w, h, r, Colors.withAlpha(Colors.lerp(t.background(), t.accent(), 0.07f), 0.95f),
                Colors.withAlpha(Colors.darken(t.background(), 0.15f), 0.97f));
        R2D.roundGradientVCorners(ctx, x, y, w, Math.min(h, 12), r, R2D.TOP, Colors.withAlpha(0xFFFFFF, 0.06f), 0);
        R2D.outlineGradientV(ctx, x, y, w, h, r, 0.6f, R2D.ALL, Colors.withAlpha(Colors.lerp(0xFFFFFFFF, t.accent(), 0.45f), 0.55f),
                Colors.withAlpha(t.accent(), 0.18f));
    }
}
