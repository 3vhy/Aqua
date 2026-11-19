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

    public ChatStyle() {
        super("chat_style", "Chat Style", "Changes how chat looks on your screen.", Category.QOL, false,
                "chat", "timestamps", "background");
    }

    /** Recolours a vanilla chat background fill, keeping its fade-out alpha. */
    public int recolor(int original) {
        float fade = Colors.a(original) / 0.5f;
        int base = themeBg.on() ? Colors.darken(Aqua.theme().background(), 0.1f) : background.color();
        return Colors.withAlpha(base, Math.min(1f, fade) * opacity.f());
    }

    public Text decorate(Text message) {
        if (!isEnabled() || !timestamps.on()) {
            return message;
        }
        MutableText stamp = Text.literal("[" + LocalTime.now().format(FMT) + "] ").formatted(Formatting.DARK_GRAY);
        return stamp.append(message);
    }
}
