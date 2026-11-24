package dev.aqua.client.hud.widgets;

import dev.aqua.client.Aqua;
import dev.aqua.client.hud.Anchor;
import dev.aqua.client.hud.HudWidget;
import dev.aqua.client.hud.Panel;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.Icon;
import dev.aqua.client.render.R2D;
import dev.aqua.client.render.font.AquaFont;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.StringSetting;
import dev.aqua.client.theme.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Staff List (R2 0:01, R7 "HUD ... staff list"): online players whose rank in the tab list is a staff
 * rank. It reads only what the tab list already shows everyone (display names and team prefixes); it
 * cannot see staff who are hidden or vanished.
 */
public final class StaffListWidget extends HudWidget {
    private final StringSetting ranks = add(new StringSetting("ranks", "Staff ranks",
            "owner, admin, manager, developer, dev, mod, moderator, helper, staff, builder, sr mod, jr mod, trial", 200)
            .describe("Comma-separated words; a player whose tab-list rank contains one of them is listed"));
    private final BoolSetting showRank = add(new BoolSetting("show_rank", "Show rank", true));
    private final BoolSetting hideEmpty = add(new BoolSetting("hide_empty", "Hide when no staff are online", false));

    private record Staff(String name, String rank, int color) {}

    private List<Staff> cache = List.of();
    private long cacheAt;

    public StaffListWidget() {
        super("staff_list", "Staff List", "Staff online on this server, read from the tab list.", Icon.SHIELD, false, Anchor.TOP_LEFT, 5, 120,
                "staff", "staff list", "mods", "admins", "moderators");
    }

    private List<String> keywords() {
        List<String> out = new ArrayList<>();
        for (String s : ranks.get().split(",")) {
            String k = s.trim().toLowerCase(Locale.ROOT);
            if (!k.isEmpty()) {
                out.add(k);
            }
        }
        return out;
    }

    /** Words inside brackets or before the name ("[Mod] Steve", "ADMIN Steve", "✦ Helper | Steve"). */
    static String rankOf(String display, String name) {
        String s = display.replace('§', ' ');
        int at = s.indexOf(name);
        String before = at > 0 ? s.substring(0, at) : "";
        String after = at >= 0 ? s.substring(at + name.length()) : "";
        String rank = (before + " " + after).replaceAll("[\\[\\]()|:<>{}✦★•*]", " ").replaceAll("\\s+", " ").trim();
        return rank;
    }

    static boolean matches(String rank, List<String> keys) {
        String r = " " + rank.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim() + " ";
        for (String k : keys) {
            if (r.contains(" " + k + " ")) {
                return true;
            }
        }
        return false;
    }

    private List<Staff> scan() {
        long now = System.currentTimeMillis();
        if (now - cacheAt < 500) {
            return cache;
        }
        cacheAt = now;
        ClientPlayNetworkHandler nh = mc.getNetworkHandler();
        if (nh == null) {
            cache = List.of();
            return cache;
        }
        List<String> keys = keywords();
        List<Staff> out = new ArrayList<>();
        for (PlayerListEntry e : nh.getListedPlayerListEntries()) {
            String name = e.getProfile().name();
            Text shown = e.getDisplayName();
            String display;
            if (shown != null) {
                display = shown.getString();
            } else {
                Team team = e.getScoreboardTeam();
                display = team != null ? team.getPrefix().getString() + name + team.getSuffix().getString() : name;
            }
            String rank = rankOf(display, name);
            if (!rank.isEmpty() && matches(rank, keys)) {
                Integer rgb = null;
                if (shown != null && shown.getStyle().getColor() != null) {
                    rgb = shown.getStyle().getColor().getRgb();
                } else if (e.getScoreboardTeam() != null && e.getScoreboardTeam().getColor().getColorValue() != null) {
                    rgb = e.getScoreboardTeam().getColor().getColorValue();
                }
                out.add(new Staff(name, rank.length() > 18 ? rank.substring(0, 18) : rank, rgb == null ? 0 : 0xFF000000 | rgb));
            }
        }
        out.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
        cache = out;
        return out;
    }

    private List<Staff> rows(boolean preview) {
        List<Staff> s = scan();
        if (s.isEmpty() && preview) {
            return List.of(new Staff("SampleMod", "Mod", 0xFF55FF55), new Staff("SampleAdmin", "Admin", 0xFFFF5555));
        }
        return s;
    }

    @Override
    public boolean hasContent() {
        return !hideEmpty.on() || !scan().isEmpty();
    }

    @Override
    public float width(boolean preview) {
        float w = AquaFont.width("Staff", Panel.TITLE, AquaFont.Weight.SEMIBOLD) + 40;
        for (Staff s : rows(preview)) {
            float rw = showRank.on() ? AquaFont.width(s.rank, Panel.TEXT, AquaFont.Weight.MEDIUM) + 8 : 0;
            w = Math.max(w, AquaFont.width(s.name, Panel.TEXT, AquaFont.Weight.MEDIUM) + rw + Panel.PAD * 2 + 6);
        }
        return Math.max(92, w);
    }

    @Override
    public float height(boolean preview) {
        List<Staff> r = rows(preview);
        return Panel.height(r.isEmpty() ? 1 : r.size());
    }

    @Override
    public void draw(DrawContext ctx, boolean preview, float alpha) {
        Theme t = Aqua.theme();
        List<Staff> r = rows(preview);
        boolean sample = preview && scan().isEmpty();
        float w = width(preview);
        Panel.header(ctx, w, sample ? "Staff  SAMPLE" : "Staff  " + r.size(), Icon.SHIELD, showBackground(), bg(alpha), alpha,
                r.isEmpty() ? 1 : r.size());
        if (r.isEmpty()) {
            R2D.textMid(ctx, "No staff in the tab list", Panel.PAD, Panel.rowY(0), Panel.TEXT, AquaFont.Weight.MEDIUM, Colors.fade(t.muted(), alpha));
            return;
        }
        for (int i = 0; i < r.size(); i++) {
            Staff s = r.get(i);
            float y = Panel.rowY(i);
            R2D.circle(ctx, Panel.PAD + 1.5f, y, 1.6f, Colors.fade(s.color != 0 ? s.color : t.accent(), alpha));
            R2D.textMid(ctx, s.name, Panel.PAD + 6, y, Panel.TEXT, AquaFont.Weight.MEDIUM, Colors.fade(t.text(), alpha));
            if (showRank.on()) {
                R2D.textRight(ctx, s.rank, w - Panel.PAD, y, Panel.TEXT, AquaFont.Weight.MEDIUM,
                        Colors.fade(s.color != 0 ? s.color : t.muted(), alpha * 0.95f));
            }
        }
    }
}
