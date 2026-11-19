package dev.aqua.client.music.external;

import org.jetbrains.annotations.Nullable;

/**
 * What another app (Spotify, a browser, ...) is playing, as reported by the operating system.
 *
 * @param app          the app's id as the OS reports it (for example {@code Spotify.exe} or {@code spotify})
 * @param durationMs   track length, or 0 when unknown
 * @param positionMs   playback position when the sample was taken, or -1 when unknown
 * @param sampledAt    {@link System#currentTimeMillis()} when the sample was taken
 * @param coverPath    a local image file with the cover art, if the OS provided one
 * @param coverUrl     a web address of the cover art, if the app provided one
 * @param trackId      the app's id for the track (used to seek on Linux)
 */
public record NowPlaying(String app, String title, String artist, String album, long durationMs, long positionMs,
                         boolean playing, long sampledAt, @Nullable String coverPath, @Nullable String coverUrl,
                         @Nullable String trackId) {
    /** Identifies the track: a different key means a different song. */
    public String key() {
        return app.toLowerCase(java.util.Locale.ROOT) + "\n" + artist + "\n" + title;
    }
}
