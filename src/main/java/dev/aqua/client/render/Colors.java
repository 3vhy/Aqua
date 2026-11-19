package dev.aqua.client.render;

/** ARGB colour helpers. */
public final class Colors {
    private Colors() {}

    public static int argb(int a, int r, int g, int b) {
        return (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    public static int rgb(int rgb) {
        return 0xFF000000 | rgb;
    }

    public static int alpha(int c) {
        return (c >>> 24) & 0xFF;
    }

    public static int withAlpha(int c, int a) {
        return (clamp(a) << 24) | (c & 0xFFFFFF);
    }

    public static int withAlpha(int c, float a) {
        return withAlpha(c, Math.round(a * 255f));
    }

    /** Multiplies the existing alpha by f (0..1). */
    public static int fade(int c, float f) {
        if (f >= 1f) {
            return c;
        }
        return withAlpha(c, Math.round(alpha(c) * Math.max(0f, f)));
    }

    public static int lerp(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = alpha(a), ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int ba = alpha(b), br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return argb(Math.round(aa + (ba - aa) * t), Math.round(ar + (br - ar) * t), Math.round(ag + (bg - ag) * t),
                Math.round(ab + (bb - ab) * t));
    }

    public static int brighten(int c, float f) {
        return lerp(c, withAlpha(0xFFFFFF, alpha(c)), f);
    }

    public static int darken(int c, float f) {
        return lerp(c, withAlpha(0x000000, alpha(c)), f);
    }

    public static float r(int c) {
        return ((c >> 16) & 255) / 255f;
    }

    public static float g(int c) {
        return ((c >> 8) & 255) / 255f;
    }

    public static float b(int c) {
        return (c & 255) / 255f;
    }

    public static float a(int c) {
        return alpha(c) / 255f;
    }

    public static int hsv(float h, float s, float v, int alpha) {
        int rgb = java.awt.Color.HSBtoRGB(h, s, v);
        return withAlpha(rgb, alpha);
    }

    public static float[] toHsv(int c) {
        return java.awt.Color.RGBtoHSB((c >> 16) & 255, (c >> 8) & 255, c & 255, null);
    }

    public static String hex(int c, boolean alpha) {
        return alpha ? String.format("#%08X", c) : String.format("#%06X", c & 0xFFFFFF);
    }

    public static Integer parseHex(String s) {
        if (s == null) {
            return null;
        }
        s = s.trim();
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        try {
            if (s.length() == 6) {
                return 0xFF000000 | Integer.parseUnsignedInt(s, 16);
            }
            if (s.length() == 8) {
                return (int) Long.parseLong(s, 16);
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
