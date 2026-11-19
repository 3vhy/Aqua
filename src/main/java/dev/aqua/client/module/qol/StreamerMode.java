package dev.aqua.client.module.qol;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.BoolSetting;

/** Hides your coordinates and server address in Aqua's HUD (see also Name Protect and Skin Protect). */
public final class StreamerMode extends Module {
    private final BoolSetting hideCoords = add(new BoolSetting("coords", "Hide coordinates in HUD", false));
    private final BoolSetting hideServer = add(new BoolSetting("server", "Hide server address in HUD", true));

    public StreamerMode() {
        super("streamer_mode", "Streamer Mode", "Hides your coordinates and server address in Aqua's HUD.", Category.QOL, false,
                "privacy", "hide coordinates", "hide server", "streaming");
    }

    public boolean hideCoordinates() {
        return hideCoords.on();
    }

    public boolean hideServer() {
        return hideServer.on();
    }
}
