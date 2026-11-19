package dev.aqua.client.module.cosmetic;

import dev.aqua.client.Aqua;
import dev.aqua.client.cosmetic.Mesh;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Colors;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.setting.EnumSetting;
import dev.aqua.client.setting.NumberSetting;

/** Animated wings for your own player (R1 "Wings"): angel, devil, butterfly, crystal and feather styles. */
public final class Wings extends Module {
    public enum Style implements EnumSetting.Displayable {
        ANGEL("Angel"), DEVIL("Devil"), BUTTERFLY("Butterfly"), CRYSTAL("Crystal"), FEATHER("Aqua feather");
        final String l;

        Style(String l) {
            this.l = l;
        }

        public String displayName() {
            return l;
        }
    }

    private final EnumSetting<Style> style = add(new EnumSetting<>("style", "Style", Style.ANGEL));
    private final BoolSetting customColours = add(new BoolSetting("custom_colours", "Custom colours", false));
    private final ColorSetting inner = add(new ColorSetting("inner", "Inner colour", 0xFF52E4F2, false, true).visibleWhen(customColours::on));
    private final ColorSetting outer = add(new ColorSetting("outer", "Tip colour", 0xFFA8F3FF, false, false).visibleWhen(customColours::on));
    private final NumberSetting size = add(new NumberSetting("size", "Size", 1.0, 0.6, 1.5, 0.05).unit("x"));
    private final NumberSetting flap = add(new NumberSetting("flap", "Flap speed", 1.0, 0.0, 3.0, 0.1).unit("x"));

    public Wings() {
        super("wings", "Wings", "Angel, devil, butterfly, crystal or feather wings (you only).", Category.COSMETICS, false,
                "wings", "angel", "devil", "butterfly", "back");
    }

    public Style style() {
        return style.get();
    }

    private int[] colours(Style s) {
        if (customColours.on()) {
            return new int[]{inner.color(), outer.color(), Colors.darken(inner.color(), 0.5f)};
        }
        return switch (s) {
            case ANGEL -> new int[]{0xFFF4F8FC, 0xFFFFFFFF, 0xFFD6E2EE};
            case DEVIL -> new int[]{0xFF8A1424, 0xFFB51F33, 0xFF2A0A10};
            case BUTTERFLY -> new int[]{Aqua.theme().accent(), Aqua.theme().accent2(), 0xFF1B2733};
            case CRYSTAL -> new int[]{0xFF52E4F2, 0xFFDDFBFF, 0xFF2A8FC4};
            case FEATHER -> new int[]{Aqua.theme().accent(), Aqua.theme().accent2(), Aqua.theme().elevated()};
        };
    }

    /** Maps wing-local (u outward, v upward, w back) to body space pixels for one side. */
    private static float[] p(int side, float u, float v, float w, float open, float scale) {
        u *= scale;
        v *= scale;
        float cos = (float) Math.cos(open), sin = (float) Math.sin(open);
        return new float[]{side * (1.5f + u * cos), 2.5f - v, 2.4f + w + u * sin};
    }

    private static void q(Mesh m, float[] a, float[] b, float[] c, float[] d, int col, float shade) {
        m.quad(a[0], a[1], a[2], b[0], b[1], b[2], c[0], c[1], c[2], d[0], d[1], d[2], col, shade);
    }

    /** Draws both wings in body space (pixels). */
    public void draw(Mesh m, Style s, float age, boolean moving) {
        float speed = flap.f() * (moving ? 1.7f : 1f);
        float t = age / 20f;
        float intensity = Math.min(1f, Aqua.ui().effectIntensity.f());
        float open = 0.45f + (float) Math.sin(t * Math.PI * speed) * 0.32f * intensity;
        float sc = size.f();
        int[] c = colours(s);
        for (int side = -1; side <= 1; side += 2) {
            switch (s) {
                case ANGEL -> angel(m, side, open, sc, c);
                case DEVIL -> devil(m, side, open, sc, c);
                case BUTTERFLY -> butterfly(m, side, open, sc, c, t);
                case CRYSTAL -> crystal(m, side, open, sc, c);
                case FEATHER -> feather(m, side, open, sc, c);
            }
        }
    }

    private void angel(Mesh m, int side, float open, float sc, int[] c) {
        // Three rows of overlapping feathers: coverts, secondaries, long primaries.
        float[][] rows = {{6f, 3.2f, 0f}, {10f, 3.0f, 0.3f}, {15f, 2.6f, 0.6f}};
        for (int r = 0; r < rows.length; r++) {
            int n = 6 + r;
            for (int i = 0; i < n; i++) {
                float f = i / (float) (n - 1);
                float u0 = 1f + f * rows[r][0] * 0.9f;
                float len = rows[r][0] * (0.55f + 0.45f * (float) Math.sin(f * Math.PI * 0.9f + 0.2f));
                float v0 = 8f - f * 6f - r * 1.5f;
                float u1 = u0 + len * 0.35f;
                float v1 = v0 - len;
                float w = rows[r][1];
                int col = Colors.lerp(c[2], c[1], f * 0.6f + r * 0.2f);
                q(m, p(side, u0, v0, rows[r][2], open, sc), p(side, u0 + w, v0, rows[r][2], open, sc),
                        p(side, u1 + w * 0.6f, v1, rows[r][2] + 0.2f, open, sc), p(side, u1, v1 + 0.5f, rows[r][2] + 0.2f, open, sc),
                        col, 0.95f - r * 0.06f);
            }
        }
    }

    private void devil(Mesh m, int side, float open, float sc, int[] c) {
        float[] root = p(side, 0f, 6f, 0f, open, sc);
        float[] elbow = p(side, 7f, 13f, 0f, open, sc);
        float[] rootB = p(side, 0f, 4.8f, 0f, open, sc);
        float[] elbowB = p(side, 7f, 12f, 0f, open, sc);
        // upper arm bone
        m.quad(root[0], root[1], root[2], elbow[0], elbow[1], elbow[2], elbowB[0], elbowB[1], elbowB[2], rootB[0], rootB[1], rootB[2], c[2], 0.9f);
        float[][] tips = {{15f, 12f}, {17f, 5f}, {15f, -2f}, {9f, -5f}};
        float[] prev = p(side, 2f, 0f, 0f, open, sc);
        for (int i = 0; i < tips.length; i++) {
            float[] tip = p(side, tips[i][0], tips[i][1], 0.3f, open, sc);
            // finger bone: thin strip from the elbow to the tip
            float dx = tip[0] - elbow[0], dy = tip[1] - elbow[1];
            float len = (float) Math.sqrt(dx * dx + dy * dy) + 1e-4f;
            float nx = -dy / len * 0.45f, ny = dx / len * 0.45f;
            m.quad(elbow[0] - nx, elbow[1] - ny, elbow[2] - 0.05f, elbow[0] + nx, elbow[1] + ny, elbow[2] - 0.05f,
                    tip[0] + nx * 0.4f, tip[1] + ny * 0.4f, tip[2] - 0.05f, tip[0] - nx * 0.4f, tip[1] - ny * 0.4f, tip[2] - 0.05f, c[2], 0.8f);
            // membrane between previous edge and this finger, with a scalloped edge point
            float[] mid = p(side, (tips[i][0] + (i == 0 ? 2f : tips[i - 1][0])) * 0.45f, (tips[i][1] + (i == 0 ? 0f : tips[i - 1][1])) * 0.5f - 2.5f, 0.15f, open, sc);
            q(m, elbow, tip, mid, prev, i % 2 == 0 ? c[0] : c[1], 0.85f);
            prev = tip;
        }
        q(m, elbow, prev, p(side, 3f, -1f, 0.1f, open, sc), root, c[0], 0.8f);
        // claw at the elbow
        m.tri(elbow[0] - 0.7f, elbow[1], elbow[2], elbow[0] + 0.7f, elbow[1], elbow[2], elbow[0] + side * 0.6f, elbow[1] - 2.2f, elbow[2], 0xFF1A0508, 1f);
    }

    private void butterfly(Mesh m, int side, float open, float sc, int[] c, float t) {
        float[] root = p(side, 0f, 4f, 0f, open, sc);
        int lobes = 10;
        for (int part = 0; part < 2; part++) {
            float startA = part == 0 ? 0.15f : -1.45f;
            float endA = part == 0 ? 1.55f : -0.15f;
            float rad = part == 0 ? 13f : 9f;
            float[] prev = null;
            for (int i = 0; i <= lobes; i++) {
                float a = startA + (endA - startA) * i / lobes;
                float r = rad * (0.75f + 0.25f * (float) Math.sin(i / (float) lobes * Math.PI));
                float[] pt = p(side, (float) Math.cos(a) * r + 0.5f, 4f + (float) Math.sin(a) * r, 0.1f, open, sc);
                if (prev != null) {
                    int col = Colors.lerp(c[0], c[1], i / (float) lobes);
                    m.tri(root[0], root[1], root[2], prev[0], prev[1], prev[2], pt[0], pt[1], pt[2], col, 0.95f);
                }
                prev = pt;
            }
            // spots
            float[] spot = p(side, rad * 0.55f, 4f + (part == 0 ? rad * 0.45f : -rad * 0.4f), 0.05f, open, sc);
            m.box(spot[0] - 1.2f, spot[1] - 1.2f, spot[2] - 0.3f, spot[0] + 1.2f, spot[1] + 1.2f, spot[2] - 0.05f, c[2]);
        }
    }

    private void crystal(Mesh m, int side, float open, float sc, int[] c) {
        for (int i = 0; i < 5; i++) {
            float f = i / 4f;
            float spread = -0.4f + f * 1.6f;
            float len = (8f + 6f * (float) Math.sin(f * Math.PI)) ;
            float u = (float) Math.cos(spread) * len, v = 5f + (float) Math.sin(spread) * len;
            float[] a = p(side, 0.5f, 5f, 0f, open, sc);
            float[] tip = p(side, u, v, 0.4f, open, sc);
            float[] l = p(side, u * 0.5f - 1.2f, 5f + (v - 5f) * 0.5f + 1.2f, 0.2f, open, sc);
            float[] r = p(side, u * 0.5f + 1.2f, 5f + (v - 5f) * 0.5f - 1.2f, 0.2f, open, sc);
            m.quad(a[0], a[1], a[2], l[0], l[1], l[2], tip[0], tip[1], tip[2], r[0], r[1], r[2], Colors.lerp(c[0], c[1], f), 1f);
        }
    }

    private void feather(Mesh m, int side, float open, float sc, int[] c) {
        int n = 7;
        for (int i = 0; i < n; i++) {
            float f = i / (float) (n - 1);
            float spread = -0.45f + f * 1.55f;
            float len = (9f + 9f * (float) Math.sin(f * Math.PI * 0.85f + 0.25f));
            float u = (float) Math.cos(spread) * len, v = 5f + (float) Math.sin(spread) * len;
            float px = -(float) Math.sin(spread) * 1.5f, py = (float) Math.cos(spread) * 1.5f;
            q(m, p(side, 0.5f - px * 0.3f, 5f - py * 0.3f, 0f, open, sc), p(side, 0.5f + px * 0.3f, 5f + py * 0.3f, 0f, open, sc),
                    p(side, u + px, v + py, 0.4f, open, sc), p(side, u - px, v - py, 0.4f, open, sc), Colors.lerp(c[0], c[1], f * 0.5f), 0.95f);
        }
    }
}
