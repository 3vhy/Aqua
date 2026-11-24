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
    /** Embedded cover art from the comment header (METADATA_BLOCK_PICTURE or COVERART), or null. */
    @org.jetbrains.annotations.Nullable
    public final byte[] picture;

    private OggInfo(boolean vorbis, int sampleRate, long durationMs, Map<String, String> comments) {
        this(vorbis, sampleRate, durationMs, comments, null);
    }

    private OggInfo(boolean vorbis, int sampleRate, long durationMs, Map<String, String> comments, byte[] picture) {
        this.vorbis = vorbis;
        this.sampleRate = sampleRate;
        this.durationMs = durationMs;
        this.comments = comments;
        this.picture = picture;
    }


    public static OggInfo read(Path path) throws IOException {
        try (RandomAccessFile f = new RandomAccessFile(path.toFile(), "r")) {
            long len = f.length();
            byte[] magic = new byte[4];
            if (len < 64) {
                return new OggInfo(false, 0, -1, Map.of());
            }
            f.readFully(magic);
            if (magic[0] != 'O' || magic[1] != 'g' || magic[2] != 'g' || magic[3] != 'S') {
                return new OggInfo(false, 0, -1, Map.of());
            }
            // Reassemble the first two packets (identification + comment header) page by page. The
            // comment header can be several megabytes when cover art is embedded, so it is not
            // limited to the start of the file (it is capped at 24 MB).
            f.seek(0);
            byte[][] packets = new byte[2][];
            java.io.ByteArrayOutputStream cur = new java.io.ByteArrayOutputStream();
            int pi = 0;
            byte[] header = new byte[27];
            byte[] lacing = new byte[255];
            long pos = 0;
            while (pi < 2 && pos + 27 <= len && cur.size() < 24 * 1024 * 1024) {
                f.seek(pos);
                f.readFully(header);
                if (header[0] != 'O' || header[1] != 'g' || header[2] != 'g' || header[3] != 'S') {
                    break;
                }
                int segs = header[26] & 0xFF;
                f.readFully(lacing, 0, segs);
                long dataStart = pos + 27 + segs;
                int total = 0;
                for (int s = 0; s < segs; s++) {
                    total += lacing[s] & 0xFF;
                }
                if (dataStart + total > len) {
                    break;
                }
                byte[] data = new byte[total];
                f.readFully(data);
                int dp = 0;
                for (int s = 0; s < segs && pi < 2; s++) {
                    int l = lacing[s] & 0xFF;
                    cur.write(data, dp, l);
                    dp += l;
                    if (l < 255) {
                        packets[pi++] = cur.toByteArray();
                        cur.reset();
                    }
                }
                pos = dataStart + total;
            }
            byte[] id = packets[0];
            if (id == null || id.length < 16 || id[0] != 1 || !new String(id, 1, 6, StandardCharsets.US_ASCII).equals("vorbis")) {
                return new OggInfo(false, 0, -1, Map.of());
            }
            int rate = (id[12] & 0xFF) | ((id[13] & 0xFF) << 8) | ((id[14] & 0xFF) << 16) | ((id[15] & 0xFF) << 24);
            Map<String, String> comments = new HashMap<>();
            byte[] picture = null;
            byte[] c = packets[1];
            if (c != null && c.length > 7 && c[0] == 3) {
                try {
                    int p = 7;
                    int vendorLen = le32(c, p);
                    p += 4 + vendorLen;
                    int count = le32(c, p);
                    p += 4;
                    for (int i = 0; i < count && i < 500 && p + 4 <= c.length; i++) {
                        int l = le32(c, p);
                        p += 4;
                        if (l < 0 || p + l > c.length) {
                            break;
                        }
                        int eq = -1;
                        for (int k = p; k < p + l && k < p + 64; k++) {
                            if (c[k] == '=') {
                                eq = k - p;
                                break;
                            }
                        }
                        if (eq > 0) {
                            String key = new String(c, p, eq, StandardCharsets.UTF_8).toUpperCase(java.util.Locale.ROOT);
                            if (key.equals("METADATA_BLOCK_PICTURE") || key.equals("COVERART")) {
                                if (picture == null) {
                                    picture = decodePicture(key, new String(c, p + eq + 1, l - eq - 1, StandardCharsets.US_ASCII));
                                }
                            } else if (l < 8192) {
                                comments.put(key, new String(c, p + eq + 1, l - eq - 1, StandardCharsets.UTF_8));
                            }
                        }
                        p += l;
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
            return new OggInfo(true, rate, dur, comments, picture);
        }
    }

    /**
     * METADATA_BLOCK_PICTURE holds a base64 FLAC picture block (type, MIME type, description,
     * size, colour info, then the image bytes); the older COVERART tag holds base64 image bytes.
     */
    @org.jetbrains.annotations.Nullable
    static byte[] decodePicture(String key, String base64) {
        try {
            byte[] b = java.util.Base64.getMimeDecoder().decode(base64.trim());
            if (key.equals("COVERART")) {
                return b;
            }
            java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(b);
            bb.getInt();
            int mimeLen = bb.getInt();
            bb.position(bb.position() + mimeLen);
            int descLen = bb.getInt();
            bb.position(bb.position() + descLen + 16);
            int dataLen = bb.getInt();
            if (dataLen <= 0 || dataLen > bb.remaining()) {
                return null;
            }
            byte[] img = new byte[dataLen];
            bb.get(img);
            return img;
        } catch (Exception e) {
            return null;
        }
    }

    private static int le32(byte[] b, int p) {
        return (b[p] & 0xFF) | ((b[p + 1] & 0xFF) << 8) | ((b[p + 2] & 0xFF) << 16) | ((b[p + 3] & 0xFF) << 24);
    }
}
