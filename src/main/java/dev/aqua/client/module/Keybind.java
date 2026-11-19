package dev.aqua.client.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.util.InputUtil;

/** A key or mouse button bound to an Aqua action. */
public final class Keybind {
    public static final int NONE = -1;

    private InputUtil.Type type = InputUtil.Type.KEYSYM;
    private int code = NONE;

    public Keybind() {}

    public Keybind(InputUtil.Type type, int code) {
        this.type = type;
        this.code = code;
    }

    public boolean isBound() {
        return code != NONE;
    }

    public InputUtil.Type type() {
        return type;
    }

    public int code() {
        return code;
    }

    public void set(InputUtil.Type type, int code) {
        this.type = type;
        this.code = code;
    }

    public void set(Keybind other) {
        set(other.type, other.code);
    }

    public void clear() {
        this.code = NONE;
        this.type = InputUtil.Type.KEYSYM;
    }

    public boolean matches(InputUtil.Type t, int c) {
        return isBound() && type == t && code == c;
    }

    public boolean sameAs(Keybind o) {
        return o != null && isBound() && o.isBound() && type == o.type && code == o.code;
    }

    public InputUtil.Key key() {
        return type.createFromCode(code);
    }

    public String display() {
        if (!isBound()) {
            return "None";
        }
        try {
            String s = key().getLocalizedText().getString();
            return s.length() > 14 ? s.substring(0, 14) : s;
        } catch (Exception e) {
            return "?";
        }
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("type", type.name());
        o.addProperty("code", code);
        return o;
    }

    public boolean fromJson(JsonElement e) {
        if (e == null || !e.isJsonObject()) {
            return false;
        }
        try {
            JsonObject o = e.getAsJsonObject();
            InputUtil.Type t = InputUtil.Type.valueOf(o.get("type").getAsString());
            int c = o.get("code").getAsInt();
            if (t == InputUtil.Type.SCANCODE || c < -1 || c > 512) {
                return false;
            }
            set(t, c);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
