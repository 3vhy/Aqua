package dev.aqua.client.gui.pages;

import dev.aqua.client.Aqua;
import dev.aqua.client.gui.PageHost;
import dev.aqua.client.gui.Ui;
import dev.aqua.client.gui.UiSounds;
import dev.aqua.client.music.LyricLine;
import dev.aqua.client.music.MusicModule;
import dev.aqua.client.music.MusicPlayer;
import dev.aqua.client.music.Track;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.Glass;
import dev.aqua.client.render.Icon;
import dev.aqua.client.render.R2D;
import dev.aqua.client.render.font.AquaFont;
import dev.aqua.client.theme.Theme;
import dev.aqua.client.util.Session;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.List;

/** Standalone music player: now playing, seek bar, controls, lyrics switch and your library. */
public final class MusicPage implements Page {
    @Override
    public float render(Ui ui, DrawContext ctx, float x, float y, float w, float a, PageHost menu) {
        Theme t = Aqua.theme();
        MusicPlayer mp = MusicPlayer.get();
        MusicModule mod = Aqua.modules().get(MusicModule.class);
        if (!mod.isEnabled()) {
            Page.card(ctx, t, x, y, w, 30, a);
            Page.heading(ctx, t, "Music Player is off", "Turn it on to play your music", x + 9, y + 5, a);
            boolean on = ui.toggle("mp_on", x + w - 26, y + 10, false);
            if (on) {
                mod.setEnabled(true, true);
            }
            return y + 34;
        }
        Track tr = mp.current();
        // ---------------------------------------------------------- now playing
        float ch = 78;
        Page.card(ctx, t, x, y, w, ch, a);
        float cs = 56;
        Identifier cover = mp.cover();
        if (cover != null) {
            R2D.texture(ctx, cover, x + 9, y + 9, cs, cs, Colors.withAlpha(0xFFFFFF, Math.round(a * 255)));
        } else {
            R2D.roundGradientV(ctx, x + 9, y + 9, cs, cs, 6, Colors.fade(t.accent(), a), Colors.fade(t.elevated(), a));
            R2D.icon(ctx, Icon.NOTE, x + 9 + cs / 2f - 10, y + 9 + cs / 2f - 10, 20, Colors.fade(t.text(), a));
        }
        float tx = x + 9 + cs + 10;
        float tw = w - (tx - x) - 9;
        String app = mp.sourceApp();
        String title = tr != null ? tr.title() : (mp.library().isEmpty() ? "No music yet" : "Pick a song below");
        String artist = tr != null ? (tr.artist().isEmpty() ? "Unknown artist" : tr.artist()) + (app != null ? "  \u00b7  " + app : "")
                : (mp.library().isEmpty() ? "Play something in Spotify, or add .ogg files to config/aqua/music"
                : mp.library().size() + " songs in your library");
        // Spotify switch, top right of the card
        float swx = x + w - 9 - 17;
        R2D.textRight(ctx, "Spotify", swx - 4, y + 13.5f, 5f, AquaFont.Weight.MEDIUM, Colors.fade(t.muted(), a));
        boolean ext = ui.toggle("mp_external", swx, y + 9, mod.externalSetting().on());
        if (ext != mod.externalSetting().on()) {
            mod.externalSetting().set(ext);
        }
        float titleW = Math.min(tw, swx - 32 - tx);
        R2D.text(ctx, AquaFont.ellipsize(title, titleW, 8f, AquaFont.Weight.SEMIBOLD), tx, y + 9, 8f, AquaFont.Weight.SEMIBOLD, Colors.fade(t.text(), a));
        R2D.text(ctx, AquaFont.ellipsize(artist, tw, 5.4f, AquaFont.Weight.MEDIUM), tx, y + 20, 5.4f, AquaFont.Weight.MEDIUM, Colors.fade(t.muted(), a));
        String status = mp.lyricStatus();
        if (tr == null && mod.externalEnabled()) {
            status = dev.aqua.client.music.external.ExternalMedia.get().status();
        }
        if (!status.isEmpty()) {
            R2D.icon(ctx, Icon.NOTE, tx, y + 29.5f, 5, Colors.fade(t.accent(), a));
            R2D.text(ctx, AquaFont.ellipsize(status, tw - 7, 4.6f, AquaFont.Weight.MEDIUM), tx + 7, y + 29.5f, 4.6f, AquaFont.Weight.MEDIUM,
                    Colors.fade(t.muted(), a));
        }
        // seek bar
        long pos = mp.positionMs();
        long dur = tr != null ? tr.durationMs() : 0;
        float by = y + 42;
        R2D.textMid(ctx, Session.format(tr != null ? pos : 0), tx, by, 4.6f, AquaFont.Weight.MEDIUM, Colors.fade(t.muted(), a));
        R2D.textRight(ctx, dur > 0 ? Session.format(dur) : "--:--", x + w - 9, by, 4.6f, AquaFont.Weight.MEDIUM, Colors.fade(t.muted(), a));
        float bx = tx + 20, bw = (x + w - 9) - 22 - bx;
        boolean hovBar = ui.hover(bx, by - 4, bw, 8);
        float frac = dur > 0 ? Math.min(1f, pos / (float) dur) : 0f;
        R2D.round(ctx, bx, by - 1, bw, 2, 1, Colors.fade(t.border(), a));
        R2D.round(ctx, bx, by - 1, bw * frac, 2, 1, Colors.fade(t.accent(), a));
        if (hovBar && dur > 0) {
            R2D.circle(ctx, bx + bw * frac, by, 3, Colors.fade(0xFFFFFFFF, a));
            ui.tooltip("Click to seek");
        }
        if (dur > 0 && ui.clickedIn(bx, by - 4, bw, 8)) {
            mp.seek((long) (Math.max(0, Math.min(1, (ui.mx - bx) / bw)) * dur));
            UiSounds.click(1f);
        }
        // controls
        float cy = y + 52;
        if (ui.iconButton("mp_prev", tx, cy, 16, Icon.PREV, t.text(), "Previous")) {
            mp.previous();
        }
        R2D.circle(ctx, tx + 28, cy + 8, 9, Colors.fade(t.accent(), a));
        if (ui.iconButton("mp_play", tx + 20, cy, 16, mp.isPlaying() ? Icon.PAUSE : Icon.PLAY, t.onAccent(), mp.isPlaying() ? "Pause" : "Play")) {
            mp.togglePause();
        }
        if (ui.iconButton("mp_next", tx + 40, cy, 16, Icon.NEXT, t.text(), "Next")) {
            mp.next();
        }
        float ox = tx + 64;
        R2D.textMid(ctx, "Shuffle", ox, cy + 8, 5f, AquaFont.Weight.MEDIUM, Colors.fade(t.muted(), a));
        boolean sh = ui.toggle("mp_shuffle", ox + 24, cy + 3.25f, mod.shuffleSetting().on());
        if (sh != mod.shuffleSetting().on()) {
            mod.shuffleSetting().set(sh);
        }
        ox += 48;
        R2D.textMid(ctx, "Lyrics", ox, cy + 8, 5f, AquaFont.Weight.MEDIUM, Colors.fade(t.muted(), a));
        boolean ly = ui.toggle("mp_lyrics", ox + 21, cy + 3.25f, mod.lyricsSetting().on());
        if (ly != mod.lyricsSetting().on()) {
            mod.lyricsSetting().set(ly);
        }
        ox += 44;
        if (ox + 70 < x + w - 9) {
            ui.dropdown("mp_repeat", ox, cy + 2.5f, Math.min(80, x + w - 9 - ox), mod.repeatSetting());
        }
        y += ch + 4;

        // ---------------------------------------------------------- lyrics preview
        List<LyricLine> lines = mp.lyricsFor(tr);
        if (mod.lyricsEnabled() && !lines.isEmpty()) {
            float lh = 46;
            Page.card(ctx, t, x, y, w, lh, a);
            int idx = mp.lyricIndex();
            for (int k = -1; k <= 1; k++) {
                int li = idx + k;
                if (li < 0 || li >= lines.size()) {
                    continue;
                }
                boolean cur = k == 0;
                float size = cur ? 7f : 5.4f;
                String txt = AquaFont.ellipsize(lines.get(li).text(), w - 24, size, cur ? AquaFont.Weight.SEMIBOLD : AquaFont.Weight.MEDIUM);
                R2D.textCentered(ctx, txt, x + w / 2f, y + 23 + k * 13, size, cur ? AquaFont.Weight.SEMIBOLD : AquaFont.Weight.MEDIUM,
                        Colors.fade(cur ? t.text() : t.muted(), a));
            }
            y += lh + 4;
        }

        // ---------------------------------------------------------- volume + library
        Page.card(ctx, t, x, y, w, 20, a);
        R2D.icon(ctx, Icon.NOTE, x + 9, y + 6.5f, 7, Colors.fade(t.muted(), a));
        R2D.textMid(ctx, "Volume", x + 19, y + 10, 5.4f, AquaFont.Weight.MEDIUM, Colors.fade(t.text(), a));
        // The slider fills the space left of the buttons, leaving room for the value.
        float sliderW = Math.max(30, Math.min(w * 0.4f, (x + w - 9 - 52 - 52) - (x + 52) - 26));
        ui.slider("mp_volume", x + 52, y + 5.5f, sliderW, mod.volumeSetting());
        R2D.textMid(ctx, mod.volumeSetting().format(), x + 58 + sliderW, y + 10, 5f, AquaFont.Weight.MEDIUM, Colors.fade(t.muted(), a));
        if (ui.button("mp_folder", x + w - 9 - 52, y + 4.5f, 52, 11, "Open folder", Icon.FOLDER, false)) {
            Util.getOperatingSystem().open(mp.folder());
        }
        if (ui.button("mp_rescan", x + w - 9 - 52 - 52, y + 4.5f, 48, 11, mp.scanning() ? "Scanning" : "Rescan", Icon.RESET, false)) {
            mp.rescan();
        }
        y += 24;

        List<Track> lib = mp.library();
        float rowH = 17;
        float lh = 22 + Math.max(1, lib.size()) * rowH + 4;
        Page.card(ctx, t, x, y, w, lh, a);
        Page.heading(ctx, t, "Library", lib.isEmpty() ? "" : lib.size() + (lib.size() == 1 ? " song" : " songs"), x + 9, y + 5, a);
        float ry = y + 22;
        if (lib.isEmpty()) {
            R2D.textMid(ctx, "Put .ogg files (and optional .lrc lyrics) in config/aqua/music, then press Rescan", x + 9, ry + rowH / 2f, 5f,
                    AquaFont.Weight.MEDIUM, Colors.fade(t.muted(), a));
        }
        for (int i = 0; i < lib.size(); i++) {
            Track tk = lib.get(i);
            boolean cur = !mp.isExternal() && i == mp.index() && tr != null;
            boolean hov = ui.hover(x + 4, ry, w - 8, rowH - 1);
            if (cur || hov) {
                R2D.round(ctx, x + 4, ry, w - 8, rowH - 1, 4, Colors.fade(cur ? Colors.withAlpha(t.accent(), 0.18f)
                        : (Glass.on() ? Colors.withAlpha(0xFFFFFF, 0.06f) : Colors.withAlpha(t.elevated(), 0.9f)), a));
            }
            R2D.icon(ctx, cur && mp.isPlaying() ? Icon.PLAY : Icon.NOTE, x + 10, ry + rowH / 2f - 3.5f, 7, Colors.fade(cur ? t.accent() : t.muted(), a));
            R2D.textMid(ctx, AquaFont.ellipsize(tk.title(), w * 0.45f, 5.6f, AquaFont.Weight.SEMIBOLD), x + 22, ry + rowH / 2f - 0.5f, 5.6f,
                    AquaFont.Weight.SEMIBOLD, Colors.fade(t.text(), a));
            R2D.textMid(ctx, AquaFont.ellipsize(tk.artist().isEmpty() ? "Unknown artist" : tk.artist(), w * 0.3f, 5f, AquaFont.Weight.MEDIUM),
                    x + 22 + w * 0.47f, ry + rowH / 2f - 0.5f, 5f, AquaFont.Weight.MEDIUM, Colors.fade(t.muted(), a));
            String right = !tk.supported() ? "Unsupported" : (tk.durationMs() > 0 ? Session.format(tk.durationMs()) : "");
            R2D.textRight(ctx, right, x + w - 12, ry + rowH / 2f - 0.5f, 5f, AquaFont.Weight.MEDIUM,
                    Colors.fade(tk.supported() ? t.muted() : 0xFFFFB547, a));
            if (ui.clickedIn(x + 4, ry, w - 8, rowH - 1)) {
                mp.play(i);
                UiSounds.click(1.1f);
            }
            ry += rowH;
        }
        return y + lh + 4;
    }
}
