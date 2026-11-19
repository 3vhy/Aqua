package dev.aqua.client.hud.widgets;

import dev.aqua.client.Aqua;
import dev.aqua.client.hud.Anchor;
import dev.aqua.client.hud.HudWidget;
import dev.aqua.client.music.LyricLine;
import dev.aqua.client.music.MusicPlayer;
import dev.aqua.client.music.Track;
import dev.aqua.client.render.Anim;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.Icon;
import dev.aqua.client.render.R2D;
import dev.aqua.client.render.font.AquaFont;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.theme.Theme;
import dev.aqua.client.util.Session;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.util.List;

/** Now-playing card: cover, title, artist, equaliser, progress and three lyric lines (R3 0:02). */
public final class MusicWidget extends HudWidget {
    private final BoolSetting lyrics = add(new BoolSetting("lyrics", "Show lyrics", true));
    private final BoolSetting hideWhenIdle = add(new BoolSetting("hide_idle", "Hide when nothing plays", true));
    private final Anim lyricScroll = new Anim(0f, 260f);
    private int lastLyric = -2;

    private static final float W = 150;

    public MusicWidget() {
        super("music_card", "Music", "Now playing with progress and synced lyrics.", Icon.NOTE, false, Anchor.BOTTOM_RIGHT, -6, -6,
                "music", "song", "lyrics", "now playing", "spotify");
    }

    @Override
    public boolean hasContent() {
        MusicPlayer.State st = MusicPlayer.get().state();
        return !hideWhenIdle.on() || st == MusicPlayer.State.PLAYING || st == MusicPlayer.State.PAUSED;
    }

    private boolean showLyrics(Track t) {
        return lyrics.on() && t != null && !t.lyrics().isEmpty();
    }

    @Override
    public float width(boolean preview) {
        return W;
    }

    @Override
    public float height(boolean preview) {
        Track t = MusicPlayer.get().current();
        return 40 + ((showLyrics(t) || (preview && t == null && lyrics.on())) ? 30 : 0);
    }

    @Override
    public void draw(DrawContext ctx, boolean preview, float alpha) {
        Theme th = Aqua.theme();
        MusicPlayer mp = MusicPlayer.get();
        Track t = mp.current();
        float h = height(preview);
        if (showBackground()) {
            R2D.round(ctx, 0, 0, W, h, 6, bg(alpha));
        }
        // Cover art (or a themed placeholder)
        Identifier cover = mp.cover();
        if (cover != null) {
            R2D.texture(ctx, cover, 6, 6, 22, 22, Colors.withAlpha(0xFFFFFF, Math.round(alpha * 255)));
        } else {
            R2D.roundGradientV(ctx, 6, 6, 22, 22, 4, Colors.fade(th.accent(), alpha * 0.85f), Colors.fade(th.elevated(), alpha));
            R2D.icon(ctx, Icon.NOTE, 11, 11, 12, Colors.fade(th.text(), alpha));
        }
        String title;
        String artist;
        if (t == null) {
            title = preview ? "Sample Track" : "Nothing playing";
            artist = preview ? "Sample Artist" : (mp.library().isEmpty() ? "Add .ogg files to config/aqua/music" : "Press play in Aqua > Music");
        } else {
            title = t.title();
            artist = t.artist().isEmpty() ? "Unknown artist" : t.artist();
        }
        R2D.text(ctx, AquaFont.ellipsize(title, W - 52, 6.2f, AquaFont.Weight.SEMIBOLD), 33, 8, 6.2f, AquaFont.Weight.SEMIBOLD,
                Colors.fade(th.text(), alpha));
        R2D.text(ctx, AquaFont.ellipsize(artist, W - 52, 5f, AquaFont.Weight.MEDIUM), 33, 17.5f, 5f, AquaFont.Weight.MEDIUM,
                Colors.fade(th.muted(), alpha));
        // Equaliser bars
        float time = (System.currentTimeMillis() % 100000L) / 1000f;
        for (int i = 0; i < 4; i++) {
            float bh = mp.isPlaying() ? 2f + 5f * (0.5f + 0.5f * (float) Math.sin(time * (5f + i * 1.9f) + i)) : 2f;
            R2D.round(ctx, W - 15 + i * 2.3f, 14 - bh, 1.3f, bh, 0.65f, Colors.fade(th.accent(), alpha));
        }
        // Progress
        long pos = t == null ? (preview ? 61000 : 0) : mp.positionMs();
        long dur = t == null ? (preview ? 215000 : 0) : t.durationMs();
        float frac = dur > 0 ? Math.min(1f, pos / (float) dur) : 0f;
        float by = 33;
        R2D.textMid(ctx, Session.format(pos), 6, by, 4.4f, AquaFont.Weight.MEDIUM, Colors.fade(th.muted(), alpha));
        String total = dur > 0 ? Session.format(dur) : "N/A";
        R2D.textRight(ctx, total, W - 6, by, 4.4f, AquaFont.Weight.MEDIUM, Colors.fade(th.muted(), alpha));
        float bx = 24;
        float bw = W - 48;
        R2D.round(ctx, bx, by - 0.75f, bw, 1.5f, 0.75f, Colors.fade(th.border(), alpha));
        R2D.round(ctx, bx, by - 0.75f, bw * frac, 1.5f, 0.75f, Colors.fade(th.accent(), alpha));

        if (showLyrics(t) || (preview && t == null && lyrics.on())) {
            List<LyricLine> lines = t != null ? t.lyrics() : List.of(new LyricLine(0, "First sample lyric line"),
                    new LyricLine(1, "Current sample lyric line"), new LyricLine(2, "Next sample lyric line"));
            int idx = t != null ? mp.lyricIndex() : 1;
            if (idx != lastLyric) {
                lyricScroll.snap(1f).target(0f);
                lastLyric = idx;
            }
            float sc = lyricScroll.get();
            R2D.pushClip(ctx, 4, 40, W - 8, 29);
            for (int k = -1; k <= 2; k++) {
                int li = idx + k;
                if (li < 0 || li >= lines.size()) {
                    continue;
                }
                float ly = 49 + (k + sc) * 9.5f;
                boolean cur = k == 0;
                String txt = AquaFont.ellipsize(lines.get(li).text(), W - 12, cur ? 5.6f : 5f, cur ? AquaFont.Weight.SEMIBOLD : AquaFont.Weight.MEDIUM);
                R2D.textMid(ctx, txt, 6, ly, cur ? 5.6f : 5f, cur ? AquaFont.Weight.SEMIBOLD : AquaFont.Weight.MEDIUM,
                        Colors.fade(cur ? th.text() : th.muted(), alpha * (cur ? 1f : 0.7f)));
            }
            R2D.popClip(ctx);
        }
    }
}
