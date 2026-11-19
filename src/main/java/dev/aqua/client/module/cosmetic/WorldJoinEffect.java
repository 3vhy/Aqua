package dev.aqua.client.module.cosmetic;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.R2D;
import dev.aqua.client.render.font.AquaFont;
import dev.aqua.client.setting.NumberSetting;
import dev.aqua.client.setting.StringSetting;
import net.minecraft.client.gui.DrawContext;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Cinematic title when you join a world: local time + "somewhere on ..." typed out letter by letter
 * (R4 0:01-0:04; R1 "World Join Effect").
 */
public final class WorldJoinEffect extends Module {
    private final StringSetting prefix = add(new StringSetting("prefix", "Second line prefix", "Somewhere on", 32));
    private final NumberSetting duration = add(new NumberSetting("duration", "Duration", 4.5, 2, 10, 0.5).unit(" s"));
    private long start = -1;
    private String line1 = "";
    private String line2 = "";

    public WorldJoinEffect() {
        super("world_join_effect", "World Join Effect", "A typewriter title with the time and place when you join.", Category.COSMETICS,
                true, "join title", "intro", "typewriter", "cinematic");
        hideFromModuleList();
    }

    public void trigger() {
        if (!isEnabled()) {
            return;
        }
        line1 = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH));
        String where;
        if (mc.isInSingleplayer() && mc.getServer() != null) {
            where = mc.getServer().getSaveProperties().getLevelName();
        } else if (mc.getCurrentServerEntry() != null) {
            where = mc.getCurrentServerEntry().name;
        } else {
            where = "a new world";
        }
        line2 = (prefix.get().isBlank() ? "" : prefix.get() + " ") + where;
        line2 = line2.toUpperCase(Locale.ROOT);
        start = System.currentTimeMillis();
    }

    public void render(DrawContext ctx) {
        if (start < 0 || !isEnabled()) {
            return;
        }
        float t = (System.currentTimeMillis() - start) / 1000f;
        float dur = duration.f();
        if (t > dur) {
            start = -1;
            return;
        }
        float fadeOut = Math.min(1f, (dur - t) / 0.6f);
        float cx = ctx.getScaledWindowWidth() / 2f;
        float cy = ctx.getScaledWindowHeight() * 0.32f;
        int text = Colors.fade(Aqua.theme().text(), fadeOut);
        float a1 = Math.min(1f, t / 0.5f);
        R2D.textCentered(ctx, line1, cx, cy, 9f, AquaFont.Weight.BOLD, Colors.fade(text, a1));
        int chars = Math.min(line2.length(), (int) Math.max(0, (t - 0.6f) * 28));
        String typed = line2.substring(0, chars);
        StringBuilder spaced = new StringBuilder();
        for (char c : typed.toCharArray()) {
            spaced.append(c).append(' ');
        }
        String s = spaced.toString().trim();
        float w = AquaFont.width(s, 4.6f, AquaFont.Weight.SEMIBOLD);
        R2D.text(ctx, s, cx - w / 2f, cy + 8, 4.6f, AquaFont.Weight.SEMIBOLD, Colors.fade(Colors.withAlpha(Aqua.theme().text(), 0.8f), fadeOut));
        if (chars < line2.length() && (System.currentTimeMillis() / 400) % 2 == 0) {
            R2D.rect(ctx, cx + w / 2f + 1.5f, cy + 8.5f, 1f, 5.5f, Colors.fade(Aqua.theme().accent(), fadeOut));
        }
    }
}
