package dev.aqua.client.hud.widgets;

import dev.aqua.client.Aqua;
import dev.aqua.client.hud.Anchor;
import dev.aqua.client.hud.HudWidget;
import dev.aqua.client.render.Anim;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.Glass;
import dev.aqua.client.render.Icon;
import dev.aqua.client.render.R2D;
import dev.aqua.client.render.font.AquaFont;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.EnumSetting;
import dev.aqua.client.theme.Theme;
import dev.aqua.client.util.CpsTracker;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;

/** W/A/S/D, LMB/RMB (optional CPS) and Space keys (Fern R1-R2, R3 0:02 style variants). */
public final class KeystrokesWidget extends HudWidget {
    public enum Style implements EnumSetting.Displayable {
        FERN("Fern"), SOFT("Soft"), OUTLINE("Outline");
        final String l;

        Style(String l) {
            this.l = l;
        }

        public String displayName() {
            return l;
        }
    }

    private final EnumSetting<Style> style = add(new EnumSetting<>("style", "Style", Style.FERN));
    private final BoolSetting mouse = add(new BoolSetting("mouse", "Mouse buttons", true));
    private final BoolSetting cps = add(new BoolSetting("cps", "CPS in mouse keys", false));
    private final BoolSetting mousePad = add(new BoolSetting("mouse_pad", "Mouse movement between LMB and RMB", true)
            .describe("A dot that follows your real mouse movement (Fern style)"));
    private final BoolSetting space = add(new BoolSetting("space", "Space bar", true));
    private final BoolSetting spaceLabel = add(new BoolSetting("space_label", "SPACE label", true).visibleWhen(space::on));

    private static final float K = 14f;
    private static final float G = 2f;
    private final Anim[] press = new Anim[7];

    public KeystrokesWidget() {
        super("keystrokes", "Keystrokes", "Shows your movement keys and mouse buttons.", Icon.KEYBOARD, true, Anchor.MIDDLE_LEFT, 5, 0,
                "keys", "wasd", "cps");
        for (int i = 0; i < press.length; i++) {
            press[i] = new Anim(0f, 120f);
        }
    }

    @Override
    public float width(boolean preview) {
        return K * 3 + G * 2;
    }

    @Override
    public float height(boolean preview) {
        float h = K * 2 + G;
        if (mouse.on()) {
            h += G + K;
        }
        if (space.on()) {
            h += G + (spaceLabel.on() ? 10 : 7);
        }
        return h;
    }

    private void key(DrawContext ctx, float x, float y, float w, float h, String label, Anim anim, boolean down, float alpha) {
        Theme t = Aqua.theme();
        float p = anim.target(down).get();
        int base = bg(alpha);
        float r = style.is(Style.SOFT) ? Math.min(w, h) * 0.4f : 3.2f;
        if (style.is(Style.OUTLINE)) {
            if (showBackground()) {
                Glass.hud(ctx, x, y, w, h, r, Colors.fade(base, 0.6f));
            }
            R2D.outline(ctx, x, y, w, h, r, 0.8f, Colors.fade(Colors.lerp(t.border(), t.accent(), p), alpha));
        } else {
            if (showBackground()) {
                Glass.hud(ctx, x, y, w, h, r, base);
            }
            if (p > 0.01f) {
                R2D.round(ctx, x, y, w, h, r, Colors.fade(style.is(Style.SOFT) ? t.accent2() : t.accent(), alpha * p));
            }
        }
        int tc = Colors.lerp(t.text(), t.onAccent(), style.is(Style.OUTLINE) ? 0 : p);
        float size = label.length() > 2 ? 4.6f : 5.6f;
        R2D.textCentered(ctx, label, x + w / 2f, y + h / 2f, size, AquaFont.Weight.SEMIBOLD, Colors.fade(tc, alpha));
    }

    /** Small square whose dot follows your actual mouse movement and settles back to the centre. */
    private void drawMousePad(DrawContext ctx, float x, float y, float w, float h, float alpha) {
        Theme t = Aqua.theme();
        if (showBackground()) {
            Glass.hud(ctx, x, y, w, h, 3.2f, bg(alpha));
        }
        float cx = x + w / 2f, cy = y + h / 2f;
        float range = Math.min(w, h) / 2f - 2.2f;
        float dx = dev.aqua.client.util.MouseTracker.x() * range;
        float dy = dev.aqua.client.util.MouseTracker.y() * range;
        float moving = Math.min(1f, (float) Math.hypot(dx, dy) / Math.max(0.1f, range));
        R2D.circle(ctx, cx, cy, 0.5f, Colors.fade(t.muted(), alpha * 0.5f));
        R2D.circle(ctx, cx + dx, cy + dy, 1.4f, Colors.fade(Colors.lerp(t.text(), t.accent(), moving), alpha));
    }

    private static String keyName(KeyBinding kb, String fallback) {
        String s = kb.getBoundKeyLocalizedText().getString();
        if (s.isEmpty()) {
            return fallback;
        }
        return s.length() > 3 ? fallback : s.toUpperCase();
    }

    @Override
    public void draw(DrawContext ctx, boolean preview, float alpha) {
        var o = mc.options;
        key(ctx, K + G, 0, K, K, keyName(o.forwardKey, "W"), press[0], o.forwardKey.isPressed(), alpha);
        key(ctx, 0, K + G, K, K, keyName(o.leftKey, "A"), press[1], o.leftKey.isPressed(), alpha);
        key(ctx, K + G, K + G, K, K, keyName(o.backKey, "S"), press[2], o.backKey.isPressed(), alpha);
        key(ctx, (K + G) * 2, K + G, K, K, keyName(o.rightKey, "D"), press[3], o.rightKey.isPressed(), alpha);
        float y = (K + G) * 2;
        float full = K * 3 + G * 2;
        if (mouse.on()) {
            String l = cps.on() ? "LMB " + CpsTracker.left() : "LMB";
            String r = cps.on() ? "RMB " + CpsTracker.right() : "RMB";
            if (mousePad.on()) {
                float pad = 10f;
                float mw = (full - pad - G * 2) / 2f;
                key(ctx, 0, y, mw, K, l, press[4], o.attackKey.isPressed(), alpha);
                drawMousePad(ctx, mw + G, y, pad, K, alpha);
                key(ctx, mw + G * 2 + pad, y, mw, K, r, press[5], o.useKey.isPressed(), alpha);
            } else {
                float mw = (full - G) / 2f;
                key(ctx, 0, y, mw, K, l, press[4], o.attackKey.isPressed(), alpha);
                key(ctx, mw + G, y, mw, K, r, press[5], o.useKey.isPressed(), alpha);
            }
            y += K + G;
        }
        if (space.on()) {
            Theme t = Aqua.theme();
            float sh = spaceLabel.on() ? 10f : 7f;
            float p = press[6].target(o.jumpKey.isPressed()).get();
            if (showBackground()) {
                Glass.hud(ctx, 0, y, full, sh, 3, bg(alpha));
            }
            if (p > 0.01f) {
                R2D.round(ctx, 0, y, full, sh, 3, Colors.fade(t.accent(), alpha * p));
            }
            int fg = Colors.fade(Colors.lerp(t.muted(), t.onAccent(), p), alpha);
            if (spaceLabel.on()) {
                String label = "S P A C E";
                R2D.textCentered(ctx, label, full / 2f, y + sh / 2f, 4.2f, AquaFont.Weight.SEMIBOLD, fg);
            } else {
                R2D.round(ctx, full / 2f - 8, y + 3, 16, 1.1f, 0.55f, fg);
            }
        }
    }
}
