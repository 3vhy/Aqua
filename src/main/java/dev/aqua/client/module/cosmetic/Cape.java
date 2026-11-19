package dev.aqua.client.module.cosmetic;

import dev.aqua.client.Aqua;
import dev.aqua.client.cosmetic.Mesh;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Colors;
import dev.aqua.client.setting.EnumSetting;
import dev.aqua.client.setting.NumberSetting;

/** A flowing cosmetic cape for your own player (hides your vanilla cape on your screen while enabled). */
public final class Cape extends Module {
    public enum Style implements EnumSetting.Displayable {
        AQUA("Aqua"), THEME("Theme"), TOPOGRAPHIC("Topographic"), GALAXY("Galaxy"), FLAME("Flame");
        final String l;

        Style(String l) {
            this.l = l;
        }

        public String displayName() {
            return l;
        }
    }

    private final EnumSetting<Style> style = add(new EnumSetting<>("style", "Style", Style.AQUA));
    private final NumberSetting wave = add(new NumberSetting("wave", "Ripple", 1.0, 0.0, 2.0, 0.1).unit("x"));

    public Cape() {
        super("cape", "Cape", "A flowing Aqua cape (you only).", Category.COSMETICS, false, "cape", "cloak");
    }

    public Style style() {
        return style.get();
    }

    private int colour(Style s, float along, float across, float t) {
        return switch (s) {
            case AQUA -> Colors.lerp(0xFFA8F3FF, 0xFF1E86C0, along);
            case THEME -> Colors.lerp(Aqua.theme().accent(), Colors.darken(Aqua.theme().background(), 0.2f), along);
            case TOPOGRAPHIC -> {
                double v = Math.sin(across * 7 + along * 5) * 1.3 + Math.cos(along * 9 - across * 4) + Math.sin((across + along) * 13) * 0.4;
                yield (v * 3.0 - Math.floor(v * 3.0)) < 0.2 ? 0xFF152028 : 0xFFD6F4F7;
            }
            case GALAXY -> Colors.lerp(0xFF2B1650, 0xFF0B0A1E, along);
            case FLAME -> Colors.lerp(0xFFFFD25A, 0xFFB5172B, Math.min(1f, along + 0.15f * (float) Math.sin(t * 8 + across * 6)));
        };
    }

    /**
     * Draws the cape in body space (pixels). {@code swing} is the backwards angle in degrees (from the
     * vanilla cape physics), {@code side} the sideways tilt.
     */
    public void draw(Mesh m, Style s, float t, float swing, float side) {
        int segs = style.is(Style.TOPOGRAPHIC) ? 22 : 10;
        float width = 10f;
        float length = 16f;
        float rippleAmp = wave.f() * 0.6f;
        float baseAngle = (float) Math.toRadians(6f + swing);
        float y = 0f, z = 2.2f;
        for (int i = 0; i < segs; i++) {
            float along = i / (float) segs;
            float seg = length / segs;
            float a = baseAngle * (0.6f + along * 0.6f) + (float) Math.sin(t * 5f - along * 6f) * 0.06f * rippleAmp;
            float ny = y + (float) Math.cos(a) * seg;
            float nz = z + (float) Math.sin(a) * seg;
            float tilt = side * 0.01f * along;
            int cols = s == Style.TOPOGRAPHIC ? 10 : 4;
            for (int k = 0; k < cols; k++) {
                float x0 = -width / 2f + k * width / cols, x1 = x0 + width / cols;
                int c = colour(s, along, (k + 0.5f) / cols, t);
                m.quad(x0 + tilt * y, y, z, x1 + tilt * y, y, z, x1 + tilt * ny, ny, nz, x0 + tilt * ny, ny, nz, c, 0.9f);
                m.quad(x0 + tilt * y, y, z + 0.6f, x0 + tilt * ny, ny, nz + 0.6f, x1 + tilt * ny, ny, nz + 0.6f, x1 + tilt * y, y, z + 0.6f,
                        Colors.darken(c, 0.15f), 0.8f);
            }
            y = ny;
            z = nz;
        }
        if (s == Style.AQUA) {
            // Drop emblem on the back of the cape
            float ey = length * 0.35f;
            float ez = 2.2f + (float) Math.sin(baseAngle) * ey + 0.65f;
            m.glow(-1.5f, ey, ez, 1.5f, ey + 3f, ez + 0.1f, 0xFFFFFFFF);
            m.glow(-0.8f, ey - 1.6f, ez, 0.8f, ey, ez + 0.1f, 0xFFFFFFFF);
        } else if (s == Style.GALAXY) {
            for (int i = 0; i < 14; i++) {
                float sx = ((i * 37) % 9) - 4.5f;
                float sa = ((i * 53) % 15) / 15f;
                float sy = length * sa * (float) Math.cos(baseAngle);
                float sz = 2.2f + (float) Math.sin(baseAngle) * length * sa + 0.65f;
                boolean tw = ((int) (t * 3 + i)) % 4 == 0;
                m.glow(sx - 0.3f, sy - 0.3f, sz, sx + 0.3f, sy + 0.3f, sz + 0.05f, tw ? 0xFFFFFFFF : 0xFFB9A6FF);
            }
        }
    }
}
