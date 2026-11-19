package dev.aqua.client.hud.widgets;

import dev.aqua.client.Aqua;
import dev.aqua.client.hud.Anchor;
import dev.aqua.client.hud.HudWidget;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.Icon;
import dev.aqua.client.render.R2D;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.theme.Theme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Restyled sidebar using the server's own scoreboard data (baseline "scoreboard styling").
 * Text keeps the server's formatting and is drawn with the vanilla text renderer.
 */
public final class ScoreboardWidget extends HudWidget {
    private final BoolSetting numbers = add(new BoolSetting("numbers", "Show score numbers", true));
    private final BoolSetting accentTitle = add(new BoolSetting("accent_title", "Accent title bar", true));
    private final BoolSetting textShadow = add(new BoolSetting("shadow", "Text shadow", false));

    private record Row(Text name, Text score, int scoreWidth) {}

    private static final Comparator<ScoreboardEntry> ORDER = Comparator.comparing(ScoreboardEntry::value)
            .reversed().thenComparing(ScoreboardEntry::owner, String.CASE_INSENSITIVE_ORDER);

    public ScoreboardWidget() {
        super("scoreboard", "Scoreboard", "Restyles the server sidebar scoreboard.", Icon.LIST, true, Anchor.MIDDLE_RIGHT, -4, 0,
                "sidebar", "score", "objective");
    }

    private ScoreboardObjective objective() {
        if (mc.world == null || mc.player == null) {
            return null;
        }
        Scoreboard sb = mc.world.getScoreboard();
        Team team = sb.getScoreHolderTeam(mc.player.getNameForScoreboard());
        ScoreboardObjective obj = null;
        if (team != null) {
            ScoreboardDisplaySlot slot = ScoreboardDisplaySlot.fromFormatting(team.getColor());
            if (slot != null) {
                obj = sb.getObjectiveForSlot(slot);
            }
        }
        return obj != null ? obj : sb.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
    }

    private List<Row> rows(ScoreboardObjective obj, TextRenderer tr) {
        List<Row> out = new ArrayList<>();
        Scoreboard sb = obj.getScoreboard();
        NumberFormat fmt = obj.getNumberFormatOr(StyledNumberFormat.RED);
        sb.getScoreboardEntries(obj).stream().filter(e -> !e.hidden()).sorted(ORDER).limit(15).forEach(e -> {
            Team team = sb.getScoreHolderTeam(e.owner());
            Text name = Team.decorateName(team, e.name());
            Text score = e.formatted(fmt);
            out.add(new Row(name, score, tr.getWidth(score)));
        });
        return out;
    }

    @Override
    public boolean hasContent() {
        return objective() != null;
    }

    @Override
    public float width(boolean preview) {
        TextRenderer tr = mc.textRenderer;
        ScoreboardObjective obj = objective();
        if (obj == null) {
            return 100;
        }
        int w = tr.getWidth(obj.getDisplayName());
        for (Row r : rows(obj, tr)) {
            w = Math.max(w, tr.getWidth(r.name) + (numbers.on() ? r.scoreWidth + 8 : 0));
        }
        return w + 12;
    }

    @Override
    public float height(boolean preview) {
        ScoreboardObjective obj = objective();
        if (obj == null) {
            return 60;
        }
        return 14 + rows(obj, mc.textRenderer).size() * 9.5f + 4;
    }

    @Override
    public void draw(DrawContext ctx, boolean preview, float alpha) {
        Theme t = Aqua.theme();
        TextRenderer tr = mc.textRenderer;
        ScoreboardObjective obj = objective();
        float w = width(preview);
        float h = height(preview);
        if (showBackground()) {
            R2D.round(ctx, 0, 0, w, h, 5, bg(alpha));
        }
        int a = Math.round(alpha * 255);
        if (obj == null) {
            R2D.textCentered(ctx, "Server scoreboard", w / 2f, 10, 6f, dev.aqua.client.render.font.AquaFont.Weight.SEMIBOLD,
                    Colors.fade(t.text(), alpha));
            R2D.textCentered(ctx, "appears here", w / 2f, 20, 5f, dev.aqua.client.render.font.AquaFont.Weight.MEDIUM,
                    Colors.fade(t.muted(), alpha));
            return;
        }
        if (accentTitle.on()) {
            R2D.roundCorners(ctx, 0, 0, w, 13, 5, R2D.TOP, Colors.fade(Colors.withAlpha(t.accent(), 0.22f), alpha));
        }
        Text title = obj.getDisplayName();
        ctx.drawText(tr, title, Math.round((w - tr.getWidth(title)) / 2f), 3, Colors.withAlpha(0xFFFFFF, a), textShadow.on());
        List<Row> r = rows(obj, tr);
        for (int i = 0; i < r.size(); i++) {
            Row row = r.get(i);
            int y = 16 + Math.round(i * 9.5f);
            ctx.drawText(tr, row.name, 6, y, Colors.withAlpha(0xFFFFFF, a), textShadow.on());
            if (numbers.on()) {
                ctx.drawText(tr, row.score, Math.round(w - 6 - row.scoreWidth), y, Colors.withAlpha(Formatting.RED.getColorValue(), a),
                        textShadow.on());
            }
        }
    }
}
