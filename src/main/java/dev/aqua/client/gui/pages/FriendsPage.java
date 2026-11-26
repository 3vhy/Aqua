package dev.aqua.client.gui.pages;

import dev.aqua.client.Aqua;
import dev.aqua.client.gui.PageHost;
import dev.aqua.client.gui.Ui;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.Icon;
import dev.aqua.client.render.R2D;
import dev.aqua.client.render.font.AquaFont;
import dev.aqua.client.social.Friends;
import dev.aqua.client.theme.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;

import java.util.ArrayList;
import java.util.List;

/** Friends tab: add and remove friends; online players can be added with one click. */
public final class FriendsPage implements Page {
    private String input = "";

    @Override
    public float render(Ui ui, DrawContext ctx, float x, float y, float w, float a, PageHost menu) {
        Theme t = Aqua.theme();
        y = Page.heading(ctx, t, "Friends", "Friends get a green name in the tab list. The list stays on this computer.", x, y, a);
        input = ui.textField("friend_add", x, y, w - 52, 12, input, "Player name", 16, Icon.USER, s -> input = s, this::add);
        if (ui.button("friend_add_btn", x + w - 48, y, 48, 12, "Add", Icon.PLUS, true)) {
            add(input);
        }
        y += 18;
        List<String> list = Friends.all();
        if (list.isEmpty()) {
            R2D.text(ctx, "No friends yet.", x, y + 2, 5.2f, AquaFont.Weight.MEDIUM, Colors.fade(t.muted(), a));
            y += 12;
        }
        for (String name : list) {
            boolean online = online().contains(name.toLowerCase(java.util.Locale.ROOT));
            Page.card(ctx, t, x, y, w, 15, a);
            R2D.circle(ctx, x + 8, y + 7.5f, 2, Colors.fade(online ? Friends.COLOR : Colors.withAlpha(t.muted(), 0.6f), a));
            R2D.textMid(ctx, name, x + 15, y + 7.5f, 5.6f, AquaFont.Weight.SEMIBOLD, Colors.fade(t.text(), a));
            R2D.textMid(ctx, online ? "online" : "", x + 15 + R2D.width(name, 5.6f, AquaFont.Weight.SEMIBOLD) + 5, y + 7.5f, 4.6f,
                    AquaFont.Weight.MEDIUM, Colors.fade(Friends.COLOR, a));
            if (ui.iconButton("friend_rm_" + name, x + w - 15, y + 1.5f, 12, Icon.TRASH, t.muted(), "Remove")) {
                Friends.remove(name);
            }
            y += 18;
        }
        // Players on this server (or in this world) who are not friends yet.
        List<String> others = new ArrayList<>();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() != null) {
            for (PlayerListEntry e : mc.getNetworkHandler().getPlayerList()) {
                String n = e.getProfile().name();
                if (mc.player != null && n.equals(mc.player.getGameProfile().name())) {
                    continue;
                }
                if (!Friends.isFriend(n)) {
                    others.add(n);
                }
            }
        }
        if (!others.isEmpty()) {
            y += 4;
            y = Page.heading(ctx, t, "Online now", "Click a name to add it", x, y, a);
            float cx = x;
            for (String n : others) {
                float cw = R2D.width(n, 5f, AquaFont.Weight.MEDIUM) + 14;
                if (cx + cw > x + w) {
                    cx = x;
                    y += 14;
                }
                if (ui.button("friend_q_" + n, cx, y, cw, 11, n, null, false)) {
                    Friends.add(n);
                }
                cx += cw + 4;
            }
            y += 14;
        }
        return y;
    }

    private void add(String s) {
        if (Friends.add(s)) {
            input = "";
        } else if (!s.isBlank()) {
            Aqua.notifications().warn("Friends", Friends.isFriend(s) ? s + " is already a friend" : "That is not a valid player name");
        }
    }

    private static List<String> online() {
        List<String> l = new ArrayList<>();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() != null) {
            for (PlayerListEntry e : mc.getNetworkHandler().getPlayerList()) {
                l.add(e.getProfile().name().toLowerCase(java.util.Locale.ROOT));
            }
        }
        return l;
    }
}
