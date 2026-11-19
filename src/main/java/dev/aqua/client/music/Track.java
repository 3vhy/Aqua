package dev.aqua.client.music;

import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

/** A local audio file with metadata read from its Vorbis comments or file name. */
public record Track(Path file, String title, String artist, long durationMs, List<LyricLine> lyrics, @Nullable Path cover,
                    boolean supported, String problem) {
}
