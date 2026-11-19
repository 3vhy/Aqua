package dev.aqua.client.hud.widgets;

import dev.aqua.client.hud.Anchor;
import dev.aqua.client.hud.HudWidget;
import dev.aqua.client.hud.Pill;
import dev.aqua.client.render.Icon;
import dev.aqua.client.util.CpsTracker;
import net.minecraft.client.gui.DrawContext;

/** Left | right clicks per second from real input only (baseline; R4 0:44 "CPS 0"). */
public final class CpsWidget extends HudWidget {
    public CpsWidget() {
        super("cps", "CPS", "Your left and right clicks per second.", Icon.MOUSE, false, Anchor.TOP_LEFT, 5, 37,
                "clicks", "cps", "mouse");
    }

    private Pill pill() {
        return Pill.create().icon(Icon.MOUSE).value(String.valueOf(CpsTracker.left())).muted("|")
                .value(String.valueOf(CpsTracker.right())).unit("CPS");
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
