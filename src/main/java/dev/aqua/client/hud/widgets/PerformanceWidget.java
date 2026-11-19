package dev.aqua.client.hud.widgets;

import dev.aqua.client.hud.Anchor;
import dev.aqua.client.hud.HudWidget;
import dev.aqua.client.hud.Pill;
import dev.aqua.client.render.Icon;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.util.FrameStats;
import dev.aqua.client.util.TextUtil;
import net.minecraft.client.gui.DrawContext;

/** "115 FPS • 1141 / 16384 MB" (Fern R1 0:00) plus optional frame time. */
public final class PerformanceWidget extends HudWidget {
    private final BoolSetting frameTime = add(new BoolSetting("frame_time", "Frame time", true));
    private final BoolSetting memory = add(new BoolSetting("memory", "Memory", true));

    public PerformanceWidget() {
        super("performance", "Performance", "FPS, frame time and memory use.", Icon.MEMORY, true, Anchor.TOP_LEFT, 140, 5,
                "fps", "frames", "ram", "memory", "frame time");
    }

    private Pill pill() {
        Pill p = Pill.create().icon(Icon.MEMORY).value(String.valueOf(mc.getCurrentFps())).unit("FPS");
        if (frameTime.on()) {
            p.dot().value(TextUtil.fmt1(FrameStats.frameTimeMs())).unit("ms");
        }
        if (memory.on()) {
            Runtime rt = Runtime.getRuntime();
            long used = (rt.totalMemory() - rt.freeMemory()) / 1048576L;
            long max = rt.maxMemory() / 1048576L;
            p.dot().value(used + " / " + max).unit("MB");
        }
        return p;
    }

    @Override
    public float width(boolean preview) {
        return pill().width();
    }

    @Override
    public float height(boolean preview) {
        return Pill.HEIGHT;
    }

    @Override
    public void draw(DrawContext ctx, boolean preview, float alpha) {
        pill().draw(ctx, 0, 0, showBackground(), bg(alpha), alpha);
    }
}
