package dev.aqua.client.module.cosmetic;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Colors;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.setting.NumberSetting;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

/** Glowing halo above your head (R1 "Halo"). Visible to you only. */
public final class Halo extends Module {
    private final ColorSetting color = add(new ColorSetting("color", "Colour", 0xFFA8F3FF, false, true));
    private final NumberSetting size = add(new NumberSetting("size", "Size", 1.0, 0.6, 1.6, 0.05).unit("x"));
    private final NumberSetting height = add(new NumberSetting("height", "Height", 0.0, -0.1, 0.3, 0.01));

    public Halo() {
        super("halo", "Halo", "A softly glowing ring above your head (you only).", Category.COSMETICS, false, "halo", "angel", "ring");
    }

    /** Draws in head space (y down). */
    public void draw(MatrixStack.Entry entry, VertexConsumer vc, float age) {
        Matrix4f m = entry.getPositionMatrix();
        float bob = (float) Math.sin(age / 20f * 2.2f) * 0.025f;
        float y = -0.62f - height.f() + bob;
        float r = 0.26f * size.f();
        float w = 0.045f * size.f();
        int c = color.color();
        int seg = 36;
        for (int pass = 0; pass < 2; pass++) {
            float rr = pass == 0 ? r + w * 1.6f : r;
            float ww = pass == 0 ? w * 3.2f : w;
            int cc = pass == 0 ? Colors.fade(c, 0.22f) : c;
            for (int i = 0; i < seg; i++) {
                double a0 = Math.PI * 2 * i / seg, a1 = Math.PI * 2 * (i + 1) / seg;
                float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
                vc.vertex(m, c0 * (rr - ww), y, s0 * (rr - ww)).color(cc);
                vc.vertex(m, c0 * rr, y, s0 * rr).color(cc);
                vc.vertex(m, c1 * rr, y, s1 * rr).color(cc);
                vc.vertex(m, c1 * (rr - ww), y, s1 * (rr - ww)).color(cc);
            }
        }
    }
}
