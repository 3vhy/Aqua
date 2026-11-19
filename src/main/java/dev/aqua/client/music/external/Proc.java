package dev.aqua.client.music.external;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Runs a short helper command (gdbus, osascript, tasklist) and returns what it printed. */
final class Proc {
    private Proc() {}

    static String run(long timeoutMs, List<String> command) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(command).redirectErrorStream(false).start();
        p.getOutputStream().close();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Thread reader = new Thread(() -> copy(p.getInputStream(), out), "Aqua media helper output");
        reader.setDaemon(true);
        reader.start();
        Thread errors = new Thread(() -> copy(p.getErrorStream(), new ByteArrayOutputStream()), "Aqua media helper errors");
        errors.setDaemon(true);
        errors.start();
        if (!p.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) {
            p.destroyForcibly();
            throw new IOException(command.get(0) + " did not answer in time");
        }
        reader.join(1000);
        if (p.exitValue() != 0) {
            throw new IOException(command.get(0) + " exited with code " + p.exitValue());
        }
        synchronized (out) {
            return out.toString(StandardCharsets.UTF_8);
        }
    }

    private static void copy(InputStream in, ByteArrayOutputStream out) {
        byte[] buf = new byte[8192];
        try (in) {
            int n;
            while ((n = in.read(buf)) > 0) {
                synchronized (out) {
                    if (out.size() < 4 * 1024 * 1024) {
                        out.write(buf, 0, n);
                    }
                }
            }
        } catch (IOException ignored) {
        }
    }

    static boolean exists(String program) {
        String path = System.getenv("PATH");
        if (path == null) {
            return false;
        }
        for (String dir : path.split(java.io.File.pathSeparator)) {
            if (!dir.isEmpty() && java.nio.file.Files.isExecutable(java.nio.file.Path.of(dir, program))) {
                return true;
            }
        }
        return false;
    }
}
