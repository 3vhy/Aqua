package dev.aqua.client.theme;

import dev.aqua.client.render.Colors;

/** Aqua colour palette. All colours are opaque ARGB; translucency is applied at use sites. */
public record Theme(String name, int background, int panel, int elevated, int accent, int accent2, int text, int muted) {

    public static final Theme AQUA = new Theme("Aqua", 0xFF101820, 0xFF18232D, 0xFF22323F, 0xFF52E4F2, 0xFFA8F3FF, 0xFFF2FAFC, 0xFF98ADB9);
    public static final Theme MONO = new Theme("Mono", 0xFF111214, 0xFF1A1B1E, 0xFF26282C, 0xFFE6EAEE, 0xFFFFFFFF, 0xFFF4F5F6, 0xFF9A9EA6);
    public static final Theme MINT = new Theme("Mint", 0xFF0F1813, 0xFF16231B, 0xFF213328, 0xFF5BE38C, 0xFFB6F5CC, 0xFFF1FBF4, 0xFF94B3A0);
    public static final Theme SAKURA = new Theme("Sakura", 0xFF160F14, 0xFF21161D, 0xFF2E1F29, 0xFFFF5FAE, 0xFFFFB8DA, 0xFFFDF2F7, 0xFFB79AAA);
    public static final Theme AMETHYST = new Theme("Amethyst", 0xFF120F1A, 0xFF1B1726, 0xFF272135, 0xFFB48CFF, 0xFFDCC9FF, 0xFFF6F2FF, 0xFFA69CBD);
    public static final Theme EMBER = new Theme("Ember", 0xFF17110E, 0xFF221915, 0xFF30231C, 0xFFFF8A4C, 0xFFFFC9A8, 0xFFFCF4EF, 0xFFB8A193);
    public static final Theme[] PRESETS = {AQUA, MONO, MINT, SAKURA, AMETHYST, EMBER};

    /** Window / large surface colour with the client's standard translucency. */
    public int windowBg(float opacity) {
        return Colors.withAlpha(background, opacity);
    }

    /** Fern-style HUD capsule background. */
    public int hudBg(float opacity) {
        return Colors.withAlpha(Colors.darken(background, 0.25f), opacity);
    }

    public int border() {
        return Colors.withAlpha(Colors.brighten(elevated, 0.25f), 0.55f);
    }

    public int divider() {
        return Colors.withAlpha(Colors.brighten(elevated, 0.15f), 0.6f);
    }

    public int accentDim() {
        return Colors.lerp(elevated, accent, 0.35f);
    }

    public Theme withName(String n) {
        return new Theme(n, background, panel, elevated, accent, accent2, text, muted);
    }

    public Theme with(String field, int color) {
        int c = color | 0xFF000000;
        return switch (field) {
            case "background" -> new Theme(name, c, panel, elevated, accent, accent2, text, muted);
            case "panel" -> new Theme(name, background, c, elevated, accent, accent2, text, muted);
            case "elevated" -> new Theme(name, background, panel, c, accent, accent2, text, muted);
            case "accent" -> new Theme(name, background, panel, elevated, c, accent2, text, muted);
            case "accent2" -> new Theme(name, background, panel, elevated, accent, c, text, muted);
            case "text" -> new Theme(name, background, panel, elevated, accent, accent2, c, muted);
            case "muted" -> new Theme(name, background, panel, elevated, accent, accent2, text, c);
            default -> this;
        };
    }

    public int get(String field) {
        return switch (field) {
            case "background" -> background;
            case "panel" -> panel;
            case "elevated" -> elevated;
            case "accent" -> accent;
            case "accent2" -> accent2;
            case "text" -> text;
            default -> muted;
        };
    }

    public int onAccent() {
        // Dark text on bright accents for contrast.
        float l = 0.2126f * Colors.r(accent) + 0.7152f * Colors.g(accent) + 0.0722f * Colors.b(accent);
        return l > 0.55f ? 0xFF0B1217 : 0xFFFFFFFF;
    }
}
