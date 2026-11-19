package dev.aqua.client.hud.widgets;

import dev.aqua.client.Aqua;
import dev.aqua.client.hud.Anchor;
import dev.aqua.client.hud.HudWidget;
import dev.aqua.client.hud.Pill;
import dev.aqua.client.music.MusicPlayer;
import dev.aqua.client.music.Track;
import dev.aqua.client.notify.Notification;
import dev.aqua.client.notify.Notifications;
import dev.aqua.client.render.Anim;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.Icon;
import dev.aqua.client.render.R2D;
import dev.aqua.client.render.font.AquaFont;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.theme.Theme;
import net.minecraft.client.gui.DrawContext;

/**
 * Top-centre capsule that morphs between the brand (idle), the playing track and toggle
 * notifications - the "Fern" / "trick - acoustic" / "FreeCam enabled" capsule of R1/R2.
 */
public final class IslandWidget extends HudWidget {
    private final BoolSetting showMusic = add(new BoolSetting("music", "Show playing music", true));
    private final BoolSetting showIdle = add(new BoolSetting("idle", "Show brand when idle", true));
    private final Anim widthAnim = new Anim(60f, 220f);
    private final Anim swap = new Anim(1f, 180f);
    private Object lastContent;

    public IslandWidget() {
        super("island", "Island", "Brand, now playing and notifications in one capsule.", Icon.BELL, true, Anchor.TOP_CENTER, 0, 4,
                "notifications", "dynamic island", "now playing");
    }

    private enum Mode { IDLE, MUSIC, NOTIFY }

    private Mode mode() {
        Notification n = Aqua.notifications().current();
        if (n != null) {
            return Mode.NOTIFY;
        }
        if (showMusic.on() && MusicPlayer.get().current() != null && MusicPlayer.get().isPlaying()) {
            return Mode.MUSIC;
        }
        return Mode.IDLE;
    }

    private String trackLabel(Track t) {
        return t.artist().isEmpty() ? t.title() : t.title() + " - " + t.artist();
    }

    private float contentWidth(Mode m) {
        return switch (m) {
            case NOTIFY -> {
                Notification n = Aqua.notifications().current();
                yield 5 + 4 + AquaFont.width(n.title + " " + n.message, Pill.VALUE, AquaFont.Weight.SEMIBOLD) + Pill.PAD_X * 2 + 2;
            }
            case MUSIC -> Pill.ICON + 4 + AquaFont.width(AquaFont.ellipsize(trackLabel(MusicPlayer.get().current()), 120,
                    Pill.VALUE, AquaFont.Weight.SEMIBOLD), Pill.VALUE, AquaFont.Weight.SEMIBOLD) + 4 + 8 + Pill.PAD_X * 2;
            case IDLE -> Pill.create().logo(WatermarkWidget.EMBLEM).value(Aqua.NAME).width();
        };
    }

    @Override
    public boolean hasContent() {
        return mode() != Mode.IDLE || showIdle.on();
    }

    @Override
    public float width(boolean preview) {
        return Math.max(30, widthAnim.target(contentWidth(mode())).get());
    }

    @Override
    public float height(boolean preview) {
        return Pill.HEIGHT;
    }

    @Override
    public void draw(DrawContext ctx, boolean preview, float alpha) {
        Theme t = Aqua.theme();
        Mode m = mode();
        Object content = m == Mode.NOTIFY ? Aqua.notifications().current() : m;
        if (content != lastContent) {
            lastContent = content;
            swap.snap(0f).target(1f);
        }
        float w = width(preview);
        if (showBackground()) {
            R2D.round(ctx, 0, 0, w, Pill.HEIGHT, Pill.HEIGHT / 2f, bg(alpha));
        }
        float ca = alpha * Anim.easeOutCubic(swap.get());
        float cw = contentWidth(m);
        float x = (w - cw) / 2f + Pill.PAD_X;
        float cy = Pill.HEIGHT / 2f;
        R2D.pushClip(ctx, 0, 0, w, Pill.HEIGHT);
        switch (m) {
            case NOTIFY -> {
                Notification n = (Notification) content;
                int dot = Notifications.dotColor(n, t);
                R2D.circle(ctx, x + 2.5f, cy, 2.5f, Colors.fade(dot, ca));
                float tx = x + 9;
                tx += R2D.textMid(ctx, n.title, tx, cy, Pill.VALUE, AquaFont.Weight.SEMIBOLD, Colors.fade(t.text(), ca));
                tx += AquaFont.width(" ", Pill.VALUE, AquaFont.Weight.SEMIBOLD);
                R2D.textMid(ctx, n.message, tx, cy, Pill.VALUE, AquaFont.Weight.SEMIBOLD, Colors.fade(t.text(), ca * 0.85f));
            }
            case MUSIC -> {
                Track tr = MusicPlayer.get().current();
                R2D.icon(ctx, Icon.DISC, x, cy - Pill.ICON / 2f, Pill.ICON, Colors.fade(t.text(), ca));
                float tx = x + Pill.ICON + 4;
                String label = AquaFont.ellipsize(trackLabel(tr), 120, Pill.VALUE, AquaFont.Weight.SEMIBOLD);
                tx += R2D.textMid(ctx, label, tx, cy, Pill.VALUE, AquaFont.Weight.SEMIBOLD, Colors.fade(t.text(), ca));
                tx += 4;
                float time = (System.currentTimeMillis() % 100000L) / 1000f;
                for (int i = 0; i < 4; i++) {
                    float h = 2f + 4f * (0.5f + 0.5f * (float) Math.sin(time * (5.5f + i * 1.7f) + i * 1.3f));
                    if (!MusicPlayer.get().isPlaying()) {
                        h = 2f;
                    }
                    R2D.round(ctx, tx + i * 2.1f, cy + 3.5f - h, 1.2f, h, 0.6f, Colors.fade(t.text(), ca));
                }
            }
            case IDLE -> Pill.create().logo(WatermarkWidget.EMBLEM).value(Aqua.NAME).draw(ctx, (w - cw) / 2f, 0, false, 0, ca);
        }
        R2D.popClip(ctx);
    }
}
