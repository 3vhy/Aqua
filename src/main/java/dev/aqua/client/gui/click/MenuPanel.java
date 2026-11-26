package dev.aqua.client.gui.click;

import dev.aqua.client.render.Icon;

import java.util.List;

/**
 * The category panels of the panel menu, with their default places. The layout follows the
 * reference video: a row of panels across the top, Combat and the client panel to the right, and
 * Visuals overlapping them on the far right. Positions are in a 640x360 layout that is scaled to
 * the window.
 */
public enum MenuPanel {
    RENDER("Render", Icon.EYE, 25, 62, List.of("freelook", "low_health_warning", "nametag_style", "time_changer", "waypoints",
            "death_waypoints", "weather_changer", "world_tint", "custom_fog", "custom_rain", "rain_splashes", "water_colour",
            "no_block_rotation", "low_overlays", "smooth_camera")),
    SERVER("Server", Icon.GLOBE, 95.5f, 61, List.of("staff_list", "auto_reconnect", "aqua_badge", "streamer_mode")),
    MISC("Misc", Icon.GRID, 164, 61, List.of("chat_macro", "chat_style", "coord_snapper", "creamy_keys", "media_hider", "music",
            "name_protect", "screenshot_toast", "skin_protect", "slow_swing", "weather_notifier")),
    MOVEMENT("Movement", Icon.MOVE, 236.5f, 62, List.of("auto_jump", "toggle_sneak", "toggle_sprint")),
    COMBAT("Combat", Icon.CROSSHAIR, 361, 56, List.of("attack_indicator", "damage_fx", "hit_fx", "kill_effects", "swing_trails",
            "pop_effect", "projectile_trails")),
    CLIENT("Aqua Client", Icon.PIN, 434, 61, List.of("discord_presence", "hud", "keybinds", "performance_mode", "music_card",
            "account", "armor", "clock", "compass", "cps", "effects", "inventory", "island", "item_counter", "keystrokes",
            "module_list", "network", "performance", "position", "scoreboard", "watermark", "effect_budget", "unfocused_fps",
            "particle_limiter")),
    COSMETICS("Cosmetics", Icon.HANGER, 435, 159, List.of("ambience", "cape", "crystal_pulse", "dash_trail", "emotes", "fireflies",
            "floating_cubes", "glass", "halo", "hats", "jump_effects", "orbit", "pet", "player_trail", "sky_lyrics", "wind", "wings",
            "world_join_effect")),
    VISUALS("Visuals", Icon.MONITOR, 497.5f, 131, List.of("character_skin", "three_d_hand", "custom_sky", "block_outline",
            "color_grading", "knives", "crosshair", "custom_fov", "custom_glint", "floating_relics", "hand_glow", "hand_pose",
            "hit_particles", "jump_circles", "motion_blur", "particles", "pharaoh_headdress", "animations", "target_marker",
            "totem_spirit", "zoom", "hand_shader", "item_glow"));

    public final String title;
    public final Icon icon;
    public final float defaultX;
    public final float defaultY;
    public final List<String> moduleIds;
    /** Most rows shown before the panel scrolls (the client panel is kept short, as in the video). */
    public final int maxRows;

    MenuPanel(String title, Icon icon, float x, float y, List<String> ids) {
        this.title = title;
        this.icon = icon;
        this.defaultX = x;
        this.defaultY = y;
        this.moduleIds = ids;
        this.maxRows = title.equals("Aqua Client") ? 8 : 40;
    }

    /** The panel a module belongs to (modules not listed go to Misc, so none is ever missing). */
    public static MenuPanel of(String moduleId) {
        for (MenuPanel p : values()) {
            if (p.moduleIds.contains(moduleId)) {
                return p;
            }
        }
        return MISC;
    }
}
