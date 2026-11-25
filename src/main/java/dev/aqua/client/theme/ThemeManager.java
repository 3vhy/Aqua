package dev.aqua.client.theme;

import com.google.gson.JsonObject;
import dev.aqua.client.Aqua;
import dev.aqua.client.render.Colors;

/** Holds the active palette (a preset or a user-defined custom palette). */
public final class ThemeManager {
    private String presetName = Theme.AQUA.name();
    private Theme custom = Theme.AQUA.withName("Custom");
    private Theme active = Theme.AQUA;

    private Theme glassBase;
    private Theme glassVariant;

    /**
     * The theme in use. With the Glass style, secondary text is a little brighter, because glass
     * shows a lighter, busier backdrop than solid panels do.
     */
    public Theme active() {
        Theme a = active;
        if (dev.aqua.client.Aqua.ui() == null || !dev.aqua.client.Aqua.ui().glass()) {
            return a;
        }
        if (glassBase != a) {
            glassBase = a;
            glassVariant = a.with("muted", dev.aqua.client.render.Colors.lerp(a.muted(), a.text(), 0.3f));
        }
        return glassVariant;
    }

    /** The theme exactly as chosen (no Glass adjustment), for editors. */
    public Theme chosen() {
        return active;
    }

    public String presetName() {
        return presetName;
    }

    public Theme custom() {
        return custom;
    }

    public void select(String name) {
        if ("Custom".equals(name)) {
            presetName = "Custom";
            active = custom;
        } else {
            for (Theme t : Theme.PRESETS) {
                if (t.name().equals(name)) {
                    presetName = t.name();
                    active = t;
                    break;
                }
            }
        }
        Aqua.config().markDirty();
    }

    public void setCustom(Theme t) {
        custom = t.withName("Custom");
        if ("Custom".equals(presetName)) {
            active = custom;
        }
        Aqua.config().markDirty();
    }

    public void reset() {
        presetName = Theme.AQUA.name();
        custom = Theme.AQUA.withName("Custom");
        active = Theme.AQUA;
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("preset", presetName);
        JsonObject c = new JsonObject();
        c.addProperty("background", Colors.hex(custom.background(), false));
        c.addProperty("panel", Colors.hex(custom.panel(), false));
        c.addProperty("elevated", Colors.hex(custom.elevated(), false));
        c.addProperty("accent", Colors.hex(custom.accent(), false));
        c.addProperty("accent2", Colors.hex(custom.accent2(), false));
        c.addProperty("text", Colors.hex(custom.text(), false));
        c.addProperty("muted", Colors.hex(custom.muted(), false));
        o.add("custom", c);
        return o;
    }

    public void fromJson(JsonObject o) {
        reset();
        if (o == null) {
            return;
        }
        try {
            if (o.has("custom") && o.get("custom").isJsonObject()) {
                JsonObject c = o.getAsJsonObject("custom");
                Theme d = Theme.AQUA;
                custom = new Theme("Custom",
                        col(c, "background", d.background()), col(c, "panel", d.panel()), col(c, "elevated", d.elevated()),
                        col(c, "accent", d.accent()), col(c, "accent2", d.accent2()), col(c, "text", d.text()), col(c, "muted", d.muted()));
            }
            if (o.has("preset")) {
                String p = o.get("preset").getAsString();
                presetName = Theme.AQUA.name();
                active = Theme.AQUA;
                if ("Custom".equals(p)) {
                    presetName = "Custom";
                    active = custom;
                } else {
                    for (Theme t : Theme.PRESETS) {
                        if (t.name().equals(p)) {
                            presetName = p;
                            active = t;
                        }
                    }
                }
            }
        } catch (Exception e) {
            reset();
        }
    }

    private static int col(JsonObject o, String key, int def) {
        if (!o.has(key)) {
            return def;
        }
        Integer c = Colors.parseHex(o.get(key).getAsString());
        return c == null ? def : (c | 0xFF000000);
    }
}
