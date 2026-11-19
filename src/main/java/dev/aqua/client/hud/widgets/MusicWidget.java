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
    public enum Style implements dev.aqua.client.setting.EnumSetting.Displayable {
        CARD("Card"), COMPACT("Compact (standalone)");
        final String l;

        Style(String l) {
            this.l = l;
        }

        public String displayName() {
            return l;
        }
    }

    private final dev.aqua.client.setting.EnumSetting<Style> style = add(new dev.aqua.client.setting.EnumSetting<>("style", "Style", Style.CARD));
    private final BoolSetting lyrics = add(new BoolSetting("lyrics", "Show lyrics on the card", true));
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

    private boolean lyricsAllowed() {
        dev.aqua.client.music.MusicModule m = Aqua.modules().get(dev.aqua.client.music.MusicModule.class);
        return lyrics.on() && style.is(Style.CARD) && (m == null || m.lyricsEnabled());
    }

    private boolean showLyrics(Track t) {
        return lyricsAllowed() && t != null && !MusicPlayer.get().lyricsFor(t).isEmpty();
    }

    @Override
    public float width(boolean preview) {
        return style.is(Style.COMPACT) ? 120 : W;
    }

    @Override
    public float height(boolean preview) {
        if (style.is(Style.COMPACT)) {
            return 22;
        }
        Track t = MusicPlayer.get().current();
        return 40 + ((showLyrics(t) || (preview && t == null && lyricsAllowed())) ? 30 : 0);
    }

    private void drawCompact(DrawContext ctx, boolean preview, float alpha) {
        Theme th = Aqua.theme();
        MusicPlayer mp = MusicPlayer.get();
        Track t = mp.current();
        float w = width(preview);
        if (showBackground()) {
            R2D.round(ctx, 0, 0, w, 22, 6, bg(alpha));
        }
        Identifier cover = mp.cover();
        if (cover != null) {
            R2D.texture(ctx, cover, 4, 4, 14, 14, Colors.withAlpha(0xFFFFFF, Math.round(alpha * 255)));
        } else {
            R2D.roundGradientV(ctx, 4, 4, 14, 14, 3, Colors.fade(th.accent(), alpha * 0.85f), Colors.fade(th.elevated(), alpha));
            R2D.icon(ctx, Icon.NOTE, 7, 7, 8, Colors.fade(th.text(), alpha));
        }
        String title = t != null ? t.title() : (preview ? "Sample Track" : "Nothing playing");
        String artist = t != null ? (t.artist().isEmpty() ? "Unknown artist" : t.artist()) : (preview ? "Sample Artist" : "Aqua Music");
        R2D.text(ctx, AquaFont.ellipsize(title, w - 40, 5.6f, AquaFont.Weight.SEMIBOLD), 22, 4.5f, 5.6f, AquaFont.Weight.SEMIBOLD, Colors.fade(th.text(), alpha));
        R2D.text(ctx, AquaFont.ellipsize(artist, w - 40, 4.6f, AquaFont.Weight.MEDIUM), 22, 12f, 4.6f, AquaFont.Weight.MEDIUM, Colors.fade(th.muted(), alpha));
        float time = (System.currentTimeMillis() % 100000L) / 1000f;
        for (int i = 0; i < 3; i++) {
            float bh = mp.isPlaying() ? 2f + 4f * (0.5f + 0.5f * (float) Math.sin(time * (5f + i * 1.9f) + i)) : 2f;
            R2D.round(ctx, w - 11 + i * 2.3f, 13 - bh, 1.3f, bh, 0.65f, Colors.fade(th.accent(), alpha));
        }
        long dur = t != null ? t.durationMs() : 0;
        float frac = dur > 0 ? Math.min(1f, mp.positionMs() / (float) dur) : (preview ? 0.3f : 0f);
        R2D.round(ctx, 4, 19.5f, w - 8, 1f, 0.5f, Colors.fade(th.border(), alpha));
        R2D.round(ctx, 4, 19.5f, (w - 8) * frac, 1f, 0.5f, Colors.fade(th.accent(), alpha));
    }

    @Override
    public void draw(DrawContext ctx, boolean preview, float alpha) {
        if (style.is(Style.COMPACT)) {
            drawCompact(ctx, preview, alpha);
            return;
        }
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
            artist = preview ? "Sample Artist" : "Play something in Spotify or Aqua > Music";
        } else {
            title = t.title();
            artist = t.artist().isEmpty() ? "Unknown artist" : t.artist();
            if (mp.sourceApp() != null) {
                artist += "  \u00b7  " + mp.sourceApp();
            }
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

        if (showLyrics(t) || (preview && t == null && lyricsAllowed())) {
            List<LyricLine> lines = t != null ? mp.lyricsFor(t) : List.of(new LyricLine(0, "First sample lyric line"),
                    new LyricLine(1, "Current sample lyric line"), new LyricLine(2, "Next sample lyric line"));
            int idx = t != null ? mp.lyricIndex() : 1;
            if (idx != lastLyric) {
                lyricScroll.snap(1f).target(0f);
                lastLyric = idx;
            }
            float sc = lyricScroll.get();
            R2D.pushClip(ctx, 4, 40, W - 8, 29);
            for (int k = -2; k <= 2; k++) {
                int li = idx + k;
                if (li < 0 || li >= lines.size()) {
                    continue;
                }
                float rel = k + sc;
                // Previous, current and next line fully inside the card; lines scrolling in or out fade.
                float vis = Math.max(0f, Math.min(1f, 2f - Math.abs(rel)));
                if (vis <= 0.01f) {
                    continue;
                }
                float ly = 54.5f + rel * 9.2f;
                boolean cur = k == 0;
                String txt = AquaFont.ellipsize(lines.get(li).text(), W - 12, cur ? 5.6f : 5f, cur ? AquaFont.Weight.SEMIBOLD : AquaFont.Weight.MEDIUM);
                R2D.textMid(ctx, txt, 6, ly, cur ? 5.6f : 5f, cur ? AquaFont.Weight.SEMIBOLD : AquaFont.Weight.MEDIUM,
                        Colors.fade(cur ? th.text() : th.muted(), alpha * vis * (cur ? 1f : 0.7f)));
            }
            R2D.popClip(ctx);
        }
    }
}
