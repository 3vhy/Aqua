package dev.aqua.client.module.cosmetic;

import dev.aqua.client.cosmetic.Mesh;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Colors;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.setting.EnumSetting;

/** Hats for your own player: glow cone, top hat, crown, witch hat, devil horns and cat ears. */
public final class Hats extends Module {
    public enum Style implements EnumSetting.Displayable {
        GLOW_CONE("Glow hat"), TOP_HAT("Top hat"), CROWN("Crown"), WITCH("Witch hat"), HORNS("Devil horns"), CAT_EARS("Cat ears");
        final String l;

        Style(String l) {
            this.l = l;
        }

        public String displayName() {
            return l;
        }
    }

    private final EnumSetting<Style> style = add(new EnumSetting<>("style", "Hat", Style.GLOW_CONE));
    private final ColorSetting color = add(new ColorSetting("color", "Colour", 0xFF52E4F2, false, true));
    private final BoolSetting glow = add(new BoolSetting("glow", "Glowing trim", true));

    public Hats() {
        super("hats", "Hats", "Glow hat, top hat, crown, witch hat, horns or cat ears (you only).", Category.COSMETICS, false,
                "hat", "glow hat", "china hat", "crown", "horns", "ears");
    }

    public Style style() {
        return style.get();
    }

    /** Solid parts, head space (pixels, top of head at y = -8). */
    public void drawSolid(Mesh m, Style s, float t) {
        int c = color.color();
        switch (s) {
            case GLOW_CONE -> {
                // drawn translucent in drawTranslucent; only the glowing rim here
                if (glow.on()) {
                    for (int i = 0; i < 32; i++) {
                        double a0 = Math.PI * 2 * i / 32;
                        float r = 8.2f;
                        m.glow((float) Math.cos(a0) * r - 0.4f, -9.4f, (float) Math.sin(a0) * r - 0.4f,
                                (float) Math.cos(a0) * r + 0.4f, -8.9f, (float) Math.sin(a0) * r + 0.4f, Colors.brighten(c, 0.3f));
                    }
                }
            }
            case TOP_HAT -> {
                m.cylinder(0, -9.2f, -8.4f, 0, 7f, 20, 0xFF16181C);
                m.cylinder(0, -16f, -9.2f, 0, 4.4f, 20, 0xFF1D2025);
                m.cylinder(0, -11f, -9.2f, 0, 4.55f, 20, c);
            }
            case CROWN -> {
                int gold = 0xFFF2C94C;
                for (int i = 0; i < 4; i++) {
                    float a = i * 2.25f;
                    m.box(-4.6f + a, -10f, -4.6f, -2.3f + a, -8f, -4.2f, gold);
                    m.box(-4.6f + a, -10f, 4.2f, -2.3f + a, -8f, 4.6f, gold);
                    m.box(-4.6f, -10f, -4.6f + a, -4.2f, -8f, -2.3f + a, gold);
                    m.box(4.2f, -10f, -4.6f + a, 4.6f, -8f, -2.3f + a, gold);
                }
                for (int i = 0; i < 5; i++) {
                    float x = -4f + i * 2f;
                    m.box(x - 0.6f, -12.5f + (i % 2) * 1f, -4.6f, x + 0.6f, -10f, -4.2f, gold);
                    m.box(x - 0.6f, -12.5f + (i % 2) * 1f, 4.2f, x + 0.6f, -10f, 4.6f, gold);
                }
                m.glow(-0.8f, -9.6f, -4.8f, 0.8f, -8.4f, -4.55f, c);
            }
            case WITCH -> {
                m.cylinder(0, -9f, -8.4f, 0, 8.5f, 24, 0xFF2A1F3D);
                m.cone(0, -9f, -17f, 0.5f, 4.6f, 16, 0xFF33264A);
                m.cylinder(0, -10.4f, -9f, 0, 4.7f, 16, c);
                m.cone(0.5f, -16.6f, -19.5f, 2.6f, 1.6f, 8, 0xFF33264A);
            }
            case HORNS -> {
                int horn = 0xFF7A1424, tip = 0xFF2A0A10;
                for (int side = -1; side <= 1; side += 2) {
                    for (int i = 0; i < 6; i++) {
                        float w = 1.4f - i * 0.18f;
                        float x = side * (2.6f + i * 0.55f);
                        float y = -8.5f - i * 1.1f;
                        float z = -1.5f + i * 0.35f;
                        m.box(x - w, y - 1.2f, z - w, x + w, y, z + w, i > 3 ? tip : horn);
                    }
                }
            }
            case CAT_EARS -> {
                for (int side = -1; side <= 1; side += 2) {
                    float x = side * 2.6f;
                    m.tri(x - 2.2f, -8f, -0.5f, x + 2.2f, -8f, -0.5f, x + side * 0.6f, -12.5f, 0f, c, 0.9f);
                    m.tri(x - 1.2f, -8.1f, -0.6f, x + 1.2f, -8.1f, -0.6f, x + side * 0.4f, -11f, -0.4f, 0xFFF7A8C6, 1f);
                    m.box(x - 2.2f, -8.6f, -0.3f, x + 2.2f, -8f, 0.5f, c);
                }
            }
        }
    }

    /** Translucent parts (glow cone body). */
    public void drawTranslucent(Mesh m, Style s, float t) {
        if (s == Style.GLOW_CONE) {
            int c = Colors.withAlpha(color.color(), 0.55f);
            m.cone(0, -9.1f, -14.5f, 0, 8.2f, 28, c);
        }
    }

    public boolean usesTranslucent(Style s) {
        return s == Style.GLOW_CONE;
    }
}
