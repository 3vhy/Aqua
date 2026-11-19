package dev.aqua.client;

import dev.aqua.client.config.ConfigManager;
import dev.aqua.client.config.UiSettings;
import dev.aqua.client.hud.HudManager;
import dev.aqua.client.module.KeybindManager;
import dev.aqua.client.module.ModuleManager;
import dev.aqua.client.notify.Notifications;
import dev.aqua.client.theme.Theme;
import dev.aqua.client.theme.ThemeManager;

/** Static access point to Aqua's managers (client thread only). */
public final class Aqua {
    public static final String NAME = "Aqua";
    public static final String VERSION = "1.0.0";

    static UiSettings ui;
    static ThemeManager themes;
    static ModuleManager modules;
    static HudManager hud;
    static ConfigManager config;
    static KeybindManager keybinds;
    static Notifications notifications;

    private Aqua() {}

    public static UiSettings ui() {
        return ui;
    }

    public static ThemeManager themes() {
        return themes;
    }

    public static Theme theme() {
        return themes.active();
    }

    public static ModuleManager modules() {
        return modules;
    }

    public static HudManager hud() {
        return hud;
    }

    public static ConfigManager config() {
        return config;
    }

    public static KeybindManager keybinds() {
        return keybinds;
    }

    public static Notifications notifications() {
        return notifications;
    }
}
