package dev.aqua.client.gui.click;

import dev.aqua.client.Aqua;
import dev.aqua.client.render.Colors;
import dev.aqua.client.theme.Theme;

/**
 * Colours of the panel menu. With the default Aqua theme they match the reference video exactly
 * (deep blue highlights, a royal-blue settings card); other themes tint them with their accent.
 */
public final class ClickStyle {
    private ClickStyle() {}

    public static final int PANEL_BG = 0xDC121519;
    public static final int GRID = 0x09FFFFFF;
    public static final int ROW_TEXT = 0xFFC9CED6;
    public static final int HEADER_TEXT = 0xFFF2F4F7;
    public static final int MUTED = 0xFF8B939C;
    public static final int FIELD = 0xFF292D35;
    public static final int TOOLTIP_BG = 0xF2191C21;
    public static final int TAB_BG = 0xE6121518;
    public static final int CARD_TEXT = 0xFFF4F6FF;
    public static final int CARD_DARK = 0xFF161A2A;
    public static final int CARD_LINE = 0x14FFFFFF;

    private static final int VIDEO_BLUE = 0xFF1A87C9;
    private static final int VIDEO_CARD = 0xFF1E2896;

    private static boolean defaultTheme() {
        return Aqua.themes().presetName().equals(Theme.AQUA.name());
    }

    /** Enabled rows, the selected tab, switches that are on and primary buttons. */
    public static int blue() {
        return defaultTheme() ? VIDEO_BLUE : Colors.lerp(Aqua.theme().accent(), 0xFF0E5E9C, 0.45f);
    }

    /** The settings card. */
    public static int card() {
        return defaultTheme() ? VIDEO_CARD : Colors.lerp(Colors.darken(Aqua.theme().accent(), 0.5f), 0xFF151A40, 0.35f);
    }

    public static int cardMuted() {
        return Colors.lerp(card(), 0xFFFFFFFF, 0.62f);
    }

    /** Off switch track on the card. */
    public static int cardTrack() {
        return Colors.lerp(card(), 0xFF000000, 0.45f);
    }
}
