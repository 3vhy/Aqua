package dev.aqua.client.music.external;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * macOS: asks Spotify (and, if allowed, Apple Music) what is playing through AppleScript. An app is
 * only asked while it is running, so this never starts Spotify or shows a "Where is Spotify?" prompt.
 * macOS asks the player once for permission ("Minecraft wants to control Spotify").
 */
final class MacMediaBackend implements MediaBackend {
    private static final String SEP = "\u001f";

    @Nullable
    private String app;

    @Override
    public String name() {
        return "AppleScript";
    }

    private static boolean running(String process) {
        try {
            Proc.run(2000, List.of("pgrep", "-x", process));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Nullable
    public NowPlaying poll(boolean spotifyOnly) throws Exception {
        NowPlaying np = running("Spotify") ? query("Spotify") : null;
        if (np == null && !spotifyOnly && running("Music")) {
            np = query("Music");
        }
        app = np != null ? np.app() : null;
        return np;
    }

    @Nullable
    private static NowPlaying query(String app) throws Exception {
        boolean spotify = app.equals("Spotify");
        String script = "tell application \"" + app + "\"\n"
                + "  set s to player state as string\n"
                + "  if s is \"stopped\" then return \"STOPPED\"\n"
                + "  set t to current track\n"
                + "  set d to \"" + SEP + "\"\n"
                + "  return s & d & (name of t) & d & (artist of t) & d & (album of t) & d & ((duration of t) as string) & d & "
                + "((player position) as string) & d & " + (spotify ? "(artwork url of t) & d & (id of t)" : "\"\" & d & \"\"") + "\n"
                + "end tell";
        long t0 = System.currentTimeMillis();
        String out = Proc.run(4000, List.of("osascript", "-e", script)).strip();
        if (out.isEmpty() || out.equals("STOPPED")) {
            return null;
        }
        String[] f = out.split(SEP, -1);
        if (f.length < 8) {
            return null;
        }
        // Spotify reports the length in milliseconds, Apple Music in seconds; positions are seconds.
        double duration = parse(f[4]);
        long durationMs = Math.round(spotify ? duration : duration * 1000);
        long positionMs = Math.round(parse(f[5]) * 1000);
        String art = f[6].isBlank() ? null : f[6];
        return new NowPlaying(app, f[1], f[2], f[3], durationMs, positionMs, f[0].equals("playing"),
                (t0 + System.currentTimeMillis()) / 2, null, art, f[7].isBlank() ? null : f[7]);
    }

    /** AppleScript prints reals with the system's decimal separator. */
    private static double parse(String s) {
        try {
            return Double.parseDouble(s.strip().replace(',', '.'));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public void control(Command command, long arg) throws Exception {
        String a = app;
        if (a == null) {
            return;
        }
        String verb = switch (command) {
            case TOGGLE -> "playpause";
            case NEXT -> "next track";
            case PREVIOUS -> "previous track";
            case SEEK -> "set player position to " + String.format(java.util.Locale.ROOT, "%.1f", Math.max(0, arg) / 1000.0);
        };
        Proc.run(4000, List.of("osascript", "-e", "tell application \"" + a + "\" to " + verb));
    }
}
