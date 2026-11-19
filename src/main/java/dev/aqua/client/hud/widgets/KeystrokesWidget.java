package dev.aqua.client.hud.widgets;

import dev.aqua.client.Aqua;
import dev.aqua.client.hud.Anchor;
import dev.aqua.client.hud.HudWidget;
import dev.aqua.client.render.Anim;
import dev.aqua.client.render.Colors;
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
    private final BoolSetting cps = add(new BoolSetting("cps", "CPS in mouse keys", true));
    private final BoolSetting space = add(new BoolSetting("space", "Space bar", true));

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
            h += G + 7;
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
                R2D.round(ctx, x, y, w, h, r, Colors.fade(base, 0.6f));
            }
            R2D.outline(ctx, x, y, w, h, r, 0.8f, Colors.fade(Colors.lerp(t.border(), t.accent(), p), alpha));
        } else if (showBackground() || p > 0.01f) {
            int fill = Colors.lerp(base, Colors.fade(style.is(Style.SOFT) ? t.accent2() : t.accent(), alpha), p);
            R2D.round(ctx, x, y, w, h, r, fill);
        }
        int tc = Colors.lerp(t.text(), t.onAccent(), style.is(Style.OUTLINE) ? 0 : p);
        float size = label.length() > 2 ? 4.6f : 5.6f;
        R2D.textCentered(ctx, label, x + w / 2f, y + h / 2f, size, AquaFont.Weight.SEMIBOLD, Colors.fade(tc, alpha));
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
            float mw = (full - G) / 2f;
            String l = cps.on() ? "LMB " + CpsTracker.left() : "LMB";
            String r = cps.on() ? "RMB " + CpsTracker.right() : "RMB";
            key(ctx, 0, y, mw, K, l, press[4], o.attackKey.isPressed(), alpha);
            key(ctx, mw + G, y, mw, K, r, press[5], o.useKey.isPressed(), alpha);
            y += K + G;
        }
        if (space.on()) {
            Theme t = Aqua.theme();
            float p = press[6].target(o.jumpKey.isPressed()).get();
            int fill = Colors.lerp(bg(alpha), Colors.fade(t.accent(), alpha), p);
            if (showBackground() || p > 0.01f) {
                R2D.round(ctx, 0, y, full, 7, 3, fill);
            }
            R2D.round(ctx, full / 2f - 8, y + 3, 16, 1.1f, 0.55f, Colors.fade(Colors.lerp(t.muted(), t.onAccent(), p), alpha));
        }
    }
}
