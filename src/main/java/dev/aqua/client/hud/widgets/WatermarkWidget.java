package dev.aqua.client.hud.widgets;

import dev.aqua.client.Aqua;
import dev.aqua.client.hud.Anchor;
import dev.aqua.client.hud.HudWidget;
import dev.aqua.client.hud.Pill;
import dev.aqua.client.render.Icon;
import dev.aqua.client.setting.BoolSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/** "Aqua • 1.0.0" brand capsule (Fern R1/R2 0:00), optional FPS (R3 0:00). */
public final class WatermarkWidget extends HudWidget {
    public static final Identifier EMBLEM = Identifier.of("aqua", "textures/gui/emblem_small.png");
    private final BoolSetting showVersion = add(new BoolSetting("version", "Show version", true));
    private final BoolSetting showFps = add(new BoolSetting("fps", "Show FPS", false));

    public WatermarkWidget() {
        super("watermark", "Watermark", "Aqua brand capsule with version.", Icon.DROP, true, Anchor.TOP_LEFT, 5, 5, "brand", "logo");
    }

    private Pill pill() {
        Pill p = Pill.create().logo(EMBLEM).value(Aqua.NAME);
        if (showVersion.on()) {
            p.dot().muted(Aqua.VERSION);
        }
        if (showFps.on()) {
            p.dot().value(String.valueOf(mc.getCurrentFps())).unit("FPS");
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
