package dev.aqua.client.hud.widgets;

import dev.aqua.client.hud.Anchor;
import dev.aqua.client.hud.HudWidget;
import dev.aqua.client.hud.Pill;
import dev.aqua.client.render.Icon;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.util.Session;
import net.minecraft.client.gui.DrawContext;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Local clock and session duration (baseline addition). */
public final class ClockWidget extends HudWidget {
    private static final DateTimeFormatter H24 = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter H12 = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private final BoolSetting twelveHour = add(new BoolSetting("twelve_hour", "12-hour clock", false));
    private final BoolSetting session = add(new BoolSetting("session", "Session time", true));

    public ClockWidget() {
        super("clock", "Clock", "Local time and how long you have been playing.", Icon.CLOCK, false, Anchor.TOP_LEFT, 230, 21,
                "time", "session", "playtime");
    }

    private Pill pill(boolean preview) {
        Pill p = Pill.create().icon(Icon.CLOCK).value(LocalTime.now().format(twelveHour.on() ? H12 : H24));
        if (session.on()) {
            long d = Session.durationMs();
            p.dot().value(d < 0 && preview ? "12:34" : Session.format(d)).unit("session");
        }
        return p;
    }

    @Override
    public float width(boolean preview) {
        return pill(preview).width();
    }

    @Override
    public float height(boolean preview) {
        return Pill.HEIGHT;
    }

    @Override
    public void draw(DrawContext ctx, boolean preview, float alpha) {
        pill(preview).draw(ctx, 0, 0, showBackground(), bg(alpha), alpha);
    }
}
