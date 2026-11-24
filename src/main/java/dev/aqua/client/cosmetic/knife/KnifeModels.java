package dev.aqua.client.cosmetic.knife;

import dev.aqua.client.cosmetic.Mesh;
import dev.aqua.client.render.Colors;

/**
 * Original knife and sword models. Local frame (pixels): +Y runs from the grip towards the tip, X is
 * blade width, Z thickness; the grip top (where the blade starts) is near the origin.
 */
public final class KnifeModels {
    private KnifeModels() {}

    /** Colours of a knife finish. */
    public interface Finish {
        /** Blade colour along its length (0 at the guard, 1 at the tip). */
        int blade(float along);

        /** The sharpened edge. */
        int edge(float along);

        /** Handle scales / grip. */
        int handle();

        /** Inlays, wraps and trims. */
        int accent();

        /** Pins, guards, bolsters, rings. */
        int metal();
    }

    // ------------------------------------------------------------------ helpers

    /**
     * A blade between a spine polyline (thick back) and an edge polyline (sharpened): two flat sides
     * that thin towards the edge, the spine face and a bright edge bevel.
     */
    private static void strip(Mesh m, float[][] spine, float[][] edge, float thick, Finish f) {
        int n = spine.length;
        for (int i = 0; i < n - 1; i++) {
            float a0 = i / (float) (n - 1), a1 = (i + 1) / (float) (n - 1);
            int c = Colors.lerp(f.blade(a0), f.blade(a1), 0.5f);
            int e = f.edge((a0 + a1) / 2f);
            float[] s0 = spine[i], s1 = spine[i + 1], e0 = edge[i], e1 = edge[i + 1];
            float et = thick * 0.2f;
            m.quad(s0[0], s0[1], -thick, s1[0], s1[1], -thick, e1[0], e1[1], -et, e0[0], e0[1], -et, c, 0.95f);
            m.quad(s0[0], s0[1], thick, e0[0], e0[1], et, e1[0], e1[1], et, s1[0], s1[1], thick, c, 0.8f);
            m.quad(s0[0], s0[1], -thick, s0[0], s0[1], thick, s1[0], s1[1], thick, s1[0], s1[1], -thick, Colors.darken(c, 0.18f), 0.7f);
            m.quad(e0[0], e0[1], -et, e1[0], e1[1], -et, e1[0], e1[1], et, e0[0], e0[1], et, e, 1f);
        }
    }

    /** Double-edged blade: a centre ridge with an edge on each side. */
    private static void doubleEdge(Mesh m, float y0, float y1, float halfWidth, float pointFrom, float thick, Finish f) {
        int n = 16;
        float[][] ridge = new float[n + 1][], right = new float[n + 1][], left = new float[n + 1][];
        for (int i = 0; i <= n; i++) {
            float t = i / (float) n;
            float y = y0 + (y1 - y0) * t;
            float w = t < pointFrom ? halfWidth * (1f - 0.12f * t) : halfWidth * (1f - 0.12f * pointFrom) * (1f - (t - pointFrom) / (1f - pointFrom));
            ridge[i] = new float[]{0f, y};
            right[i] = new float[]{Math.max(0.02f, w), y};
            left[i] = new float[]{-Math.max(0.02f, w), y};
        }
        strip(m, ridge, right, thick, f);
        strip(m, ridge, left, thick, f);
    }

    private static float[][] octagon(float cx, float cy, float rx, float ry) {
        float[][] p = new float[8][];
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8 + Math.PI / 8;
            p[i] = new float[]{cx + (float) Math.cos(a) * rx, cy + (float) Math.sin(a) * ry};
        }
        return p;
    }

    private static int pulse(int c, float time, float speed) {
        float k = 0.75f + 0.25f * (float) Math.sin(time * speed);
        return Colors.argb(Colors.alpha(c), Math.round(((c >> 16) & 255) * k), Math.round(((c >> 8) & 255) * k), Math.round((c & 255) * k));
    }

    // ------------------------------------------------------------------ knives

    /** Karambit: claw blade curving forward, ridged finger grip and a finger ring at the butt. */
    public static void karambit(Mesh m, Finish f) {
        int metal = f.metal();
        m.torusZ(0.15f, -7.7f, 0f, 1.95f, 0.5f, 22, 8, metal);
        m.extrude(new float[][]{{-0.5f, -6.3f}, {0.8f, -6.3f}, {0.6f, -5.6f}, {-0.6f, -5.6f}}, -0.45f, 0.45f, metal, metal);
        float[][] handle = {
                {-1.05f, -5.9f}, {-1.15f, -3.6f}, {-1.05f, -1.2f}, {-0.95f, 1.3f}, {0.9f, 1.5f},
                {1.1f, 0.9f}, {1.6f, 0.35f}, {1.15f, -0.3f},
                {1.25f, -1.2f}, {1.75f, -1.8f}, {1.25f, -2.5f},
                {1.3f, -3.3f}, {1.8f, -3.9f}, {1.25f, -4.6f},
                {1.05f, -5.4f}, {0.7f, -5.95f}
        };
        m.extrude(handle, -0.78f, 0.78f, f.handle(), Colors.darken(f.handle(), 0.15f));
        // metal liners peeking out between the scales
        m.extrude(handle, -0.3f, 0.3f, metal, metal);
        for (float y : new float[]{-3.9f, -1.0f, 0.6f}) {
            m.cylinderZ(0.05f, y, -0.82f, 0.82f, 0.26f, 10, metal);
        }
        m.extrude(new float[][]{{-0.95f, 1.3f}, {0.9f, 1.5f}, {1.15f, 2.4f}, {-0.85f, 2.3f}}, -0.5f, 0.5f, metal, metal);
        // claw blade: spine on the outer arc, edge on the inner arc, tip pointing forward and down
        int n = 22;
        float cx = 5.5f, cy = 2.3f;
        float[][] spine = new float[n + 1][], edge = new float[n + 1][];
        for (int i = 0; i <= n; i++) {
            float t = i / (float) n;
            double a = Math.toRadians(180 - t * 145);
            float ro = 5.9f;
            float width = 2.5f * (1f - t * t * 0.97f) * (1f - 0.15f * t);
            float ri = ro - width;
            spine[i] = new float[]{cx + (float) Math.cos(a) * ro - 0.15f, cy + (float) Math.sin(a) * ro};
            edge[i] = new float[]{cx + (float) Math.cos(a) * ri, cy + (float) Math.sin(a) * ri};
        }
        strip(m, spine, edge, 0.34f, f);
        // jimping on the spine for the thumb
        for (int k = 0; k < 5; k++) {
            double a = Math.toRadians(172 - k * 5.5);
            float x = cx + (float) Math.cos(a) * 6.05f - 0.15f, y = cy + (float) Math.sin(a) * 6.05f;
            m.cbox(x, y, 0f, 0.28f, 0.28f, 0.6f, Colors.darken(f.blade(0.1f), 0.25f));
        }
    }

    /** Butterfly knife: skeletonised twin handles that swing around the pivot ({@code open} 0..1). */
    public static void butterfly(Mesh m, Finish f, float open) {
        balisongHandle(m, f, -0.45f, true);
        balisongHandle(m.rotatedZ(open * 180f, 0f, 0.35f), f, 0.45f, false);
        int n = 18;
        float[][] spine = new float[n + 1][], edge = new float[n + 1][];
        for (int i = 0; i <= n; i++) {
            float t = i / (float) n;
            float y = 0.9f + t * 10.6f;
            float sx = t < 0.62f ? -0.6f : -0.6f + (float) Math.pow((t - 0.62f) / 0.38f, 1.6) * 0.95f;
            float ex = t < 0.78f ? 1.05f : 1.05f - (float) Math.pow((t - 0.78f) / 0.22f, 1.3) * 0.7f;
            spine[i] = new float[]{sx, y};
            edge[i] = new float[]{Math.max(ex, sx + 0.04f), y + (t > 0.97f ? 0.15f : 0f)};
        }
        strip(m, spine, edge, 0.3f, f);
        // swedge along the clip
        m.extrude(new float[][]{{-0.55f, 7.6f}, {-0.2f, 7.6f}, {0.25f, 11.2f}, {0.05f, 11.3f}}, -0.31f, 0.31f,
                Colors.brighten(f.blade(0.8f), 0.18f), f.blade(0.8f));
        // kicker and tang
        m.extrude(new float[][]{{-0.65f, 0.1f}, {1.15f, 0.1f}, {1.25f, 0.9f}, {0.7f, 1.4f}, {-0.65f, 1.0f}}, -0.32f, 0.32f,
                f.blade(0f), Colors.darken(f.blade(0f), 0.15f));
        m.cylinderZ(0f, 0.35f, -0.95f, 0.95f, 0.3f, 10, f.metal());
        m.cylinderZ(0.6f, 0.95f, -0.95f, 0.95f, 0.2f, 8, f.metal());
    }

    private static void balisongHandle(Mesh m, Finish f, float z, boolean latch) {
        float w = 0.72f, y0 = -10.6f, y1 = 0.5f, t = 0.3f;
        int base = f.handle();
        m.box(-w, y0, z - t, -w + 0.34f, y1, z + t, base);
        m.box(w - 0.34f, y0, z - t, w, y1, z + t, base);
        for (float y : new float[]{y0, -7.4f, -4.5f, -1.6f}) {
            m.box(-w, y, z - t, w, y + 0.7f, z + t, base);
        }
        m.box(-w, y1 - 0.8f, z - t, w, y1, z + t, base);
        // coloured inlays along both rails on the outside face
        float face = z + (z < 0 ? -t : t);
        float d = z < 0 ? -0.05f : 0.05f;
        m.box(-w + 0.06f, y0 + 0.9f, Math.min(face, face + d), -w + 0.28f, y1 - 1.0f, Math.max(face, face + d), f.accent());
        m.box(w - 0.28f, y0 + 0.9f, Math.min(face, face + d), w - 0.06f, y1 - 1.0f, Math.max(face, face + d), f.accent());
        m.cylinderZ(0f, y0 + 0.35f, z - t - 0.06f, z + t + 0.06f, 0.2f, 8, f.metal());
        if (latch) {
            m.extrude(new float[][]{{-0.2f, y0 - 0.1f}, {1.05f, y0 - 0.1f}, {1.05f, y0 + 0.35f}, {0.75f, y0 + 0.35f}, {0.75f, y0 + 0.05f},
                    {-0.2f, y0 + 0.2f}}, z - 0.12f, z + 0.12f, f.metal(), f.metal());
        }
    }

    /** M9-style bayonet: ribbed round grip, angled crossguard, clip-point blade with a saw-back and cutter hole. */
    public static void bayonet(Mesh m, Finish f) {
        int metal = f.metal();
        int grip = f.handle();
        m.cylinder(0f, -7.7f, -6.7f, 0f, 1.0f, 12, metal);
        m.box(-1.05f, -7.5f, -0.25f, 1.05f, -7.1f, 0.25f, 0xFF15171B);
        m.cylinder(0f, -6.7f, -0.2f, 0f, 0.86f, 12, grip);
        for (float y = -6.3f; y < -0.6f; y += 0.8f) {
            m.cylinder(0f, y, y + 0.32f, 0f, 0.98f, 12, Colors.darken(grip, 0.25f));
        }
        m.extrude(new float[][]{{-2.9f, 0.2f}, {-2.6f, -0.2f}, {2.1f, -0.2f}, {2.7f, 0.35f}, {2.25f, 0.75f}, {-2.5f, 0.75f}}, -0.4f, 0.4f,
                metal, Colors.darken(metal, 0.1f));
        m.torusZ(-2.55f, 0.28f, 0f, 0.62f, 0.17f, 14, 6, metal);
        m.extrude(new float[][]{{-0.75f, 0.75f}, {1.35f, 0.75f}, {1.35f, 1.7f}, {-0.75f, 1.7f}}, -0.36f, 0.36f, f.blade(0f),
                Colors.darken(f.blade(0f), 0.15f));
        int n = 24;
        float[][] spine = new float[n + 1][], edge = new float[n + 1][];
        for (int i = 0; i <= n; i++) {
            float t = i / (float) n;
            float y = 1.7f + t * 17f;
            float sx = t < 0.62f ? -0.75f : -0.75f + (float) Math.pow((t - 0.62f) / 0.38f, 1.4) * 1.15f;
            float ex = t < 0.72f ? 1.35f : 1.35f - (float) Math.pow((t - 0.72f) / 0.28f, 1.25) * 0.95f;
            spine[i] = new float[]{sx, y};
            edge[i] = new float[]{Math.max(ex, sx + 0.04f), y};
        }
        strip(m, spine, edge, 0.36f, f);
        // saw-back teeth on the spine
        int teeth = Colors.darken(f.blade(0.2f), 0.12f);
        for (int k = 0; k < 10; k++) {
            float y = 3.0f + k * 0.78f;
            m.extrude(new float[][]{{-0.74f, y}, {-1.3f, y + 0.42f}, {-0.74f, y + 0.78f}}, -0.22f, 0.22f, teeth, teeth);
        }
        // fuller and wire-cutter hole
        m.extrude(new float[][]{{0.05f, 5.4f}, {0.4f, 5.4f}, {0.4f, 12.5f}, {0.05f, 12.2f}}, -0.37f, 0.37f,
                Colors.darken(f.blade(0.5f), 0.22f), Colors.darken(f.blade(0.5f), 0.22f));
        m.extrude(octagon(0.45f, 3.4f, 0.55f, 0.8f), -0.38f, 0.38f, 0xFF0C0E12, 0xFF0C0E12);
    }

    /** Stiletto: long slender double-edged blade, small crossguard, slim grip with bolsters. */
    public static void stiletto(Mesh m, Finish f) {
        int metal = f.metal();
        m.cone(0f, -6.0f, -7.2f, 0f, 0.45f, 8, metal);
        m.box(-0.62f, -6.0f, -0.55f, 0.62f, -5.3f, 0.55f, metal);
        m.extrude(new float[][]{{-0.55f, -5.3f}, {0.55f, -5.3f}, {0.62f, -3.0f}, {0.55f, -0.6f}, {-0.55f, -0.6f}, {-0.62f, -3.0f}},
                -0.55f, 0.55f, f.handle(), Colors.darken(f.handle(), 0.15f));
        for (float y : new float[]{-4.4f, -1.6f}) {
            m.cylinderZ(0f, y, -0.6f, 0.6f, 0.17f, 8, metal);
        }
        m.box(-0.62f, -0.6f, -0.55f, 0.62f, 0f, 0.55f, metal);
        m.box(-1.75f, 0f, -0.32f, 1.75f, 0.55f, 0.32f, metal);
        m.cbox(-1.85f, 0.27f, 0f, 0.45f, 0.45f, 0.45f, metal);
        m.cbox(1.85f, 0.27f, 0f, 0.45f, 0.45f, 0.45f, metal);
        doubleEdge(m, 0.55f, 16.5f, 0.68f, 0.72f, 0.3f, f);
    }

    // ------------------------------------------------------------------ fantasy swords

    /** Frost sword: icy blade with jagged edges, a glowing core and crystal guard. */
    public static void frostSword(Mesh m, float time) {
        int ice = 0xFFBFE9FF, deep = 0xFF4FA8E0, core = pulse(0xFF7FF4FF, time, 3f);
        m.cylinder(0f, -6.2f, 0f, 0f, 0.72f, 10, 0xFF23405F);
        for (float y = -5.8f; y < -0.3f; y += 0.9f) {
            m.cylinder(0f, y, y + 0.4f, 0f, 0.8f, 10, 0xFF8FD7FF);
        }
        m.cone(0f, -6.4f, -8.4f, 0f, 1.0f, 6, ice);
        m.cone(0f, -6.4f, -5.6f, 0f, 1.0f, 6, deep);
        m.extrude(new float[][]{{-3.6f, 1.4f}, {-2.6f, -0.2f}, {-1.0f, 0.3f}, {0f, -0.5f}, {1.0f, 0.3f}, {2.6f, -0.2f}, {3.6f, 1.4f},
                {1.8f, 0.9f}, {0f, 1.6f}, {-1.8f, 0.9f}}, -0.55f, 0.55f, ice, deep);
        int n = 14;
        float[][] mid = new float[n + 1][], r = new float[n + 1][], l = new float[n + 1][];
        for (int i = 0; i <= n; i++) {
            float t = i / (float) n;
            float y = 1.3f + t * 21f;
            float w = (1.55f + ((i % 2 == 0) ? 0.45f : -0.1f)) * (t > 0.82f ? 1f - (t - 0.82f) / 0.18f : 1f);
            mid[i] = new float[]{0f, y};
            r[i] = new float[]{Math.max(0.03f, w), y};
            l[i] = new float[]{-Math.max(0.03f, w * 0.92f), y};
        }
        Finish iceF = solid(ice, 0xFFFFFFFF);
        strip(m, mid, r, 0.34f, iceF);
        strip(m, mid, l, 0.34f, iceF);
        m.glow(-0.18f, 1.6f, -0.37f, 0.18f, 19.5f, 0.37f, core);
    }

    /** Sun greatblade: broad blade, golden sunburst guard and a glowing seam. */
    public static void sunSword(Mesh m, float time) {
        int gold = 0xFFE8B43A, steel = 0xFFF3EBD9, glow = pulse(0xFFFFA43C, time, 2.2f);
        m.cylinderZ(0f, -6.9f, -0.5f, 0.5f, 1.05f, 16, gold);
        m.glow(-0.35f, -7.25f, -0.55f, 0.35f, -6.55f, 0.55f, glow);
        m.cylinder(0f, -6.0f, 0f, 0f, 0.8f, 10, 0xFF6A1F14);
        for (float y = -5.6f; y < -0.4f; y += 1.1f) {
            m.cylinder(0f, y, y + 0.35f, 0f, 0.88f, 10, gold);
        }
        float[][] sun = new float[16][];
        for (int i = 0; i < 16; i++) {
            double a = Math.PI * 2 * i / 16;
            float rr = (i % 2 == 0) ? 3.4f : 1.8f;
            sun[i] = new float[]{(float) Math.cos(a) * rr * 1.15f, 0.6f + (float) Math.sin(a) * rr * 0.55f};
        }
        m.extrude(sun, -0.6f, 0.6f, gold, Colors.darken(gold, 0.2f));
        Finish f = new Finish() {
            public int blade(float a) {
                return Colors.lerp(steel, 0xFFFFE2A0, a * 0.7f);
            }

            public int edge(float a) {
                return 0xFFFFFFFF;
            }

            public int handle() {
                return gold;
            }

            public int accent() {
                return gold;
            }

            public int metal() {
                return gold;
            }
        };
        int n = 10;
        float[][] mid = new float[n + 1][], r = new float[n + 1][], l = new float[n + 1][];
        for (int i = 0; i <= n; i++) {
            float t = i / (float) n;
            float y = 1.8f + t * 19.5f;
            float w = t < 0.85f ? 2.4f - 0.4f * t : (2.4f - 0.34f) * (1f - (t - 0.85f) / 0.15f);
            mid[i] = new float[]{0f, y};
            r[i] = new float[]{Math.max(0.03f, w), y - (t > 0.85f ? (t - 0.85f) * 4f : 0f)};
            l[i] = new float[]{-Math.max(0.03f, w), y};
        }
        strip(m, mid, r, 0.42f, f);
        strip(m, mid, l, 0.42f, f);
        m.glow(-0.2f, 2.0f, -0.45f, 0.2f, 19f, 0.45f, glow);
    }

    /** Void sword: dark hooked blade with glowing violet cracks and horned guard. */
    public static void voidSword(Mesh m, float time) {
        int dark = 0xFF1B1328, rim = 0xFF4A2E73, rune = pulse(0xFFB45CFF, time, 4f);
        m.cylinder(0f, -6.2f, 0f, 0f, 0.72f, 10, 0xFF2A1840);
        for (float y = -5.8f; y < -0.4f; y += 0.95f) {
            m.cylinder(0f, y, y + 0.3f, 0f, 0.8f, 10, rim);
        }
        m.cylinderZ(0f, -7.0f, -0.7f, 0.7f, 0.95f, 14, rim);
        m.glow(-0.45f, -7.45f, -0.75f, 0.45f, -6.55f, 0.75f, rune);
        m.extrude(new float[][]{{-3.8f, 2.6f}, {-3.2f, 0.4f}, {-1.4f, -0.3f}, {1.4f, -0.3f}, {3.2f, 0.4f}, {3.8f, 2.6f}, {2.7f, 1.0f},
                {1.2f, 0.7f}, {-1.2f, 0.7f}, {-2.7f, 1.0f}}, -0.5f, 0.5f, rim, dark);
        Finish f = solid(dark, 0xFF8C5BD6);
        int n = 20;
        float[][] spine = new float[n + 1][], edge = new float[n + 1][];
        for (int i = 0; i <= n; i++) {
            float t = i / (float) n;
            float y = 1.0f + t * 20.5f;
            float hook = t > 0.8f ? (float) Math.pow((t - 0.8f) / 0.2f, 2) * -2.2f : 0f;
            float w = 2.0f * (t > 0.8f ? 1f - (t - 0.8f) / 0.2f * 0.92f : 1f - 0.15f * t);
            spine[i] = new float[]{-0.9f + hook * 0.3f, y};
            edge[i] = new float[]{-0.9f + w + hook, y + (t > 0.8f ? (t - 0.8f) * 3f : 0f)};
        }
        strip(m, spine, edge, 0.4f, f);
        float x = 0f;
        for (int k = 0; k < 7; k++) {
            float y = 2.5f + k * 2.3f;
            float nx = (k % 2 == 0) ? 0.45f : -0.2f;
            m.glowTri(x, y, -0.42f, nx, y + 2.3f, -0.42f, x + 0.25f, y, -0.42f, rune);
            m.glowTri(x, y, 0.42f, x + 0.25f, y, 0.42f, nx, y + 2.3f, 0.42f, rune);
            x = nx;
        }
    }

    /** Thorn sword: crimson blade with thorns on the spine, vine-wrapped grip and a rose pommel. */
    public static void thornSword(Mesh m, float time) {
        int crimson = 0xFFB0173B, petal = 0xFFE8456B, vine = 0xFF2E7A3A, leaf = 0xFF46A853;
        m.cylinder(0f, -6.0f, 0f, 0f, 0.7f, 10, 0xFF3B2416);
        for (float y = -5.7f; y < -0.4f; y += 0.75f) {
            m.box(-0.82f, y, -0.82f, 0.82f, y + 0.3f, 0.82f, vine);
        }
        for (int k = 0; k < 6; k++) {
            float a = k * 60f;
            Mesh r = m.rotatedZ(a, 0f, -7.0f);
            r.extrude(new float[][]{{0f, -7.0f}, {0.8f, -6.3f}, {0.5f, -5.6f}, {0f, -5.9f}, {-0.5f, -5.6f}, {-0.8f, -6.3f}}, -0.35f, 0.35f,
                    k % 2 == 0 ? petal : crimson, crimson);
        }
        m.glow(-0.3f, -7.3f, -0.4f, 0.3f, -6.7f, 0.4f, pulse(0xFFFFB3C8, time, 2f));
        m.extrude(new float[][]{{-3.2f, 0.9f}, {-2.0f, -0.2f}, {-0.6f, 0.2f}, {0f, -0.4f}, {0.6f, 0.2f}, {2.0f, -0.2f}, {3.2f, 0.9f},
                {1.6f, 0.7f}, {0f, 1.3f}, {-1.6f, 0.7f}}, -0.45f, 0.45f, leaf, vine);
        Finish f = new Finish() {
            public int blade(float a) {
                return Colors.lerp(0xFF6E0A22, petal, a);
            }

            public int edge(float a) {
                return 0xFFFFD6E0;
            }

            public int handle() {
                return vine;
            }

            public int accent() {
                return leaf;
            }

            public int metal() {
                return crimson;
            }
        };
        int n = 18;
        float[][] spine = new float[n + 1][], edge = new float[n + 1][];
        for (int i = 0; i <= n; i++) {
            float t = i / (float) n;
            float y = 1.2f + t * 19f;
            float w = 2.1f * (t > 0.78f ? 1f - (t - 0.78f) / 0.22f * 0.97f : 1f - 0.12f * t);
            spine[i] = new float[]{-0.85f, y};
            edge[i] = new float[]{-0.85f + w, y};
        }
        strip(m, spine, edge, 0.36f, f);
        for (int k = 0; k < 7; k++) {
            float y = 2.4f + k * 2.2f;
            m.extrude(new float[][]{{-0.84f, y}, {-1.9f, y + 0.9f}, {-0.84f, y + 0.55f}}, -0.15f, 0.15f, 0xFF3A0A16, 0xFF3A0A16);
        }
    }

    private static Finish solid(int blade, int edge) {
        return new Finish() {
            public int blade(float a) {
                return Colors.lerp(blade, Colors.brighten(blade, 0.12f), a);
            }

            public int edge(float a) {
                return edge;
            }

            public int handle() {
                return blade;
            }

            public int accent() {
                return edge;
            }

            public int metal() {
                return edge;
            }
        };
    }
}
