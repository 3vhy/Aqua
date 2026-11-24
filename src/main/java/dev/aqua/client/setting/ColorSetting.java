package dev.aqua.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.aqua.client.Aqua;
import dev.aqua.client.render.Colors;

/** ARGB colour, optionally following the active theme's accent colour. */
public final class ColorSetting extends Setting<Integer> {
    private final boolean allowAlpha;
    private boolean syncTheme;
    private final boolean defaultSync;

    public ColorSetting(String id, String name, int def, boolean allowAlpha, boolean syncWithTheme) {
        super(id, name, def);
        this.allowAlpha = allowAlpha;
        this.syncTheme = syncWithTheme;
        this.defaultSync = syncWithTheme;
    }

    public ColorSetting(String id, String name, int def) {
        this(id, name, def, false, false);
    }

    public boolean allowAlpha() {
        return allowAlpha;
    }

    public boolean syncTheme() {
        return syncTheme;
    }

    public void setSyncTheme(boolean s) {
        this.syncTheme = s;
        set(value);
    }

    /**
     * The effective colour: the theme accent when this setting follows the theme, or when Colour Sync
     * (Settings) makes every colour follow it. The setting's own alpha is kept.
     */
    public int color() {
        if (syncTheme() || (Aqua.ui() != null && Aqua.ui().colorSync.on())) {
            return Colors.withAlpha(Aqua.theme().accent(), Colors.alpha(value));
        }
        return value;
    }

    @Override
    public void reset() {
        this.syncTheme = defaultSync;
        super.reset();
    }

    @Override
    public boolean isDefault() {
        return super.isDefault() && syncTheme == defaultSync;
    }

    @Override
    protected Integer validate(Integer v) {
        if (v == null) {
            return null;
        }
        return allowAlpha ? v : (v | 0xFF000000);
    }

    @Override
    public JsonElement toJson() {
        JsonObject o = new JsonObject();
        o.add("argb", new JsonPrimitive(Colors.hex(value, true)));
        o.add("theme", new JsonPrimitive(syncTheme));
        return o;
    }

    @Override
    public boolean fromJson(JsonElement e) {
        if (e != null && e.isJsonObject()) {
            JsonObject o = e.getAsJsonObject();
            Integer c = o.has("argb") ? Colors.parseHex(o.get("argb").getAsString()) : null;
            if (c == null) {
                return false;
            }
            this.syncTheme = o.has("theme") && o.get("theme").getAsBoolean();
            set(c);
            return true;
        }
        return false;
    }
}
