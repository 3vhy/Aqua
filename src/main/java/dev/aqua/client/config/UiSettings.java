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

    private final List<Setting<?>> all = new ArrayList<>();

    public final BoolSetting reducedMotion = add(new BoolSetting("reduced_motion", "Reduced motion", false)
            .describe("Replaces menu and HUD transitions with instant changes"));
    public final NumberSetting animationSpeed = add(new NumberSetting("animation_speed", "Animation speed", 1.0, 0.5, 2.0, 0.05)
            .unit("x").describe("Scales the duration of interface transitions"));
    public final BoolSetting blur = add(new BoolSetting("blur", "Background blur", true)
            .describe("Blurs the world behind Aqua menus (uses vanilla blur)"));
    public final EnumSetting<Quality> quality = add(new EnumSetting<>("quality", "Effects quality", Quality.HIGH)
            .describe("Budget for shadows, particles and animation detail"));
    public final BoolSetting shadowsSetting = add(new BoolSetting("shadows", "Soft shadows", true)
            .describe("Soft drop shadows under windows and widgets"));
    public final NumberSetting effectIntensity = add(new NumberSetting("effect_intensity", "Cosmetic intensity", 1.0, 0.0, 1.5, 0.05)
            .unit("x").describe("Master scale for cosmetic particle counts, glow and animation strength"));
    public final BoolSetting sounds = add(new BoolSetting("sounds", "Interface sounds", true));
    public final NumberSetting soundVolume = add(new NumberSetting("sound_volume", "Interface volume", 0.6, 0.0, 1.0, 0.05)
            .visibleWhen(this::soundsOn));
    public final EnumSetting<NotificationStyle> notificationStyle = add(new EnumSetting<>("notification_style",
            "Notification style", NotificationStyle.ISLAND));
    public final BoolSetting toggleNotifications = add(new BoolSetting("toggle_notifications", "Module toggle notifications", true));
    public final BoolSetting customTitle = add(new BoolSetting("custom_title", "Aqua title screen", true)
            .describe("Replace the vanilla title screen with Aqua's"));
    public final NumberSetting menuScale = add(new NumberSetting("menu_scale", "Menu scale", 1.0, 0.75, 1.25, 0.05).unit("x"));
    public final BoolSetting menuDim = add(new BoolSetting("menu_dim", "Dim world behind menu", true));

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
        return q * effectIntensity.f();
    }
}
