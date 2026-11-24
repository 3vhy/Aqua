package dev.aqua.client.music;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aqua.client.Aqua;
import dev.aqua.client.AquaClient;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
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
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Finds album art for a song that has none, with Apple's public iTunes Search API (no account).
 * Only the artist and title are sent. Results are cached in config/aqua/cache/covers.
 */
public final class CoverFetcher {
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL).build();

    private CoverFetcher() {}

    private static String key(Track t) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(
                    (t.artist().toLowerCase(Locale.ROOT) + "\n" + t.title().toLowerCase(Locale.ROOT)).getBytes(StandardCharsets.UTF_8)), 0, 10);
        } catch (Exception e) {
            return Integer.toHexString((t.artist() + t.title()).hashCode());
        }
    }

    /** Completes with a local image file, or null when no cover was found. */
    public static CompletableFuture<Path> fetch(Track t) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path dir = Aqua.config().dir().resolve("cache").resolve("covers");
                Files.createDirectories(dir);
                Path file = dir.resolve("online_" + key(t) + ".img");
                Path none = dir.resolve("online_" + key(t) + ".none");
                if (Files.isRegularFile(file)) {
                    return file;
                }
                if (Files.exists(none) && System.currentTimeMillis() - Files.getLastModifiedTime(none).toMillis() < 3L * 24 * 3600 * 1000) {
                    return null;
                }
                String url = find(t);
                if (url == null) {
                    Files.writeString(none, "");
                    return null;
                }
                HttpResponse<InputStream> res = HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(12)).GET().build(),
                        HttpResponse.BodyHandlers.ofInputStream());
                if (res.statusCode() != 200) {
                    return null;
                }
                try (InputStream in = res.body()) {
                    byte[] data = in.readNBytes(4 * 1024 * 1024 + 1);
                    if (data.length > 4 * 1024 * 1024 || data.length == 0) {
                        return null;
                    }
                    Files.write(file, data);
                }
                return file;
            } catch (Exception e) {
                AquaClient.LOGGER.debug("Cover lookup failed for {} - {}: {}", t.artist(), t.title(), e.toString());
                return null;
            }
        }, net.minecraft.util.Util.getDownloadWorkerExecutor());
    }

    @Nullable
    private static String find(Track t) throws Exception {
        String term = (t.artist().isBlank() ? "" : t.artist() + " ") + t.title();
        String q = "https://itunes.apple.com/search?media=music&entity=song&limit=10&term=" + URLEncoder.encode(term, StandardCharsets.UTF_8);
        HttpResponse<String> res = HTTP.send(HttpRequest.newBuilder(URI.create(q)).timeout(Duration.ofSeconds(12))
                .header("User-Agent", "AquaClient/" + Aqua.VERSION + " (Minecraft mod; cover art)").GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (res.statusCode() != 200) {
            return null;
        }
        JsonElement root = JsonParser.parseString(res.body());
        if (!root.isJsonObject() || !root.getAsJsonObject().has("results")) {
            return null;
        }
        JsonArray results = root.getAsJsonObject().getAsJsonArray("results");
        String want = norm(t.title());
        String wantArtist = norm(t.artist());
        String fallback = null;
        for (JsonElement e : results) {
            if (!e.isJsonObject()) {
                continue;
            }
            JsonObject o = e.getAsJsonObject();
            String art = o.has("artworkUrl100") ? o.get("artworkUrl100").getAsString() : null;
            if (art == null) {
                continue;
            }
            art = art.replace("100x100bb", "512x512bb");
            String title = o.has("trackName") ? norm(o.get("trackName").getAsString()) : "";
            String artist = o.has("artistName") ? norm(o.get("artistName").getAsString()) : "";
            boolean titleOk = !want.isEmpty() && (title.equals(want) || title.startsWith(want) || want.startsWith(title));
            if (titleOk && (wantArtist.isEmpty() || artist.contains(wantArtist) || wantArtist.contains(artist))) {
                return art;
            }
            if (titleOk && fallback == null) {
                fallback = art;
            }
        }
        return fallback;
    }

    private static String norm(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("\\(.*?\\)|\\[.*?]", "").replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }
}
