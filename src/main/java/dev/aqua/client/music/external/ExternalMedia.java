package dev.aqua.client.music.external;

import dev.aqua.client.Aqua;
import dev.aqua.client.AquaClient;
import dev.aqua.client.music.MusicModule;
import dev.aqua.client.music.Track;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Music playing in another app (Spotify, or any player if allowed), read from the operating
 * system's media controls once a second on a background thread. Nothing here needs a Spotify
 * account; the only network use is fetching the cover image when an app gives a web address for it.
 */
public final class ExternalMedia {
    private static final ExternalMedia INSTANCE = new ExternalMedia();

    public static ExternalMedia get() {
        return INSTANCE;
    }

    private record Control(MediaBackend.Command command, long arg) {}

    // Written by the media thread, read on the client thread.
    private volatile NowPlaying latest;
    private volatile long sequence;
    private volatile String status = "";
    private volatile String backendName = "";
    private final ConcurrentLinkedQueue<Control> controls = new ConcurrentLinkedQueue<>();
    private final Object wake = new Object();

    // The media thread that is currently allowed to run (older ones stop on their own).
    @Nullable
    private volatile Thread thread;
    private boolean runningSpotifyOnly;
    private long seenSequence = -1;
    @Nullable
    private String trackKey;
    @Nullable
    private Track track;
    private boolean playing;
    private long basePosition;
    private long baseTime;
    private boolean positionUnreliable;
    private long ownBase;
    private long ownTime;
    private long durationMs;
    private volatile Path downloadedCover;
    private volatile String downloadedFor;

    private ExternalMedia() {}

    private static Path cacheDir() {
        return Aqua.config().dir().resolve("cache").resolve("media");
    }

    /** Called every client tick: starts or stops the reader and follows track changes. */
    public void tick() {
        MusicModule m = Aqua.modules() != null ? Aqua.modules().get(MusicModule.class) : null;
        boolean want = m != null && m.isEnabled() && m.externalEnabled();
        boolean spotifyOnly = m == null || m.spotifyOnly();
        if (want && (thread == null || spotifyOnly != runningSpotifyOnly)) {
            stop();
            start(spotifyOnly);
        } else if (!want && thread != null) {
            stop();
        }
        if (!want) {
            clear();
            return;
        }
        long seq = sequence;
        if (seq == seenSequence) {
            return;
        }
        seenSequence = seq;
        NowPlaying np = latest;
        if (np == null) {
            clear();
            return;
        }
        long now = System.currentTimeMillis();
        boolean changed = !np.key().equals(trackKey);
        long raw = np.positionMs();
        if (changed) {
            trackKey = np.key();
            positionUnreliable = false;
            ownBase = Math.max(0, raw);
            ownTime = now;
            playing = np.playing();
        } else if (playing != np.playing()) {
            ownBase = ownPosition(now);
            ownTime = now;
        }
        playing = np.playing();
        // The position the app reported, moved forward to now.
        long reported = raw < 0 ? -1 : raw + (np.playing() ? Math.max(0, now - np.sampledAt()) : 0);
        // Spotify on Linux always reports 0. Aqua's own clock (never reset by those reports) tells
        // when the song has clearly moved on; from then on the own clock is used.
        if (raw == 0 && np.playing() && ownPosition(now) > 2500) {
            positionUnreliable = true;
        }
        if (raw > 0 && !positionUnreliable) {
            ownBase = reported;
            ownTime = now;
        }
        // A playing app that says 0 is not trusted (Spotify on Linux); Aqua's own clock is used instead.
        basePosition = raw > 0 && !positionUnreliable ? reported : ownPosition(now);
        baseTime = now;
        Path cover = np.coverPath() != null ? Path.of(np.coverPath()) : (np.coverUrl() != null && np.coverUrl().equals(downloadedFor) ? downloadedCover : null);
        if (np.coverPath() == null && np.coverUrl() != null && !np.coverUrl().equals(downloadedFor)) {
            downloadCover(np.coverUrl());
        }
        if (changed || track == null || np.durationMs() != durationMs || !java.util.Objects.equals(cover, track.cover())) {
            durationMs = np.durationMs();
            track = new Track(Path.of("external", id(np.key())), np.title(), np.artist(), durationMs, List.of(), cover, true, "");
        }
    }

    private void clear() {
        trackKey = null;
        track = null;
        playing = false;
    }

    /** The other app's current track (stable object while the song and its details stay the same), or null. */
    @Nullable
    public Track track() {
        return track;
    }

    public boolean playing() {
        return track != null && playing;
    }

    private long ownPosition(long now) {
        return ownBase + (playing ? Math.max(0, now - ownTime) : 0);
    }

    public long positionMs() {
        long p = basePosition + (playing ? System.currentTimeMillis() - baseTime : 0);
        if (durationMs > 0) {
            p = Math.min(p, durationMs);
        }
        return Math.max(0, p);
    }

    /** Whether the position comes from the app (true) or is Aqua's estimate since the song started. */
    public boolean exactPosition() {
        NowPlaying np = latest;
        return np != null && np.positionMs() >= 0 && !positionUnreliable;
    }

    /** "Spotify", "Firefox", ... for the current track's app. */
    public String appName() {
        NowPlaying np = latest;
        return np == null ? "" : prettyApp(np.app());
    }

    static String prettyApp(String id) {
        String s = id;
        if (s.toLowerCase(Locale.ROOT).contains("spotify")) {
            return "Spotify";
        }
        int bang = s.lastIndexOf('!');
        if (bang >= 0) {
            s = s.substring(bang + 1);
        }
        s = s.replaceAll("(?i)\\.exe$", "");
        int dot = s.indexOf(".instance");
        if (dot > 0) {
            s = s.substring(0, dot);
        }
        int last = s.lastIndexOf('.');
        if (last >= 0 && last < s.length() - 1) {
            s = s.substring(last + 1);
        }
        return s.isEmpty() ? "Media" : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** One line for the Music page, e.g. "Reading Spotify through Windows media controls". */
    public String status() {
        return status;
    }

    /** Play/pause, next, previous or seek in the other app (sent from the media thread). */
    public void control(MediaBackend.Command command, long arg) {
        controls.add(new Control(command, arg));
        if (command == MediaBackend.Command.SEEK) {
            basePosition = Math.max(0, arg);
            baseTime = System.currentTimeMillis();
            ownBase = basePosition;
            ownTime = baseTime;
        }
        synchronized (wake) {
            wake.notifyAll();
        }
    }

    public void togglePause() {
        control(MediaBackend.Command.TOGGLE, 0);
    }

    public void next() {
        control(MediaBackend.Command.NEXT, 0);
    }

    public void previous() {
        control(MediaBackend.Command.PREVIOUS, 0);
    }

    public void seek(long ms) {
        control(MediaBackend.Command.SEEK, ms);
    }

    private void start(boolean spotifyOnly) {
        runningSpotifyOnly = spotifyOnly;
        Thread t = new Thread(() -> loop(spotifyOnly), "Aqua Media");
        t.setDaemon(true);
        thread = t;
        t.start();
    }

    /** Stops the reader (also called when the game closes). */
    public void stop() {
        Thread t = thread;
        thread = null;
        if (t != null) {
            synchronized (wake) {
                wake.notifyAll();
            }
        }
        latest = null;
        sequence++;
    }

    @Nullable
    private static MediaBackend createBackend(boolean spotifyOnly) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return new WindowsMediaBackend(cacheDir(), spotifyOnly);
        }
        if (os.contains("mac")) {
            return new MacMediaBackend();
        }
        if (LinuxMprisBackend.available()) {
            return new LinuxMprisBackend();
        }
        return null;
    }

    private boolean current() {
        return thread == Thread.currentThread();
    }

    private void loop(boolean spotifyOnly) {
        MediaBackend backend = createBackend(spotifyOnly);
        if (backend == null) {
            status = "Not available on this system (gdbus was not found)";
            return;
        }
        backendName = backend.name();
        int failures = 0;
        try {
            while (current()) {
                Control c;
                while ((c = controls.poll()) != null) {
                    try {
                        backend.control(c.command(), c.arg());
                    } catch (UnsupportedOperationException e) {
                        status = e.getMessage();
                    } catch (Exception e) {
                        AquaClient.LOGGER.warn("Media control failed: {}", e.toString());
                    }
                }
                try {
                    NowPlaying np = backend.poll(spotifyOnly);
                    if (!current()) {
                        break;
                    }
                    latest = np;
                    sequence++;
                    failures = 0;
                    String where = spotifyOnly ? "Spotify" : "other apps";
                    status = np == null ? "Nothing playing in " + where + " (" + backendName + ")"
                            : "Playing in " + prettyApp(np.app()) + " (" + backendName + ")";
                } catch (Exception e) {
                    failures++;
                    if (failures == 1 || failures % 30 == 0) {
                        AquaClient.LOGGER.warn("Could not read music from {}: {}", backendName, e.toString());
                    }
                    status = "Can't read music from " + backendName + ": " + e.getMessage();
                    if (backend instanceof WindowsMediaBackend w && w.failed()) {
                        w.close();
                        backend = new WindowsTitleBackend();
                        backendName = backend.name();
                        AquaClient.LOGGER.info("Falling back to reading Spotify's window title");
                        failures = 0;
                    } else if (failures >= 3) {
                        latest = null;
                        sequence++;
                    }
                }
                synchronized (wake) {
                    if (current() && controls.isEmpty()) {
                        wake.wait(1000);
                    }
                }
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } finally {
            backend.close();
        }
    }

    private void downloadCover(String url) {
        downloadedFor = url;
        downloadedCover = null;
        Thread t = new Thread(() -> {
            try {
                Path dir = cacheDir();
                Files.createDirectories(dir);
                Path file = dir.resolve("cover_" + id(url) + ".img");
                if (!Files.isRegularFile(file)) {
                    HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8))
                            .followRedirects(HttpClient.Redirect.NORMAL).build();
                    HttpResponse<InputStream> res = http.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).GET().build(),
                            HttpResponse.BodyHandlers.ofInputStream());
                    if (res.statusCode() != 200) {
                        return;
                    }
                    try (InputStream in = res.body()) {
                        byte[] data = in.readNBytes(4 * 1024 * 1024 + 1);
                        if (data.length > 4 * 1024 * 1024) {
                            return;
                        }
                        Files.write(file, data);
                    }
                }
                if (url.equals(downloadedFor)) {
                    downloadedCover = file;
                    sequence++;
                }
            } catch (Exception e) {
                AquaClient.LOGGER.debug("Cover download failed: {}", e.toString());
            }
        }, "Aqua Media cover");
        t.setDaemon(true);
        t.start();
    }

    private static String id(String s) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(s.getBytes(StandardCharsets.UTF_8)), 0, 10);
        } catch (Exception e) {
            return Integer.toHexString(s.hashCode());
        }
    }
}
