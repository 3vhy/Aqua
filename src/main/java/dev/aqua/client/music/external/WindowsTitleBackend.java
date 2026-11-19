package dev.aqua.client.music.external;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Windows fallback when the media-controls reader cannot run (for example where PowerShell is
 * blocked): while Spotify plays, its window title is "Artist - Song". There is no position, length
 * or pause state in this mode, and Spotify cannot be controlled from Aqua.
 */
final class WindowsTitleBackend implements MediaBackend {
    @Override
    public String name() {
        return "Spotify window title";
    }

    @Override
    @Nullable
    public NowPlaying poll(boolean spotifyOnly) throws Exception {
        long t0 = System.currentTimeMillis();
        String out = Proc.run(6000, List.of("tasklist.exe", "/v", "/fo", "csv", "/nh", "/fi", "imagename eq Spotify.exe"));
        for (String line : out.split("\\R")) {
            List<String> cols = csv(line);
            if (cols.size() < 9) {
                continue;
            }
            String title = cols.get(cols.size() - 1).strip();
            int dash = title.indexOf(" - ");
            if (dash <= 0 || title.equalsIgnoreCase("N/A") || title.startsWith("Spotify")) {
                continue;
            }
            return new NowPlaying("Spotify.exe", title.substring(dash + 3).strip(), title.substring(0, dash).strip(), "", 0, -1, true,
                    (t0 + System.currentTimeMillis()) / 2, null, null, null);
        }
        return null;
    }

    private static List<String> csv(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (c == ',' && !quoted) {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out;
    }

    @Override
    public void control(Command command, long arg) {
        throw new UnsupportedOperationException("Spotify can't be controlled in window-title mode");
    }
}
