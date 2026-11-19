package dev.aqua.client.music;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Minimal Ogg Vorbis inspector: sample rate, duration (last granule position) and comments. */
public final class OggInfo {
    public final boolean vorbis;
    public final int sampleRate;
    public final long durationMs;
    public final Map<String, String> comments;

    private OggInfo(boolean vorbis, int sampleRate, long durationMs, Map<String, String> comments) {
        this.vorbis = vorbis;
        this.sampleRate = sampleRate;
        this.durationMs = durationMs;
        this.comments = comments;
    }

    public static OggInfo read(Path path) throws IOException {
        try (RandomAccessFile f = new RandomAccessFile(path.toFile(), "r")) {
            long len = f.length();
            int headLen = (int) Math.min(len, 256 * 1024);
            byte[] head = new byte[headLen];
            f.readFully(head);
            if (headLen < 64 || head[0] != 'O' || head[1] != 'g' || head[2] != 'g' || head[3] != 'S') {
                return new OggInfo(false, 0, -1, Map.of());
            }
            // Reassemble the first two packets (identification + comment header).
            byte[][] packets = new byte[2][];
            java.io.ByteArrayOutputStream cur = new java.io.ByteArrayOutputStream();
            int pi = 0;
            int pos = 0;
            while (pos + 27 <= headLen && pi < 2) {
                if (head[pos] != 'O' || head[pos + 1] != 'g' || head[pos + 2] != 'g' || head[pos + 3] != 'S') {
                    break;
                }
                int segs = head[pos + 26] & 0xFF;
                int dataStart = pos + 27 + segs;
                int dp = dataStart;
                for (int s = 0; s < segs && pi < 2; s++) {
                    int l = head[pos + 27 + s] & 0xFF;
                    if (dp + l > headLen) {
                        pi = 2;
                        break;
                    }
                    cur.write(head, dp, l);
                    dp += l;
                    if (l < 255) {
                        packets[pi++] = cur.toByteArray();
                        cur.reset();
                    }
                }
                int total = 0;
                for (int s = 0; s < segs; s++) {
                    total += head[pos + 27 + s] & 0xFF;
                }
                pos = dataStart + total;
            }
            byte[] id = packets[0];
            if (id == null || id.length < 16 || id[0] != 1 || !new String(id, 1, 6, StandardCharsets.US_ASCII).equals("vorbis")) {
                return new OggInfo(false, 0, -1, Map.of());
            }
            int rate = (id[12] & 0xFF) | ((id[13] & 0xFF) << 8) | ((id[14] & 0xFF) << 16) | ((id[15] & 0xFF) << 24);
            Map<String, String> comments = new HashMap<>();
            byte[] c = packets[1];
            if (c != null && c.length > 7 && c[0] == 3) {
                try {
                    int p = 7;
                    int vendorLen = le32(c, p);
                    p += 4 + vendorLen;
                    int count = le32(c, p);
                    p += 4;
                    for (int i = 0; i < count && i < 200 && p + 4 <= c.length; i++) {
                        int l = le32(c, p);
                        p += 4;
                        if (l < 0 || p + l > c.length) {
                            break;
                        }
                        String kv = new String(c, p, l, StandardCharsets.UTF_8);
                        p += l;
                        int eq = kv.indexOf('=');
                        if (eq > 0) {
                            comments.put(kv.substring(0, eq).toUpperCase(java.util.Locale.ROOT), kv.substring(eq + 1));
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            // Last page granule position = total samples.
            int tailLen = (int) Math.min(len, 128 * 1024);
            byte[] tail = new byte[tailLen];
            f.seek(len - tailLen);
            f.readFully(tail);
            long granule = -1;
            for (int i = tailLen - 27; i >= 0; i--) {
                if (tail[i] == 'O' && tail[i + 1] == 'g' && tail[i + 2] == 'g' && tail[i + 3] == 'S') {
                    long g = 0;
                    for (int b = 7; b >= 0; b--) {
                        g = (g << 8) | (tail[i + 6 + b] & 0xFF);
                    }
                    if (g > 0) {
                        granule = g;
                        break;
                    }
                }
            }
            long dur = (granule > 0 && rate > 0) ? granule * 1000L / rate : -1;
            return new OggInfo(true, rate, dur, comments);
        }
    }

    private static int le32(byte[] b, int p) {
        return (b[p] & 0xFF) | ((b[p + 1] & 0xFF) << 8) | ((b[p + 2] & 0xFF) << 16) | ((b[p + 3] & 0xFF) << 24);
    }
}
