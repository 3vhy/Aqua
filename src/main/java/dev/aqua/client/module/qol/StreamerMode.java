package dev.aqua.client.module.qol;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.StringSetting;

/**
 * "Skin Protect" (R3) adapted: hides your own identity on your own screen only - your name in Aqua's
 * HUD and above your head in third person, coordinates and server address. It never hides anything
 * from other players and reveals nothing about them.
 */
public final class StreamerMode extends Module {
    private final StringSetting alias = add(new StringSetting("alias", "Display name", "You", 16));
    private final BoolSetting hideNametag = add(new BoolSetting("nametag", "Hide own nametag name", true));
    private final BoolSetting hideCoords = add(new BoolSetting("coords", "Hide coordinates in HUD", false));
    private final BoolSetting hideServer = add(new BoolSetting("server", "Hide server address in HUD", true));
    private final BoolSetting hideSkin = add(new BoolSetting("skin", "Use default skin for yourself", false));

    public StreamerMode() {
        super("streamer_mode", "Streamer Mode", "Hides your name, coordinates and server on your own screen.", Category.QOL, false,
                "skin protect", "privacy", "hide name", "nick");
    }

    public String alias() {
        return alias.get().isBlank() ? "You" : alias.get();
    }

    public boolean hideNametag() {
        return hideNametag.on();
    }

    public boolean hideCoordinates() {
        return hideCoords.on();
    }

    public boolean hideServer() {
        return hideServer.on();
    }

    public boolean hideSkin() {
        return hideSkin.on();
    }
}
