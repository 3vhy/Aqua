package dev.aqua.client.config;

import dev.aqua.client.Aqua;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.EnumSetting;
import dev.aqua.client.setting.NumberSetting;
import dev.aqua.client.setting.Setting;

import java.util.ArrayList;
import java.util.List;

/** Client-wide preferences (stored in config/aqua/aqua.json, shared by all profiles). */
public final class UiSettings {
    public enum Quality implements EnumSetting.Displayable {
        LOW("Low"), MEDIUM("Medium"), HIGH("High");
        final String label;

        Quality(String l) {
            label = l;
        }

        public String displayName() {
            return label;
        }
    }

    public enum NotificationStyle implements EnumSetting.Displayable {
        ISLAND("Island (top centre)"), CARD("Card (bottom right)"), OFF("Off");
        final String label;

        NotificationStyle(String l) {
            label = l;
        }

        public String displayName() {
            return label;
        }
    }

    public enum Style implements EnumSetting.Displayable {
        GLASS("Glass"), SOLID("Solid");
        final String label;

        Style(String l) {
            label = l;
        }

        public String displayName() {
            return label;
        }
    }

    public enum MenuStyle implements EnumSetting.Displayable {
        PANELS("Panels"), CLASSIC("Classic window");
        final String label;

        MenuStyle(String l) {
            label = l;
        }

        public String displayName() {
            return label;
        }
    }

    public enum TitleBackground implements EnumSetting.Displayable {
        AQUA("Aqua lights"), PANORAMA("Minecraft panorama");
        final String label;

        TitleBackground(String l) {
            label = l;
        }

        public String displayName() {
            return label;
        }
    }

    private final List<Setting<?>> all = new ArrayList<>();

    public final EnumSetting<MenuStyle> menuStyle = add(new EnumSetting<>("menu_style", "Menu style", MenuStyle.PANELS)
            .describe("Panels: a floating panel per category. Classic window: one window with a sidebar"));
    /** Panel positions and open state of the panel menu (managed by the menu). */
    public final dev.aqua.client.setting.StringSetting panelLayout = add(new dev.aqua.client.setting.StringSetting("panel_layout", "Panel layout", "", 4000)
            .visibleWhen(() -> false));
    /** Friends (comma-separated names), managed on the Friends tab. */
    public final dev.aqua.client.setting.StringSetting friends = add(new dev.aqua.client.setting.StringSetting("friends", "Friends", "", 4000)
            .visibleWhen(() -> false));
    public final EnumSetting<Style> style = add(new EnumSetting<>("style", "Interface style", Style.GLASS)
            .describe("Glass: frosted panels that blur what is behind them. Solid: plain dark panels"));
    public final NumberSetting glassBlur = add(new NumberSetting("glass_blur", "Glass frost", 3, 1, 5, 1)
            .describe("How strongly glass blurs what is behind it").visibleWhen(this::glassStyle));
    public final NumberSetting glassTint = add(new NumberSetting("glass_tint", "Glass tint", 0.55, 0.15, 0.85, 0.05)
            .describe("How dark and solid the glass is (higher is easier to read on bright scenes)").visibleWhen(this::glassStyle));
    public final BoolSetting hudGlass = add(new BoolSetting("hud_glass", "Glass HUD", true)
            .describe("HUD element backgrounds are frosted glass too").visibleWhen(this::glassStyle));

    public final BoolSetting reducedMotion = add(new BoolSetting("reduced_motion", "Reduced motion", false)
            .describe("Replaces menu and HUD transitions with instant changes"));
    public final NumberSetting animationSpeed = add(new NumberSetting("animation_speed", "Animation speed", 1.0, 0.5, 2.0, 0.05)
            .unit("x").describe("Scales the duration of interface transitions"));
    public final BoolSetting blur = add(new BoolSetting("full_blur", "Blur whole background", false)
            .describe("Blurs the entire world behind Aqua menus, not just behind the glass").visibleWhen(this::glassStyle));
    public final EnumSetting<Quality> quality = add(new EnumSetting<>("quality", "Effects quality", Quality.HIGH)
            .describe("Budget for shadows, particles and animation detail"));
    public final BoolSetting shadowsSetting = add(new BoolSetting("shadows", "Soft shadows", true)
            .describe("Soft drop shadows under windows and widgets"));
    public final NumberSetting effectIntensity = add(new NumberSetting("effect_intensity", "Cosmetic intensity", 1.0, 0.0, 1.5, 0.05)
            .unit("x").describe("Master scale for cosmetic particle counts, glow and animation strength"));
    public final BoolSetting colorSync = add(new BoolSetting("color_sync", "Colour Sync", false)
            .describe("Every module colour follows the theme accent, so one colour wheel changes them all (R7)"));
    public final BoolSetting sounds = add(new BoolSetting("sounds", "Interface sounds", true));
    /** Starred modules (comma-separated ids), shown under Starred in the menu. Hidden from the Settings page. */
    public final dev.aqua.client.setting.StringSetting starred = add(new dev.aqua.client.setting.StringSetting("starred", "Starred", "", 4000)
            .visibleWhen(() -> false));

    public boolean isStarred(String id) {
        return ("," + starred.get() + ",").contains("," + id + ",");
    }

    public void toggleStar(String id) {
        java.util.LinkedHashSet<String> set = new java.util.LinkedHashSet<>();
        for (String s : starred.get().split(",")) {
            if (!s.isBlank()) {
                set.add(s.trim());
            }
        }
        if (!set.remove(id)) {
            set.add(id);
        }
        starred.set(String.join(",", set));
    }
    public final NumberSetting soundVolume = add(new NumberSetting("sound_volume", "Interface volume", 0.6, 0.0, 1.0, 0.05)
            .visibleWhen(this::soundsOn));
    public final EnumSetting<NotificationStyle> notificationStyle = add(new EnumSetting<>("notification_style",
            "Notification style", NotificationStyle.ISLAND));
    public final BoolSetting toggleNotifications = add(new BoolSetting("toggle_notifications", "Module toggle notifications", true));
    public final BoolSetting customTitle = add(new BoolSetting("custom_title", "Aqua title screen", true)
            .describe("Replace the vanilla title screen with Aqua's"));
    public final BoolSetting customPause = add(new BoolSetting("custom_pause", "Aqua pause menu", true)
            .describe("The pause menu on a glass card, with Aqua Settings, starred-module toggles and what is playing"));
    public final BoolSetting vanillaStyle = add(new BoolSetting("vanilla_style", "Aqua-style Minecraft menus", true)
            .describe("Buttons, sliders, text boxes and item tooltips in every Minecraft menu use Aqua's style"));
    public final EnumSetting<TitleBackground> titleBackground = add(new EnumSetting<>("title_background", "Title background",
            TitleBackground.AQUA).describe("What shows behind the glass on the Aqua title screen").visibleWhen(customTitle::on));
    public final NumberSetting menuScale = add(new NumberSetting("menu_scale", "Menu scale", 1.0, 0.75, 1.25, 0.05).unit("x"));
    public final BoolSetting menuDim = add(new BoolSetting("menu_dim", "Dim world behind menu", true));

    private boolean glassStyle() {
        return style.get() == Style.GLASS;
    }

    /** True when glass surfaces should be drawn (Glass style, and the blur works on this system). */
    public boolean glass() {
        return style.get() == Style.GLASS && !dev.aqua.client.render.GlassBlur.broken();
    }

    private boolean soundsOn() {
        return sounds.on();
    }

    private <S extends Setting<?>> S add(S s) {
        all.add(s);
        s.setDirtyHook(() -> Aqua.config().markDirty());
        return s;
    }

    public List<Setting<?>> all() {
        return all;
    }

    public float animationScale() {
        if (reducedMotion.on()) {
            return 0f;
        }
        return (float) (1.0 / animationSpeed.d());
    }

    public boolean shadows() {
        return shadowsSetting.on() && quality.get() != Quality.LOW;
    }

    /** Multiplier for particle budgets (quality x intensity). */
    public float particleBudget() {
        float q = switch (quality.get()) {
            case LOW -> 0.35f;
            case MEDIUM -> 0.7f;
            case HIGH -> 1f;
        };
        dev.aqua.client.module.perf.PerformanceMode pm = Aqua.modules() != null ? Aqua.modules().get(dev.aqua.client.module.perf.PerformanceMode.class) : null;
        return q * effectIntensity.f() * (pm != null ? pm.particleFactor() : 1f);
    }
}
