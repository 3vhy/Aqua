package dev.aqua.client.module.qol;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Colors;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.setting.NumberSetting;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Local chat appearance: background colour/opacity and optional timestamps (baseline addition). */
public final class ChatStyle extends Module {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm");
    private final ColorSetting background = add(new ColorSetting("background", "Background", 0xFF101820, false, false));
    private final BoolSetting themeBg = add(new BoolSetting("theme_bg", "Use theme background", true));
    private final NumberSetting opacity = add(new NumberSetting("opacity", "Background opacity", 0.55, 0.0, 1.0, 0.05));
    private final BoolSetting timestamps = add(new BoolSetting("timestamps", "Timestamps", false));
    private final BoolSetting glass = add(new BoolSetting("glass", "Frosted glass", true)
            .describe("Chat lines sit on frosted glass that blurs the world behind them (Glass style only)"));

    public ChatStyle() {
        super("chat_style", "Chat Style", "Frosted glass chat, colours and timestamps.", Category.QOL, true,
                "chat", "timestamps", "background");
    }

    /** Recolours a vanilla chat background fill, keeping its fade-out alpha. */
    public int recolor(int original) {
        float fade = Colors.a(original) / 0.5f;
        int base = themeBg.on() ? Colors.darken(Aqua.theme().background(), 0.1f) : background.color();
        return Colors.withAlpha(base, Math.min(1f, fade) * opacity.f());
    }

    private static long glassLines;

    /** Chat line backgrounds drawn as glass (used by the in-game tests). */
    public static long glassLines() {
        return glassLines;
    }

    /**
     * Draws one chat line background as frosted glass. Lines touch, so together they form one
     * seamless pane. Returns false when glass is off (the caller then draws the normal fill).
     */
    public boolean drawGlass(net.minecraft.client.gui.DrawContext ctx, int x1, int y1, int x2, int y2, int original) {
        if (!glass.on() || !dev.aqua.client.render.Glass.on() || x2 <= x1 || y2 <= y1) {
            return false;
        }
        glassLines++;
        float fade = Math.min(1f, Colors.a(original) / 0.5f);
        int tint = Colors.withAlpha(Colors.darken(Aqua.theme().background(), 0.1f), Math.min(1f, fade * opacity.f() * 0.75f));
        dev.aqua.client.render.R2D.glass(ctx, x1, y1, x2 - x1, y2 - y1, 0, dev.aqua.client.render.R2D.ALL, 0.2f,
                Colors.withAlpha(0xFFFFFF, Math.min(1f, fade * 1.2f)), tint);
        dev.aqua.client.render.R2D.rect(ctx, x1, y1, x2 - x1, y2 - y1, tint);
        return true;
    }

    public Text decorate(Text message) {
        if (!isEnabled() || !timestamps.on()) {
            return message;
        }
        MutableText stamp = Text.literal("[" + LocalTime.now().format(FMT) + "] ").formatted(Formatting.DARK_GRAY);
        return stamp.append(message);
    }
}
