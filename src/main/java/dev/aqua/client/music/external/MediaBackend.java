package dev.aqua.client.music.external;

import org.jetbrains.annotations.Nullable;

/** Reads what another app is playing from the operating system, and sends it play/pause and skip commands. */
interface MediaBackend extends AutoCloseable {
    enum Command { TOGGLE, NEXT, PREVIOUS, SEEK }

    /** Short name shown in Aqua's status line, e.g. "Windows media controls". */
    String name();

    /**
     * Blocking read of the current track; null when nothing that matches the filter is playing.
     * Called on Aqua's media thread, never on the render thread.
     */
    @Nullable
    NowPlaying poll(boolean spotifyOnly) throws Exception;

    /** Sends a command to the player last returned by {@link #poll}. {@code arg} is the seek position in ms. */
    void control(Command command, long arg) throws Exception;

    @Override
    default void close() {
    }
}
