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
        this(entry.getPositionMatrix(), vc, light);
    }

    private Mesh(Matrix4f m, VertexConsumer vc, float light) {
        this.vc = vc;
        this.m = m;
        this.light = light;
    }

    /** A mesh drawing into the same buffer, rotated by {@code degrees} around the Z axis through (px, py). */
    public Mesh rotatedZ(float degrees, float px, float py) {
        Matrix4f r = new Matrix4f(m).translate(px, py, 0).rotateZ((float) Math.toRadians(degrees)).translate(-px, -py, 0);
        return new Mesh(r, vc, light);
    }

    /** A mesh drawing into the same buffer, moved by (dx, dy, dz). */
    public Mesh moved(float dx, float dy, float dz) {
        return new Mesh(new Matrix4f(m).translate(dx, dy, dz), vc, light);
    }

    /**
     * A flat shape (outline in the XY plane, any simple polygon, either winding) made solid between
     * z0 and z1. Faces are shaded like a box; {@code sideColor} colours the rim.
     */
    public void extrude(float[][] outline, float z0, float z1, int faceColor, int sideColor) {
        int n = outline.length;
        if (n < 3) {
            return;
        }
        for (int[] t : triangulate(outline)) {
            float[] a = outline[t[0]], b = outline[t[1]], c = outline[t[2]];
            tri(a[0], a[1], z0, b[0], b[1], z0, c[0], c[1], z0, faceColor, 0.92f);
            tri(a[0], a[1], z1, c[0], c[1], z1, b[0], b[1], z1, faceColor, 0.8f);
        }
        for (int i = 0; i < n; i++) {
            float[] a = outline[i], b = outline[(i + 1) % n];
            float dx = b[0] - a[0], dy = b[1] - a[1];
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            // Faces turned towards -y (up in model space) are lit like a box top.
            float up = len < 1e-5f ? 0 : -dx / len;
            float shadeF = 0.62f + 0.33f * Math.abs(up);
            quad(a[0], a[1], z0, b[0], b[1], z0, b[0], b[1], z1, a[0], a[1], z1, sideColor, shadeF);
        }
    }

    /** Ear-clipping triangulation of a simple polygon; returns index triples. */
    private static java.util.List<int[]> triangulate(float[][] p) {
        int n = p.length;
        java.util.List<int[]> out = new java.util.ArrayList<>();
        java.util.List<Integer> idx = new java.util.ArrayList<>();
        float area = 0;
        for (int i = 0; i < n; i++) {
            float[] a = p[i], b = p[(i + 1) % n];
            area += a[0] * b[1] - b[0] * a[1];
        }
        for (int i = 0; i < n; i++) {
            idx.add(area > 0 ? i : n - 1 - i);
        }
        int guard = 0;
        while (idx.size() > 3 && guard++ < n * n) {
            boolean clipped = false;
            for (int i = 0; i < idx.size(); i++) {
                int i0 = idx.get((i + idx.size() - 1) % idx.size()), i1 = idx.get(i), i2 = idx.get((i + 1) % idx.size());
                float[] a = p[i0], b = p[i1], c = p[i2];
                float cross = (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0]);
                if (cross <= 1e-7f) {
                    continue;
                }
                boolean inside = false;
                for (int j : idx) {
                    if (j == i0 || j == i1 || j == i2) {
                        continue;
                    }
                    if (inTri(p[j], a, b, c)) {
                        inside = true;
                        break;
                    }
                }
                if (inside) {
                    continue;
                }
                out.add(new int[]{i0, i1, i2});
                idx.remove(i);
                clipped = true;
                break;
            }
            if (!clipped) {
                break;
            }
        }
        if (idx.size() == 3) {
            out.add(new int[]{idx.get(0), idx.get(1), idx.get(2)});
        }
        return out;
    }

    private static boolean inTri(float[] q, float[] a, float[] b, float[] c) {
        float d1 = (q[0] - b[0]) * (a[1] - b[1]) - (a[0] - b[0]) * (q[1] - b[1]);
        float d2 = (q[0] - c[0]) * (b[1] - c[1]) - (b[0] - c[0]) * (q[1] - c[1]);
        float d3 = (q[0] - a[0]) * (c[1] - a[1]) - (c[0] - a[0]) * (q[1] - a[1]);
        boolean neg = d1 < 0 || d2 < 0 || d3 < 0;
        boolean pos = d1 > 0 || d2 > 0 || d3 > 0;
        return !(neg && pos);
    }

    /** A ring (torus) lying in the XY plane around (cx, cy, cz); R is the ring radius, r the tube radius. */
    public void torusZ(float cx, float cy, float cz, float R, float r, int segU, int segV, int c) {
        for (int i = 0; i < segU; i++) {
            double u0 = Math.PI * 2 * i / segU, u1 = Math.PI * 2 * (i + 1) / segU;
            for (int j = 0; j < segV; j++) {
                double v0 = Math.PI * 2 * j / segV, v1 = Math.PI * 2 * (j + 1) / segV;
                float[] a = tp(cx, cy, cz, R, r, u0, v0), b = tp(cx, cy, cz, R, r, u1, v0);
                float[] d = tp(cx, cy, cz, R, r, u1, v1), e = tp(cx, cy, cz, R, r, u0, v1);
                float shadeF = 0.6f + 0.35f * (float) Math.abs(Math.cos((v0 + v1) / 2));
                quad(a[0], a[1], a[2], b[0], b[1], b[2], d[0], d[1], d[2], e[0], e[1], e[2], c, shadeF);
            }
        }
    }

    private static float[] tp(float cx, float cy, float cz, float R, float r, double u, double v) {
        float k = R + r * (float) Math.cos(v);
        return new float[]{cx + k * (float) Math.cos(u), cy + k * (float) Math.sin(u), cz + r * (float) Math.sin(v)};
    }

    /** Cylinder along the Z axis (for pins and rivets). */
    public void cylinderZ(float cx, float cy, float z0, float z1, float r, int segments, int c) {
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2 * i / segments, a1 = Math.PI * 2 * (i + 1) / segments;
            float x0 = cx + (float) Math.cos(a0) * r, y0 = cy + (float) Math.sin(a0) * r;
            float x1 = cx + (float) Math.cos(a1) * r, y1 = cy + (float) Math.sin(a1) * r;
            quad(x0, y0, z0, x1, y1, z0, x1, y1, z1, x0, y0, z1, c, 0.7f);
            tri(cx, cy, z0, x1, y1, z0, x0, y0, z0, c, 0.95f);
            tri(cx, cy, z1, x0, y0, z1, x1, y1, z1, c, 0.85f);
        }
    }

    /** Emissive triangle (unaffected by light). */
    public void glowTri(float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, int c) {
        v(x0, y0, z0, c); v(x1, y1, z1, c); v(x2, y2, z2, c); v(x2, y2, z2, c);
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
