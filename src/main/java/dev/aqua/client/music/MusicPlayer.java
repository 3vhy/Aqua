package dev.aqua.client.music;

import dev.aqua.client.Aqua;
import dev.aqua.client.AquaClient;
import dev.aqua.client.music.external.ExternalMedia;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Plays user-provided Ogg Vorbis files from {@code config/aqua/music} through Minecraft's sound
 * engine (Jukebox/Note Blocks volume). Lyrics come from a matching {@code .lrc} file and cover art
 * from a matching {@code .png}/{@code .jpg} or {@code cover.png}. Nothing is streamed or bundled.
 */
public final class MusicPlayer {
    private static final MusicPlayer INSTANCE = new MusicPlayer();
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Aqua Music IO");
        t.setDaemon(true);
        return t;
    });

    /** Files the SoundLoader mixin may open, keyed by sound token. */
    record Pending(Path path, long offsetMs) {}

    static final ConcurrentHashMap<Integer, Pending> TOKENS = new ConcurrentHashMap<>();
    private static int nextToken = 1;

    public enum State { EMPTY, STOPPED, PLAYING, PAUSED }

    private volatile List<Track> library = List.of();
    private volatile boolean scanning;
    private int index = -1;
    private State state = State.EMPTY;
    private AquaMusicSound sound;
    private boolean sawPlaying;
    private long startedAt;
    private long pausedAt;
    private long pausedTotal;
    private long resumeAt;
    private int startWaitTicks;
    private Identifier coverId;
    private Path coverFor;
    private volatile NativeImage pendingCover;
    private String lastError = "";
    private final ConcurrentHashMap<Path, List<LyricLine>> onlineLyrics = new ConcurrentHashMap<>();
    private volatile String lyricStatus = "";
    private final Random random = new Random();

    public static MusicPlayer get() {
        return INSTANCE;
    }

    public Path folder() {
        return Aqua.config().dir().resolve("music");
    }

    public List<Track> library() {
        return library;
    }

    public boolean scanning() {
        return scanning;
    }

    public String lastError() {
        return lastError;
    }

    /** Scans the music folder on a background thread. */
    public void rescan() {
        if (scanning) {
            return;
        }
        scanning = true;
        Path dir = folder();
        IO.execute(() -> {
            List<Track> out = new ArrayList<>();
            try {
                Files.createDirectories(dir);
                try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
                    for (Path p : ds) {
                        String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
                        if (Files.isRegularFile(p) && (n.endsWith(".ogg") || n.endsWith(".mp3") || n.endsWith(".wav")
                                || n.endsWith(".flac") || n.endsWith(".m4a"))) {
                            out.add(readTrack(p));
                        }
                    }
                }
            } catch (Exception e) {
                AquaClient.LOGGER.warn("Music folder scan failed", e);
            }
            out.sort((a, b) -> a.file().getFileName().toString().compareToIgnoreCase(b.file().getFileName().toString()));
            library = Collections.unmodifiableList(out);
            scanning = false;
        });
    }

    private static Track readTrack(Path p) {
        String file = p.getFileName().toString();
        String base = file.substring(0, file.lastIndexOf('.'));
        String title = base;
        String artist = "";
        int dash = base.indexOf(" - ");
        if (dash > 0) {
            artist = base.substring(0, dash).trim();
            title = base.substring(dash + 3).trim();
        }
        if (!file.toLowerCase(Locale.ROOT).endsWith(".ogg")) {
            return new Track(p, title, artist, -1, List.of(), null, false, "Unsupported format (use .ogg Vorbis)");
        }
        long duration = -1;
        boolean ok = true;
        String problem = "";
        try {
            OggInfo info = OggInfo.read(p);
            if (!info.vorbis) {
                ok = false;
                problem = "Not an Ogg Vorbis file";
            } else {
                duration = info.durationMs;
                if (info.comments.containsKey("TITLE")) {
                    title = info.comments.get("TITLE");
                }
                if (info.comments.containsKey("ARTIST")) {
                    artist = info.comments.get("ARTIST");
                }
            }
        } catch (Exception e) {
            ok = false;
            problem = "Unreadable file";
        }
        List<LyricLine> lyrics = List.of();
        Path lrc = p.resolveSibling(base + ".lrc");
        if (Files.exists(lrc)) {
            try {
                if (Files.size(lrc) < 512 * 1024) {
                    lyrics = List.copyOf(LrcParser.parse(Files.readString(lrc, StandardCharsets.UTF_8)));
                }
            } catch (Exception e) {
                AquaClient.LOGGER.warn("Could not read lyrics {}", lrc);
            }
        }
        Path cover = null;
        for (String c : new String[]{base + ".png", base + ".jpg", "cover.png", "cover.jpg", "folder.png"}) {
            Path cp = p.resolveSibling(c);
            if (Files.exists(cp)) {
                cover = cp;
                break;
            }
        }
        return new Track(p, limit(title), limit(artist), duration, lyrics, cover, ok, problem);
    }

    private static String limit(String s) {
        s = s.replaceAll("\\p{Cntrl}", "");
        return s.length() > 80 ? s.substring(0, 80) : s;
    }

    @Nullable
    private Track localCurrent() {
        List<Track> lib = library;
        return index >= 0 && index < lib.size() && state != State.EMPTY ? lib.get(index) : null;
    }

    /**
     * The other app's track when it should be shown instead of Aqua's own player: Aqua's own song
     * while it plays, otherwise a song playing in Spotify, otherwise Aqua's paused song, otherwise a
     * paused song in Spotify.
     */
    @Nullable
    private Track externalShown() {
        ExternalMedia em = ExternalMedia.get();
        Track et = em.track();
        if (et == null || state == State.PLAYING) {
            return null;
        }
        if (em.playing()) {
            return et;
        }
        return state == State.PAUSED ? null : et;
    }

    /** True while the shown song is playing in another app (Spotify, ...) rather than in Aqua. */
    public boolean isExternal() {
        return externalShown() != null;
    }

    /** "Spotify" etc. while showing another app's song, otherwise null. */
    @Nullable
    public String sourceApp() {
        return isExternal() ? ExternalMedia.get().appName() : null;
    }

    @Nullable
    public Track current() {
        Track ext = externalShown();
        return ext != null ? ext : localCurrent();
    }

    public State state() {
        if (externalShown() != null) {
            return ExternalMedia.get().playing() ? State.PLAYING : State.PAUSED;
        }
        return state;
    }

    public boolean isPlaying() {
        return state() == State.PLAYING;
    }

    public void play(int i) {
        play(i, 0);
    }

    public void play(int i, long offsetMs) {
        List<Track> lib = library;
        if (i < 0 || i >= lib.size()) {
            return;
        }
        Track t = lib.get(i);
        stopSound();
        index = i;
        if (!t.supported()) {
            lastError = t.title() + ": " + t.problem();
            Aqua.notifications().warn("Music", lastError);
            state = State.STOPPED;
            return;
        }
        int token = nextToken++;
        TOKENS.put(token, new Pending(t.file(), offsetMs));
        sound = new AquaMusicSound(token, volume());
        MinecraftClient.getInstance().getSoundManager().play(sound);
        state = State.PLAYING;
        sawPlaying = false;
        startWaitTicks = 0;
        startedAt = System.currentTimeMillis() - offsetMs;
        pausedTotal = 0;
        pausedAt = 0;
        requestCover(t);
        requestLyrics(t);
    }

    private void requestLyrics(Track t) {
        MusicModule m = Aqua.modules().get(MusicModule.class);
        if (!t.lyrics().isEmpty()) {
            lyricStatus = "Lyrics from .lrc file";
            return;
        }
        if (onlineLyrics.containsKey(t.file())) {
            lyricStatus = onlineLyrics.get(t.file()).isEmpty() ? "No synced lyrics found" : "Lyrics from LRCLIB";
            return;
        }
        if (m == null || !m.fetchOnline()) {
            lyricStatus = "";
            return;
        }
        lyricStatus = "Searching lyrics online…";
        LyricsFetcher.fetch(t).thenAccept(o -> {
            switch (o.result()) {
                case FOUND -> {
                    onlineLyrics.put(t.file(), List.copyOf(o.lines()));
                    lyricStatus = "Lyrics from LRCLIB";
                }
                case NOT_FOUND -> {
                    onlineLyrics.put(t.file(), List.of());
                    lyricStatus = "No synced lyrics found";
                }
                case FAILED -> lyricStatus = "Lyrics lookup failed (offline?)";
            }
        });
    }

    /** Lyrics for a track: its .lrc file first, otherwise lyrics found online. Empty when lyrics are off. */
    public List<LyricLine> lyricsFor(@Nullable Track t) {
        if (t == null) {
            return List.of();
        }
        MusicModule m = Aqua.modules().get(MusicModule.class);
        if (m != null && !m.lyricsEnabled()) {
            return List.of();
        }
        if (!t.lyrics().isEmpty()) {
            return t.lyrics();
        }
        return onlineLyrics.getOrDefault(t.file(), List.of());
    }

    public String lyricStatus() {
        MusicModule m = Aqua.modules().get(MusicModule.class);
        return m != null && !m.lyricsEnabled() ? "Lyrics off" : lyricStatus;
    }

    /** Restarts the current track from a position (used by the seek bar). */
    public void seek(long ms) {
        if (isExternal()) {
            ExternalMedia.get().seek(ms);
            return;
        }
        Track t = localCurrent();
        if (t == null) {
            return;
        }
        long clamped = Math.max(0, t.durationMs() > 0 ? Math.min(ms, t.durationMs() - 500) : ms);
        play(index, clamped);
    }

    public int index() {
        return index;
    }

    private float volume() {
        MusicModule m = Aqua.modules().get(MusicModule.class);
        return m != null ? m.volume() : 1f;
    }

    public void togglePause() {
        if (isExternal()) {
            ExternalMedia.get().togglePause();
            return;
        }
        if (state == State.PLAYING) {
            pause();
        } else if (state == State.PAUSED) {
            resume();
        } else if (!library.isEmpty()) {
            play(Math.max(0, index));
        }
    }

    public void pause() {
        if (state != State.PLAYING || sound == null) {
            return;
        }
        // Minecraft's sound manager has no per-instance pause; stop and remember the position.
        pausedAt = System.currentTimeMillis();
        resumeAt = positionMs();
        MinecraftClient.getInstance().getSoundManager().stop(sound);
        state = State.PAUSED;
    }

    public void resume() {
        if (state != State.PAUSED) {
            return;
        }
        // Restart the stream and skip decoded audio up to the paused position.
        play(index, resumeAt);
    }

    public void next() {
        if (isExternal()) {
            ExternalMedia.get().next();
            return;
        }
        List<Track> lib = library;
        if (lib.isEmpty()) {
            return;
        }
        MusicModule m = Aqua.modules().get(MusicModule.class);
        int n = (m != null && m.shuffle()) ? random.nextInt(lib.size()) : (index + 1) % lib.size();
        play(n);
    }

    public void previous() {
        if (isExternal()) {
            ExternalMedia.get().previous();
            return;
        }
        List<Track> lib = library;
        if (lib.isEmpty()) {
            return;
        }
        if (positionMs() > 3000) {
            play(index);
        } else {
            play((index - 1 + lib.size()) % lib.size());
        }
    }

    public void stop() {
        stopSound();
        state = library.isEmpty() ? State.EMPTY : State.STOPPED;
    }

    private void stopSound() {
        if (sound != null) {
            MinecraftClient.getInstance().getSoundManager().stop(sound);
            sound = null;
        }
    }

    public long positionMs() {
        if (isExternal()) {
            return ExternalMedia.get().positionMs();
        }
        if (state == State.EMPTY || state == State.STOPPED) {
            return 0;
        }
        long now = state == State.PAUSED ? pausedAt : System.currentTimeMillis();
        long p = now - startedAt - pausedTotal;
        Track t = localCurrent();
        if (t != null && t.durationMs() > 0) {
            p = Math.min(p, t.durationMs());
        }
        return Math.max(0, p);
    }

    /** Index of the lyric line active at the current position, or -1. */
    public int lyricIndex() {
        Track t = current();
        List<LyricLine> lines = lyricsFor(t);
        if (t == null || lines.isEmpty()) {
            return -1;
        }
        long pos = positionMs();
        int idx = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).timeMs() <= pos) {
                idx = i;
            } else {
                break;
            }
        }
        return idx;
    }

    @Nullable
    private Path externalLyricsFor;
    @Nullable
    private Path externalCoverFor;

    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ExternalMedia.get().tick();
        Track ext = externalShown();
        if (ext != null) {
            if (!ext.file().equals(externalLyricsFor)) {
                externalLyricsFor = ext.file();
                requestLyrics(ext);
            }
            if (ext.cover() != null && !ext.cover().equals(externalCoverFor)) {
                externalCoverFor = ext.cover();
                requestCover(ext);
            }
        } else if (externalLyricsFor != null) {
            externalLyricsFor = null;
            externalCoverFor = null;
            Track local = localCurrent();
            if (local != null && state != State.STOPPED && state != State.EMPTY) {
                requestCover(local);
                requestLyrics(local);
            }
        }
        if (pendingCover != null) {
            NativeImage img = pendingCover;
            pendingCover = null;
            if (coverId != null) {
                mc.getTextureManager().destroyTexture(coverId);
            }
            coverId = Identifier.of("aqua", "dynamic/music_cover");
            mc.getTextureManager().registerTexture(coverId, new NativeImageBackedTexture(() -> "aqua_cover", img));
        }
        if (state == State.PLAYING) {
            if (mc.isPaused()) {
                // Game paused (singleplayer): the sound engine pauses sources, so pause the clock too.
                pausedTotal += 50;
            }
            if (sound != null) {
                boolean playing = mc.getSoundManager().isPlaying(sound);
                if (playing) {
                    sawPlaying = true;
                } else if (sawPlaying || ++startWaitTicks > 100) {
                    Track t = localCurrent();
                    boolean finished = t != null && (t.durationMs() <= 0 || positionMs() > t.durationMs() - 1500);
                    sound = null;
                    if (finished || !sawPlaying) {
                        if (!sawPlaying) {
                            lastError = "Playback could not start";
                        }
                        MusicModule m = Aqua.modules().get(MusicModule.class);
                        if (m != null && m.repeatOne()) {
                            play(index);
                        } else if (m == null || m.autoAdvance()) {
                            next();
                        } else {
                            state = State.STOPPED;
                        }
                    } else {
                        // Stopped externally (e.g. leaving a world).
                        state = State.STOPPED;
                    }
                }
            }
        }
        if (state == State.EMPTY && !library.isEmpty()) {
            state = State.STOPPED;
            index = Math.max(index, 0);
        }
    }

    public void applyVolume() {
        if (sound != null) {
            sound.setVolume(volume());
        }
    }

    private void requestCover(Track t) {
        if (t.cover() == null) {
            coverFor = null;
            return;
        }
        if (t.cover().equals(coverFor)) {
            return;
        }
        coverFor = t.cover();
        Path p = t.cover();
        IO.execute(() -> {
            try {
                if (Files.size(p) > 8L * 1024 * 1024) {
                    return;
                }
                pendingCover = decodeCover(p);
            } catch (Exception e) {
                AquaClient.LOGGER.warn("Could not load cover art {}", p);
            }
        });
    }

    /**
     * Minecraft's image loader only accepts PNG, and cover art is usually JPEG (Spotify's always is),
     * so covers are decoded with Java's image reader and scaled down to at most 256 pixels.
     */
    private static NativeImage decodeCover(Path p) throws java.io.IOException {
        java.awt.image.BufferedImage src;
        try (InputStream in = Files.newInputStream(p)) {
            src = javax.imageio.ImageIO.read(in);
        }
        if (src == null) {
            throw new java.io.IOException("unsupported image format");
        }
        int max = 256;
        float k = Math.min(1f, max / (float) Math.max(src.getWidth(), src.getHeight()));
        int w = Math.max(1, Math.round(src.getWidth() * k));
        int h = Math.max(1, Math.round(src.getHeight() * k));
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        NativeImage out = new NativeImage(w, h, false);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                out.setColorArgb(x, y, img.getRGB(x, y));
            }
        }
        return out;
    }

    @Nullable
    public Identifier cover() {
        Track t = current();
        return t != null && t.cover() != null && t.cover().equals(coverFor) ? coverId : null;
    }

    /** Opens the stream for a sound token (called by the SoundLoader mixin on the loader thread). */
    @Nullable
    public static net.minecraft.client.sound.AudioStream openStream(int token) throws java.io.IOException {
        Pending p = TOKENS.remove(token);
        if (p == null) {
            return null;
        }
        net.minecraft.client.sound.OggAudioStream ogg =
                new net.minecraft.client.sound.OggAudioStream(new java.io.BufferedInputStream(Files.newInputStream(p.path())));
        return p.offsetMs() > 0 ? new SkippingAudioStream(ogg, p.offsetMs()) : ogg;
    }
}
