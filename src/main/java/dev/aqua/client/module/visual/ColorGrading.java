package dev.aqua.client.module.visual;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.setting.EnumSetting;
import dev.aqua.client.setting.NumberSetting;

/**
 * Color Grading (reference video): adds colour, contrast and a vignette to the world. Brightness
 * can only move a little either way, so it cannot light up dark places.
 */
public final class ColorGrading extends Module {
    public enum Look implements EnumSetting.Displayable {
        CUSTOM("Custom"), CINEMATIC("Cinematic"), WARM("Warm"), COOL("Cool"), VIVID("Vivid"), NOIR("Noir");
        final String l;

        Look(String l) {
            this.l = l;
        }

        public String displayName() {
            return l;
        }
    }

    private final EnumSetting<Look> look = add(new EnumSetting<>("look", "Look", Look.CINEMATIC));
    private final NumberSetting saturation = add(new NumberSetting("saturation", "Saturation", 1.15, 0, 2, 0.05).unit("x")
            .visibleWhen(this::custom));
    private final NumberSetting contrast = add(new NumberSetting("contrast", "Contrast", 1.1, 0.6, 1.6, 0.02).unit("x")
            .visibleWhen(this::custom));
    private final NumberSetting brightness = add(new NumberSetting("brightness", "Brightness", 0, -0.15, 0.08, 0.01)
            .visibleWhen(this::custom));
    private final ColorSetting tint = add(new ColorSetting("tint", "Tint", 0xFFFFF2E0, false, false).visibleWhen(this::custom));
    private final NumberSetting vignette = add(new NumberSetting("vignette", "Vignette", 0.35, 0, 1, 0.05));
    private final NumberSetting strength = add(new NumberSetting("strength", "Strength", 1, 0, 1, 0.05));

    public ColorGrading() {
        super("color_grading", "Color Grading", "Adds color, contrast and a vignette to the world.", Category.VISUALS, false,
                "color grading", "colour grading", "lut", "filter", "contrast", "saturation", "vignette");
    }

    private boolean custom() {
        return look.get() == Look.CUSTOM;
    }

    /** {saturation, contrast, brightness, vignette, tintR, tintG, tintB, strength} */
    public float[] params() {
        float sat, con, bri;
        int t;
        switch (look.get()) {
            case CINEMATIC -> { sat = 1.1f; con = 1.12f; bri = -0.02f; t = 0xFFFFEBD6; }
            case WARM -> { sat = 1.12f; con = 1.05f; bri = 0f; t = 0xFFFFE2C0; }
            case COOL -> { sat = 1.05f; con = 1.06f; bri = 0f; t = 0xFFD8E8FF; }
            case VIVID -> { sat = 1.45f; con = 1.15f; bri = 0.01f; t = 0xFFFFFFFF; }
            case NOIR -> { sat = 0f; con = 1.3f; bri = -0.03f; t = 0xFFFFFFFF; }
            default -> { sat = saturation.f(); con = contrast.f(); bri = brightness.f(); t = tint.color(); }
        }
        return new float[]{sat, con, Math.max(-0.15f, Math.min(0.08f, bri)), vignette.f(),
                ((t >> 16) & 255) / 255f, ((t >> 8) & 255) / 255f, (t & 255) / 255f, strength.f()};
    }
}
