package dev.aqua.client.module;

import dev.aqua.client.Aqua;
import dev.aqua.client.setting.Setting;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Base class for every Aqua feature that can be toggled. */
public abstract class Module {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    private final String id;
    private final String name;
    private final String description;
    private final Category category;
    private final String[] keywords;
    private final boolean enabledByDefault;
    private boolean enabled;
    private final Keybind bind = new Keybind();
    private final Keybind defaultBind = new Keybind();
    private final List<Setting<?>> settings = new ArrayList<>();
    private boolean showInModuleList = true;

    protected Module(String id, String name, String description, Category category, boolean enabledByDefault, String... keywords) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.enabledByDefault = enabledByDefault;
        this.enabled = false;
        this.keywords = keywords;
    }

    protected <S extends Setting<?>> S add(S setting) {
        settings.add(setting);
        setting.setDirtyHook(() -> Aqua.config().markDirty());
        return setting;
    }

    protected void defaultKey(int glfwKey) {
        defaultBind.set(net.minecraft.client.util.InputUtil.Type.KEYSYM, glfwKey);
        bind.set(defaultBind);
    }

    protected void hideFromModuleList() {
        this.showInModuleList = false;
    }

    public boolean showInModuleList() {
        return showInModuleList;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public Category category() {
        return category;
    }

    public Keybind bind() {
        return bind;
    }

    public Keybind defaultBind() {
        return defaultBind;
    }

    public List<Setting<?>> settings() {
        return Collections.unmodifiableList(settings);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean enabledByDefault() {
        return enabledByDefault;
    }

    public void toggle() {
        setEnabled(!enabled, true);
    }

    public void setEnabled(boolean on, boolean notify) {
        if (on == enabled) {
            return;
        }
        enabled = on;
        try {
            if (on) {
                onEnable();
            } else {
                onDisable();
            }
        } catch (Throwable t) {
            dev.aqua.client.AquaClient.LOGGER.error("Module {} failed to {}", id, on ? "enable" : "disable", t);
        }
        Aqua.config().markDirty();
        if (notify) {
            Aqua.notifications().moduleToggled(this);
        }
    }

    /** Restores this module's settings, key and enabled state to defaults. */
    public void resetToDefaults() {
        for (Setting<?> s : settings) {
            s.reset();
        }
        bind.set(defaultBind);
        setEnabled(enabledByDefault, false);
    }

    public boolean matches(String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String q = query.toLowerCase(Locale.ROOT).trim();
        if (name.toLowerCase(Locale.ROOT).contains(q) || description.toLowerCase(Locale.ROOT).contains(q)
                || category.title.toLowerCase(Locale.ROOT).contains(q)) {
            return true;
        }
        for (String k : keywords) {
            if (k.toLowerCase(Locale.ROOT).contains(q)) {
                return true;
            }
        }
        for (Setting<?> s : settings) {
            if (s.name().toLowerCase(Locale.ROOT).contains(q)) {
                return true;
            }
        }
        return false;
    }

    protected void onEnable() {}

    protected void onDisable() {}

    /** Called every client tick while enabled. */
    public void onTick() {}
}
