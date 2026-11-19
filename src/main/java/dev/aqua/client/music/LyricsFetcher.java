package dev.aqua.client.music;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aqua.client.Aqua;
import dev.aqua.client.AquaClient;

import java.net.URI;
import java.net.URLEncoder;
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
import java.util.concurrent.CompletableFuture;

/**
 * Finds synced lyrics for a track on LRCLIB (https://lrclib.net, a free public lyrics database) and
 * caches them in config/aqua/lyrics. Only the track's title, artist and duration are sent.
 */
public final class LyricsFetcher {
    public enum Result { FOUND, NOT_FOUND, FAILED }

    public record Outcome(Result result, List<LyricLine> lines, String source) {}

    private static final String API = "https://lrclib.net/api";
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL).build();

    private LyricsFetcher() {}

    private static Path cacheDir() {
        return Aqua.config().dir().resolve("lyrics");
    }

    private static String key(Track t) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] h = md.digest((t.artist().toLowerCase(Locale.ROOT) + "\n" + t.title().toLowerCase(Locale.ROOT)).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h, 0, 10);
        } catch (Exception e) {
            return Integer.toHexString((t.artist() + t.title()).hashCode());
        }
    }

    public static CompletableFuture<Outcome> fetch(Track t) {
        return CompletableFuture.supplyAsync(() -> {
            Path dir = cacheDir();
            Path lrc = dir.resolve(key(t) + ".lrc");
            Path none = dir.resolve(key(t) + ".none");
            try {
                Files.createDirectories(dir);
                if (Files.exists(lrc)) {
                    return new Outcome(Result.FOUND, LrcParser.parse(Files.readString(lrc, StandardCharsets.UTF_8)), "cache");
                }
                if (Files.exists(none) && System.currentTimeMillis() - Files.getLastModifiedTime(none).toMillis() < 3L * 24 * 3600 * 1000) {
                    return new Outcome(Result.NOT_FOUND, List.of(), "cache");
                }
                String synced = null;
                if (!t.artist().isBlank() && t.durationMs() > 0) {
                    JsonObject o = getObject(API + "/get?artist_name=" + enc(t.artist()) + "&track_name=" + enc(t.title())
                            + "&duration=" + Math.round(t.durationMs() / 1000.0));
                    synced = o != null ? str(o, "syncedLyrics") : null;
                }
                if (synced == null) {
                    String q = API + "/search?track_name=" + enc(t.title()) + (t.artist().isBlank() ? "" : "&artist_name=" + enc(t.artist()));
                    synced = bestFromSearch(getArray(q), t);
                    if (synced == null && !t.artist().isBlank()) {
                        synced = bestFromSearch(getArray(API + "/search?track_name=" + enc(t.title())), t);
                    }
                }
                if (synced == null || synced.isBlank()) {
                    Files.writeString(none, "");
                    return new Outcome(Result.NOT_FOUND, List.of(), "LRCLIB");
                }
                Files.writeString(lrc, synced, StandardCharsets.UTF_8);
                return new Outcome(Result.FOUND, LrcParser.parse(synced), "LRCLIB");
            } catch (Exception e) {
                AquaClient.LOGGER.warn("Lyrics lookup failed for {} - {}: {}", t.artist(), t.title(), e.toString());
                return new Outcome(Result.FAILED, List.of(), "LRCLIB");
            }
        }, net.minecraft.util.Util.getDownloadWorkerExecutor());
    }

    private static String bestFromSearch(JsonArray arr, Track t) {
        if (arr == null) {
            return null;
        }
        String fallback = null;
        for (JsonElement e : arr) {
            if (!e.isJsonObject()) {
                continue;
            }
            JsonObject o = e.getAsJsonObject();
            String s = str(o, "syncedLyrics");
            if (s == null || s.isBlank()) {
                continue;
            }
            if (fallback == null) {
                fallback = s;
            }
            if (t.durationMs() > 0 && o.has("duration") && !o.get("duration").isJsonNull()
                    && Math.abs(o.get("duration").getAsDouble() * 1000 - t.durationMs()) < 4000) {
                return s;
            }
        }
        return fallback;
    }

    private static String str(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : null;
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String get(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(12))
                .header("User-Agent", "AquaClient/" + Aqua.VERSION + " (Minecraft mod; lyrics lookup)").GET().build();
        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (res.statusCode() == 404) {
            return null;
        }
        if (res.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + res.statusCode());
        }
        String body = res.body();
        return body.length() > 2_000_000 ? null : body;
    }

    private static JsonObject getObject(String url) throws Exception {
        String b = get(url);
        if (b == null) {
            return null;
        }
        JsonElement e = JsonParser.parseString(b);
        return e.isJsonObject() ? e.getAsJsonObject() : null;
    }

    private static JsonArray getArray(String url) throws Exception {
        String b = get(url);
        if (b == null) {
            return null;
        }
        JsonElement e = JsonParser.parseString(b);
        return e.isJsonArray() ? e.getAsJsonArray() : null;
    }
}
