package dev.aqua.client.music.external;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Linux: media players (Spotify, browsers, ...) publish what they play over D-Bus (the MPRIS
 * standard). This reads it with the {@code gdbus} command that ships with GLib.
 */
final class LinuxMprisBackend implements MediaBackend {
    private static final String PREFIX = "org.mpris.MediaPlayer2.";
    private static final String PATH = "/org/mpris/MediaPlayer2";
    private static final Pattern NAME = Pattern.compile("'(org\\.mpris\\.MediaPlayer2\\.[^']+)'");

    @Nullable
    private String player;
    @Nullable
    private String trackId;

    static boolean available() {
        return Proc.exists("gdbus");
    }

    @Override
    public String name() {
        return "MPRIS (D-Bus)";
    }

    @Override
    @Nullable
    public NowPlaying poll(boolean spotifyOnly) throws Exception {
        String names = gdbus("--dest", "org.freedesktop.DBus", "--object-path", "/org/freedesktop/DBus",
                "--method", "org.freedesktop.DBus.ListNames");
        List<String> players = new ArrayList<>();
        Matcher m = NAME.matcher(names);
        while (m.find()) {
            players.add(m.group(1));
        }
        players.sort((a, b) -> Boolean.compare(!isSpotify(a), !isSpotify(b)));
        NowPlaying firstPaused = null;
        String firstPausedName = null;
        for (String name : players) {
            if (spotifyOnly && !isSpotify(name)) {
                continue;
            }
            NowPlaying np;
            try {
                np = read(name);
            } catch (Exception e) {
                continue;
            }
            if (np == null) {
                continue;
            }
            if (np.playing()) {
                player = name;
                trackId = np.trackId();
                return np;
            }
            if (firstPaused == null) {
                firstPaused = np;
                firstPausedName = name;
            }
        }
        player = firstPausedName;
        trackId = firstPaused != null ? firstPaused.trackId() : null;
        return firstPaused;
    }

    private static boolean isSpotify(String busName) {
        return busName.toLowerCase(Locale.ROOT).contains("spotify");
    }

    @Nullable
    private NowPlaying read(String name) throws Exception {
        long t0 = System.currentTimeMillis();
        String all = gdbus("--dest", name, "--object-path", PATH, "--method", "org.freedesktop.DBus.Properties.GetAll",
                "org.mpris.MediaPlayer2.Player");
        String status = GVariantText.string(all, "PlaybackStatus");
        String title = GVariantText.string(all, "xesam:title");
        if (title == null || title.isBlank() || "Stopped".equals(status)) {
            return null;
        }
        List<String> artists = GVariantText.strings(all, "xesam:artist");
        String album = GVariantText.string(all, "xesam:album");
        long lengthUs = GVariantText.number(all, "mpris:length", 0);
        long positionUs = GVariantText.number(all, "Position", -1);
        String art = GVariantText.string(all, "mpris:artUrl");
        String coverPath = null;
        String coverUrl = null;
        if (art != null && art.startsWith("file://")) {
            coverPath = java.net.URI.create(art).getPath();
        } else if (art != null && (art.startsWith("https://") || art.startsWith("http://"))) {
            coverUrl = art;
        }
        String app = name.substring(PREFIX.length());
        return new NowPlaying(app, title, String.join(", ", artists), album == null ? "" : album, Math.max(0, lengthUs / 1000),
                positionUs < 0 ? -1 : positionUs / 1000, "Playing".equals(status), (t0 + System.currentTimeMillis()) / 2,
                coverPath, coverUrl, GVariantText.string(all, "mpris:trackid"));
    }

    @Override
    public void control(Command command, long arg) throws Exception {
        String p = player;
        if (p == null) {
            return;
        }
        switch (command) {
            case TOGGLE -> gdbus("--dest", p, "--object-path", PATH, "--method", "org.mpris.MediaPlayer2.Player.PlayPause");
            case NEXT -> gdbus("--dest", p, "--object-path", PATH, "--method", "org.mpris.MediaPlayer2.Player.Next");
            case PREVIOUS -> gdbus("--dest", p, "--object-path", PATH, "--method", "org.mpris.MediaPlayer2.Player.Previous");
            case SEEK -> {
                if (trackId != null && trackId.startsWith("/")) {
                    gdbus("--dest", p, "--object-path", PATH, "--method", "org.mpris.MediaPlayer2.Player.SetPosition",
                            "objectpath '" + trackId.replace("'", "") + "'", "int64 " + Math.max(0, arg) * 1000);
                }
            }
        }
    }

    private static String gdbus(String... args) throws Exception {
        List<String> cmd = new ArrayList<>(List.of("gdbus", "call", "--session"));
        cmd.addAll(List.of(args));
        return Proc.run(3000, cmd);
    }
}
