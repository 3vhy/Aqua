package dev.aqua.client.hud.widgets;

import dev.aqua.client.Aqua;
import dev.aqua.client.hud.Anchor;
import dev.aqua.client.hud.HudWidget;
import dev.aqua.client.hud.Pill;
import dev.aqua.client.module.qol.StreamerMode;
import dev.aqua.client.render.Icon;
import net.minecraft.client.gui.DrawContext;

/** Local player name capsule (Fern R1/R2 account pill, without the account-service UID). */
public final class AccountWidget extends HudWidget {
    public AccountWidget() {
        super("account", "Account", "Shows your player name.", Icon.USER, true, Anchor.TOP_LEFT, 64, 5, "username", "name");
    }

    private Pill pill() {
        String name = mc.getSession() != null ? mc.getSession().getUsername() : "Player";
        StreamerMode sm = Aqua.modules().get(StreamerMode.class);
        if (sm != null && sm.isEnabled()) {
            name = sm.alias();
        }
        return Pill.create().icon(Icon.USER).value(name);
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
