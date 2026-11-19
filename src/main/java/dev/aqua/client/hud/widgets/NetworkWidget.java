package dev.aqua.client.hud.widgets;

import dev.aqua.client.hud.Anchor;
import dev.aqua.client.hud.HudWidget;
import dev.aqua.client.hud.Pill;
import dev.aqua.client.render.Icon;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.util.TextUtil;
import dev.aqua.client.util.TpsTracker;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;

/** "2 ms • 19.9 TPS" (Fern R1 0:00). Values that are not available show N/A. */
public final class NetworkWidget extends HudWidget {
    private final BoolSetting ping = add(new BoolSetting("ping", "Ping", true));
    private final BoolSetting tps = add(new BoolSetting("tps", "TPS", true).describe("Exact in singleplayer, estimated (~) on servers"));

    public NetworkWidget() {
        super("network", "Network", "Your ping and the server tick rate.", Icon.WIFI, true, Anchor.TOP_LEFT, 160, 21,
                "ping", "latency", "tps", "tick");
    }

    private Pill pill(boolean preview) {
        Pill p = Pill.create().icon(Icon.WIFI);
        boolean first = true;
        if (ping.on()) {
            String v = "N/A";
            if (mc.player != null && mc.getNetworkHandler() != null) {
                PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                if (e != null) {
                    v = String.valueOf(e.getLatency());
                }
            } else if (preview) {
                v = "24";
            }
            p.value(v);
            if (!"N/A".equals(v)) {
                p.unit("ms");
            }
            first = false;
        }
        if (tps.on()) {
            if (!first) {
                p.dot();
            }
            double t = mc.world != null ? TpsTracker.tps() : (preview ? 20.0 : -1);
            if (t < 0) {
                p.value("N/A");
            } else {
                p.value((TpsTracker.exact() || mc.world == null ? "" : "~") + TextUtil.fmt1(t)).unit("TPS");
            }
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
