package dev.aqua.client.cosmetic.knife;

import dev.aqua.client.cosmetic.Mesh;
import dev.aqua.client.render.Colors;

/**
 * Original knife models. Local frame (pixels): +Y runs from the grip towards the tip, X is blade
 * width, Z thickness; the grip centre is the origin.
 */
public final class KnifeModels {
    private KnifeModels() {}

    public interface Finish {
        int blade(float along);

        int edge(float along);
    }

    // ------------------------------------------------------------------ helpers

    /** Flat blade strip between a spine polyline and an edge polyline, with thickness and edge bevel. */
    private static void strip(Mesh m, float[][] spine, float[][] edge, float thick, Finish f) {
        int n = spine.length;
        for (int i = 0; i < n - 1; i++) {
            float a0 = i / (float) (n - 1), a1 = (i + 1) / (float) (n - 1);
            int c = Colors.lerp(f.blade(a0), f.blade(a1), 0.5f);
            int e = f.edge((a0 + a1) / 2f);
            float[] s0 = spine[i], s1 = spine[i + 1], e0 = edge[i], e1 = edge[i + 1];
            // two flat sides
            m.quad(s0[0], s0[1], -thick, s1[0], s1[1], -thick, e1[0], e1[1], -thick * 0.25f, e0[0], e0[1], -thick * 0.25f, c, 0.95f);
            m.quad(s0[0], s0[1], thick, e0[0], e0[1], thick * 0.25f, e1[0], e1[1], thick * 0.25f, s1[0], s1[1], thick, c, 0.8f);
            // spine face
            m.quad(s0[0], s0[1], -thick, s0[0], s0[1], thick, s1[0], s1[1], thick, s1[0], s1[1], -thick, Colors.darken(c, 0.15f), 0.7f);
            // sharpened edge face
            m.quad(e0[0], e0[1], -thick * 0.25f, e1[0], e1[1], -thick * 0.25f, e1[0], e1[1], thick * 0.25f, e0[0], e0[1], thick * 0.25f, e, 1f);
        }
    }

    private static void wrappedGrip(Mesh m, float y0, float y1, float w, float d, int base, int wrap) {
        m.box(-w / 2, y0, -d / 2, w / 2, y1, d / 2, base);
        for (float y = y0 + 0.4f; y < y1 - 0.6f; y += 1.4f) {
            m.box(-w / 2 - 0.12f, y, -d / 2 - 0.12f, w / 2 + 0.12f, y + 0.6f, d / 2 + 0.12f, wrap);
        }
    }

    // ------------------------------------------------------------------ knives

    public static void dagger(Mesh m, Finish f) {
        m.box(-1.2f, -5.2f, -1.2f, 1.2f, -4.2f, 1.2f, 0xFFB8C2CC);
        wrappedGrip(m, -4.2f, 2f, 1.6f, 1.4f, 0xFF2A1E18, 0xFF4A3426);
        m.box(-3.4f, 2f, -0.9f, 3.4f, 2.9f, 0.9f, 0xFFB8C2CC);
        int n = 12;
        float[][] l = new float[n + 1][], r = new float[n + 1][];
        for (int i = 0; i <= n; i++) {
            float t = i / (float) n;
            float y = 2.9f + t * 11.5f;
            float hw = 1.25f * (1f - t * t * 0.95f) + 0.02f;
            l[i] = new float[]{0f, y};
            r[i] = new float[]{hw, y};
        }
        strip(m, l, r, 0.32f, f);
        for (int i = 0; i <= n; i++) {
            r[i][0] = -r[i][0];
        }
        strip(m, l, r, 0.32f, f);
    }

    public static void katana(Mesh m, Finish f) {
        wrappedGrip(m, -8f, 1f, 1.5f, 1.2f, 0xFF15161A, 0xFF2A8FC4);
        m.box(-0.85f, -8.6f, -0.7f, 0.85f, -8f, 0.7f, 0xFFC9A54C);
        m.cylinder(0f, 1f, 1.5f, 0f, 2.3f, 14, 0xFF2B2D33);
        m.box(-0.8f, 1.5f, -0.6f, 0.8f, 2.4f, 0.6f, 0xFFC9A54C);
        int n = 18;
        float[][] spine = new float[n + 1][], edge = new float[n + 1][];
        for (int i = 0; i <= n; i++) {
            float t = i / (float) n;
            float y = 2.4f + t * 21f;
            float curve = -t * t * 1.6f;
            float w = 1.25f;
            if (t > 0.9f) {
                w *= 1f - (t - 0.9f) / 0.1f * 0.95f;
            }
            spine[i] = new float[]{curve - 0.55f, y};
            edge[i] = new float[]{curve - 0.55f + w, y - (t > 0.9f ? (t - 0.9f) * 6f : 0)};
        }
        strip(m, spine, edge, 0.28f, f);
    }

    public static void karambit(Mesh m, Finish f, float spin) {
        // Finger ring
        for (int i = 0; i < 16; i++) {
            double a0 = Math.PI * 2 * i / 16;
            float x = (float) Math.cos(a0) * 1.9f, y = -6.6f + (float) Math.sin(a0) * 1.9f;
            m.box(x - 0.45f, y - 0.45f, -0.55f, x + 0.45f, y + 0.45f, 0.55f, 0xFF9AA5B1);
        }
        wrappedGrip(m, -4.7f, 1.2f, 1.7f, 1.3f, 0xFF1E2228, 0xFF3A4A58);
        m.box(-1.2f, 1.2f, -0.75f, 1.6f, 2f, 0.75f, 0xFF9AA5B1);
        // Claw blade: spine on the outer arc, edge on the inner (concave) arc
        int n = 14;
        float cx = 6.2f, cy = 2f;
        float[][] spine = new float[n + 1][], edge = new float[n + 1][];
        for (int i = 0; i <= n; i++) {
            float t = i / (float) n;
            double a = Math.toRadians(180 - t * 95);
            float ro = 6.2f, ri = 6.2f - 2.1f * (1f - t * 0.95f);
            spine[i] = new float[]{cx + (float) Math.cos(a) * ro - 0.6f, cy + (float) Math.sin(a) * ro};
            edge[i] = new float[]{cx + (float) Math.cos(a) * ri, cy + (float) Math.sin(a) * ri};
        }
        strip(m, spine, edge, 0.3f, f);
    }

    public static void butterfly(Mesh m, Finish f) {
        int handle = 0xFF2C3440, hole = 0xFF0E1116;
        for (int side = -1; side <= 1; side += 2) {
            float x = side * 0.75f;
            m.box(x - 0.6f, -9f, -0.6f, x + 0.6f, 0.4f, 0.6f, handle);
            for (int i = 0; i < 5; i++) {
                float y = -8f + i * 1.7f;
                m.box(x - 0.62f, y, -0.62f, x + 0.62f, y + 0.8f, -0.55f, hole);
            }
            m.box(x - 0.7f, 0.2f, -0.7f, x + 0.7f, 0.9f, 0.7f, 0xFF9AA5B1);
        }
        m.box(-0.3f, -9.6f, -0.3f, 1.6f, -9f, 0.3f, 0xFF9AA5B1);
        // Tanto-style blade
        int n = 10;
        float[][] spine = new float[n + 1][], edge = new float[n + 1][];
        for (int i = 0; i <= n; i++) {
            float t = i / (float) n;
            float y = 0.9f + t * 10f;
            spine[i] = new float[]{-0.6f, y};
            float w = t < 0.8f ? 1.5f : 1.5f * (1f - (t - 0.8f) / 0.2f) + 0.05f;
            edge[i] = new float[]{-0.6f + w, y + (t >= 0.8f ? (t - 0.8f) * 2f : 0)};
        }
        strip(m, spine, edge, 0.28f, f);
    }
}
