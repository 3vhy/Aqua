package dev.aqua.client.cosmetic;

import dev.aqua.client.render.Colors;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

/**
 * Shaded flat-colour geometry for 3D cosmetics in entity model space (pixel units, y pointing down,
 * front = -z, like vanilla cuboids). Faces get fixed directional shading multiplied by world light.
 */
public final class Mesh {
    private final VertexConsumer vc;
    private final Matrix4f m;
    private final float light;

    public Mesh(MatrixStack.Entry entry, VertexConsumer vc, float light) {
        this.vc = vc;
        this.m = entry.getPositionMatrix();
        this.light = light;
    }

    private int shade(int c, float s) {
        float f = s * light;
        return Colors.argb(Colors.alpha(c), Math.round(((c >> 16) & 255) * f), Math.round(((c >> 8) & 255) * f), Math.round((c & 255) * f));
    }

    private void v(float x, float y, float z, int c) {
        vc.vertex(m, x, y, z).color(c);
    }

    /** Axis-aligned box from (x0,y0,z0) to (x1,y1,z1). */
    public void box(float x0, float y0, float z0, float x1, float y1, float z1, int c) {
        boxShaded(x0, y0, z0, x1, y1, z1, c, false);
    }

    /** Same as {@link #box} but unaffected by lighting (glowing parts). */
    public void glow(float x0, float y0, float z0, float x1, float y1, float z1, int c) {
        boxShaded(x0, y0, z0, x1, y1, z1, c, true);
    }

    private void boxShaded(float x0, float y0, float z0, float x1, float y1, float z1, int c, boolean emissive) {
        int top = emissive ? c : shade(c, 1.0f);
        int bottom = emissive ? c : shade(c, 0.55f);
        int front = emissive ? c : shade(c, 0.86f);
        int side = emissive ? c : shade(c, 0.7f);
        // top (y0)
        v(x0, y0, z0, top); v(x0, y0, z1, top); v(x1, y0, z1, top); v(x1, y0, z0, top);
        // bottom (y1)
        v(x0, y1, z0, bottom); v(x1, y1, z0, bottom); v(x1, y1, z1, bottom); v(x0, y1, z1, bottom);
        // front (z0) and back (z1)
        v(x0, y0, z0, front); v(x1, y0, z0, front); v(x1, y1, z0, front); v(x0, y1, z0, front);
        v(x0, y0, z1, front); v(x0, y1, z1, front); v(x1, y1, z1, front); v(x1, y0, z1, front);
        // sides (x0, x1)
        v(x0, y0, z0, side); v(x0, y1, z0, side); v(x0, y1, z1, side); v(x0, y0, z1, side);
        v(x1, y0, z0, side); v(x1, y0, z1, side); v(x1, y1, z1, side); v(x1, y1, z0, side);
    }

    /** Box given by centre and size. */
    public void cbox(float cx, float cy, float cz, float w, float h, float d, int c) {
        box(cx - w / 2f, cy - h / 2f, cz - d / 2f, cx + w / 2f, cy + h / 2f, cz + d / 2f, c);
    }

    /** Flat two-sided quad. */
    public void quad(float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2,
                     float x3, float y3, float z3, int c, float shadeFactor) {
        int s = shade(c, shadeFactor);
        v(x0, y0, z0, s); v(x1, y1, z1, s); v(x2, y2, z2, s); v(x3, y3, z3, s);
    }

    /** Flat triangle (degenerate quad). */
    public void tri(float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, int c, float shadeFactor) {
        quad(x0, y0, z0, x1, y1, z1, x2, y2, z2, x2, y2, z2, c, shadeFactor);
    }

    /** Vertical cylinder (y axis) approximated by n boxes-free side quads plus caps. */
    public void cylinder(float cx, float y0, float y1, float cz, float r, int segments, int c) {
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2 * i / segments, a1 = Math.PI * 2 * (i + 1) / segments;
            float x0 = cx + (float) Math.cos(a0) * r, z0 = cz + (float) Math.sin(a0) * r;
            float x1 = cx + (float) Math.cos(a1) * r, z1 = cz + (float) Math.sin(a1) * r;
            float s = 0.62f + 0.3f * (float) Math.abs(Math.sin((a0 + a1) / 2));
            quad(x0, y0, z0, x0, y1, z0, x1, y1, z1, x1, y0, z1, c, s);
            tri(cx, y0, cz, x0, y0, z0, x1, y0, z1, c, 1f);
            tri(cx, y1, cz, x1, y1, z1, x0, y1, z0, c, 0.55f);
        }
    }

    /** Cone with its base on y0 (radius r) and tip at yTip. */
    public void cone(float cx, float y0, float yTip, float cz, float r, int segments, int c) {
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2 * i / segments, a1 = Math.PI * 2 * (i + 1) / segments;
            float x0 = cx + (float) Math.cos(a0) * r, z0 = cz + (float) Math.sin(a0) * r;
            float x1 = cx + (float) Math.cos(a1) * r, z1 = cz + (float) Math.sin(a1) * r;
            float s = 0.65f + 0.3f * (float) Math.abs(Math.sin((a0 + a1) / 2));
            tri(x0, y0, z0, cx, yTip, cz, x1, y0, z1, c, s);
            tri(cx, y0, cz, x1, y0, z1, x0, y0, z0, c, 0.55f);
        }
    }

    /** Flat ring/disc in the XZ plane at height y. */
    public void ring(float cx, float y, float cz, float rIn, float rOut, int segments, int c) {
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2 * i / segments, a1 = Math.PI * 2 * (i + 1) / segments;
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            quad(cx + c0 * rIn, y, cz + s0 * rIn, cx + c0 * rOut, y, cz + s0 * rOut, cx + c1 * rOut, y, cz + s1 * rOut,
                    cx + c1 * rIn, y, cz + s1 * rIn, c, 1f);
        }
    }

    /** Light factor from a packed lightmap coordinate and the world's sky darkness. */
    public static float lightFactor(int packedLight, float skyDarkness) {
        int block = (packedLight & 0xFFFF) >> 4;
        int sky = (packedLight >> 20) & 0xF;
        float skyEff = sky * (1f - skyDarkness);
        float l = Math.max(block, skyEff) / 15f;
        return 0.28f + 0.72f * l;
    }
}
