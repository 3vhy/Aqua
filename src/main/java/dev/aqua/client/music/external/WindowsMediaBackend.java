package dev.aqua.client.music.external;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aqua.client.AquaClient;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Windows 10/11: reads the system media session (the "now playing" box Windows shows next to the
 * volume flyout), which Spotify and most players and browsers fill in. A small PowerShell script
 * shipped with Aqua stays running in the background and prints one line per update.
 */
final class WindowsMediaBackend implements MediaBackend {
    private final Path cacheDir;
    private final boolean spotifyOnly;
    @Nullable
    private Process process;
    @Nullable
    private Writer commands;
    private volatile NowPlaying latest;
    private volatile boolean nothing = true;
    private volatile String error;
    private volatile long lastLineAt;
    private volatile int lines;
    private int starts;

    WindowsMediaBackend(Path cacheDir, boolean spotifyOnly) {
        this.cacheDir = cacheDir;
        this.spotifyOnly = spotifyOnly;
    }

    @Override
    public String name() {
        return "Windows media controls";
    }

    private static String powershell() {
        String root = System.getenv("SystemRoot");
        if (root != null) {
            Path p = Path.of(root, "System32", "WindowsPowerShell", "v1.0", "powershell.exe");
            if (Files.isRegularFile(p)) {
                return p.toString();
            }
        }
        return "powershell.exe";
    }

    private void ensureStarted() throws IOException {
        Process p = process;
        if (p != null && p.isAlive()) {
            return;
        }
        if (p != null && lines == 0) {
            starts = Math.max(starts, 3);
        }
        if (starts >= 3) {
            throw new IOException("the Windows media reader could not start" + (error != null ? ": " + error : ""));
        }
        starts++;
        Files.createDirectories(cacheDir);
        Path script = cacheDir.resolve("windows_media.ps1");
        try (InputStream in = WindowsMediaBackend.class.getResourceAsStream("/assets/aqua/media/windows_media.ps1")) {
            if (in == null) {
                throw new IOException("windows_media.ps1 is missing from the Aqua jar");
            }
            Files.copy(in, script, StandardCopyOption.REPLACE_EXISTING);
        }
        ProcessBuilder pb = new ProcessBuilder(powershell(), "-NoLogo", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass",
                "-File", script.toString());
        pb.environment().put("AQUA_MEDIA_FILTER", spotifyOnly ? "spotify" : "any");
        pb.environment().put("AQUA_COVER_DIR", cacheDir.toString());
        Process started = pb.start();
        process = started;
        lines = 0;
        lastLineAt = System.currentTimeMillis();
        commands = new OutputStreamWriter(started.getOutputStream(), StandardCharsets.UTF_8);
        Thread out = new Thread(() -> read(started), "Aqua Windows media");
        out.setDaemon(true);
        out.start();
        Thread err = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(started.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                int n = 0;
                while ((line = r.readLine()) != null) {
                    if (!line.isBlank() && n++ < 10) {
                        AquaClient.LOGGER.warn("Windows media reader: {}", line);
                        if (error == null) {
                            error = line.strip();
                        }
                    }
                }
            } catch (IOException ignored) {
            }
        }, "Aqua Windows media errors");
        err.setDaemon(true);
        err.start();
    }

    private void read(Process p) {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.strip();
                if (!line.startsWith("{")) {
                    continue;
                }
                lastLineAt = System.currentTimeMillis();
                lines++;
                try {
                    handle(JsonParser.parseString(line).getAsJsonObject(), lastLineAt);
                } catch (Exception e) {
                    error = "unreadable line from the media reader";
                }
            }
        } catch (IOException ignored) {
        }
    }

    private void handle(JsonObject o, long at) {
        if (o.has("error")) {
            error = str(o, "error");
            return;
        }
        error = null;
        if (o.has("none")) {
            nothing = true;
            latest = null;
            return;
        }
        String title = str(o, "title");
        if (title.isBlank()) {
            nothing = true;
            latest = null;
            return;
        }
        String cover = str(o, "cover");
        nothing = false;
        latest = new NowPlaying(str(o, "app"), title, str(o, "artist"), str(o, "album"), Math.max(0, num(o, "durationMs")),
                num(o, "positionMs"), o.has("playing") && o.get("playing").getAsBoolean(), at, cover.isBlank() ? null : cover, null, null);
    }

    private static String str(JsonObject o, String k) {
        JsonElement e = o.get(k);
        return e == null || e.isJsonNull() ? "" : e.getAsString();
    }

    private static long num(JsonObject o, String k) {
        JsonElement e = o.get(k);
        try {
            return e == null || e.isJsonNull() ? -1 : e.getAsLong();
        } catch (Exception ex) {
            return -1;
        }
    }

    @Override
    @Nullable
    public NowPlaying poll(boolean ignored) throws Exception {
        ensureStarted();
        if (lines > 0 && System.currentTimeMillis() - lastLineAt > 8000) {
            Process p = process;
            if (p != null) {
                p.destroyForcibly();
            }
            throw new IOException("the Windows media reader stopped answering");
        }
        if (lines == 0 && System.currentTimeMillis() - lastLineAt > 15000) {
            // It started but never answered: give up on it so the window-title fallback takes over.
            starts = 3;
            Process p = process;
            if (p != null) {
                p.destroyForcibly();
            }
            throw new IOException("the Windows media reader did not answer" + (error != null ? ": " + error : ""));
        }
        if (error != null && lines > 0 && latest == null && !nothing) {
            throw new IOException(error);
        }
        return nothing ? null : latest;
    }

    @Override
    public void control(Command command, long arg) throws Exception {
        Writer w = commands;
        if (w == null) {
            return;
        }
        String line = switch (command) {
            case TOGGLE -> "toggle";
            case NEXT -> "next";
            case PREVIOUS -> "previous";
            case SEEK -> "seek " + Math.max(0, arg);
        };
        w.write(line + "\n");
        w.flush();
    }

    /** True once the reader has given up and the window-title fallback should be used. */
    boolean failed() {
        Process p = process;
        return starts >= 3 && (p == null || !p.isAlive() || lines == 0);
    }

    @Override
    public void close() {
        Process p = process;
        process = null;
        if (p != null) {
            try {
                if (commands != null) {
                    commands.close();
                }
            } catch (IOException ignored) {
            }
            p.destroy();
        }
    }
}
