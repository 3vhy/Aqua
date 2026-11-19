package dev.aqua.client.module.cosmetic;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Colors;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.setting.EnumSetting;
import dev.aqua.client.setting.NumberSetting;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

/** Original animated wings for your own player (R1 "Wings"). Visible to you only. */
public final class Wings extends Module {
    public enum Style implements EnumSetting.Displayable {
        FEATHER("Feather"), CRYSTAL("Crystal");
        final String l;

        Style(String l) {
            this.l = l;
        }

        public String displayName() {
            return l;
        }
    }

    private final EnumSetting<Style> style = add(new EnumSetting<>("style", "Style", Style.FEATHER));
    private final ColorSetting inner = add(new ColorSetting("inner", "Inner colour", 0xE652E4F2, true, true));
    private final ColorSetting outer = add(new ColorSetting("outer", "Tip colour", 0xCCA8F3FF, true, false));
    private final NumberSetting size = add(new NumberSetting("size", "Size", 1.0, 0.5, 1.6, 0.05).unit("x"));
    private final NumberSetting flap = add(new NumberSetting("flap", "Flap speed", 1.0, 0.0, 3.0, 0.1).unit("x"));

    public Wings() {
        super("wings", "Wings", "Animated wings on your back (you only).", Category.COSMETICS, false, "wings", "angel", "back");
    }

    /** Draws both wings in body space (y down, +z towards the back). */
    public void draw(MatrixStack.Entry entry, VertexConsumer vc, float age, boolean moving) {
        float intensity = Aqua.ui().effectIntensity.f();
        float speed = flap.f() * (moving ? 1.8f : 1f);
        float t = age / 20f;
        float flapAngle = (float) Math.sin(t * Math.PI * speed) * 0.35f * Math.min(1f, intensity) + 0.25f;
        float s = size.f();
        for (int side = -1; side <= 1; side += 2) {
            int feathers = style.is(Style.CRYSTAL) ? 5 : 7;
            for (int i = 0; i < feathers; i++) {
                float f = i / (float) (feathers - 1);
                float spread = -0.45f + f * 1.55f; // radians from horizontal (upwards)
                float len = (0.62f + 0.62f * (float) Math.sin(f * Math.PI * 0.85f + 0.25f)) * s;
                float width = (style.is(Style.CRYSTAL) ? 0.12f : 0.09f) * s;
                float open = flapAngle * (0.6f + 0.4f * f);
                featherQuad(entry, vc, side, spread, len, width, open, f);
            }
        }
    }

    private void featherQuad(MatrixStack.Entry entry, VertexConsumer vc, int side, float spread, float len, float width, float open, float f) {
        Matrix4f m = entry.getPositionMatrix();
        // Root on the upper back.
        float rx = side * 0.06f;
        float ry = 0.18f;
        float rz = 0.16f;
        float dirX = side * (float) Math.cos(spread);
        float dirY = -(float) Math.sin(spread);
        float dirZ = 0.35f + open;
        float l = (float) Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        dirX /= l;
        dirY /= l;
        dirZ /= l;
        float tx = rx + dirX * len, ty = ry + dirY * len, tz = rz + dirZ * len;
        // Width perpendicular to the feather within the wing plane.
        float px = -dirY * width, py = dirX * width;
        int ci = Colors.lerp(inner.color(), outer.color(), f * 0.3f);
        int co = outer.color();
        vc.vertex(m, rx - px * 0.3f, ry - py * 0.3f, rz).color(ci);
        vc.vertex(m, rx + px * 0.3f, ry + py * 0.3f, rz).color(ci);
        vc.vertex(m, tx + px, ty + py, tz).color(co);
        vc.vertex(m, tx - px, ty - py, tz).color(Colors.fade(co, 0.6f));
    }
}
