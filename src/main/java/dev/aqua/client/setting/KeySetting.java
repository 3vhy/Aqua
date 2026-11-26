package dev.aqua.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.aqua.client.module.Keybind;
import net.minecraft.client.util.InputUtil;

/**
 * A key or mouse button a module reacts to (for example each Chat Macro). Stored as "KEYSYM:code"
 * or "MOUSE:code"; empty means no key.
 */
public final class KeySetting extends Setting<String> {
    public KeySetting(String id, String name) {
        super(id, name, "");
    }

    public Keybind keybind() {
        String v = get();
        int i = v.indexOf(':');
        if (i < 0) {
            return new Keybind();
        }
        try {
            InputUtil.Type t = v.startsWith("MOUSE") ? InputUtil.Type.MOUSE : InputUtil.Type.KEYSYM;
            return new Keybind(t, Integer.parseInt(v.substring(i + 1)));
        } catch (NumberFormatException e) {
            return new Keybind();
        }
    }

    public void setKey(InputUtil.Type type, int code) {
        set((type == InputUtil.Type.MOUSE ? "MOUSE:" : "KEYSYM:") + code);
    }

    public void clear() {
        set("");
    }

    public boolean matches(InputUtil.Type type, int code) {
        return keybind().matches(type, code);
    }

    public String display() {
        Keybind k = keybind();
        return k.isBound() ? k.display() : "None";
    }

    @Override
    protected String validate(String v) {
        if (v == null) {
            return null;
        }
        return v.isEmpty() || v.matches("(KEYSYM|MOUSE):-?\\d{1,4}") ? v : null;
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value);
    }

    @Override
    public boolean fromJson(JsonElement e) {
        if (e != null && e.isJsonPrimitive()) {
            String before = get();
            set(e.getAsString());
            return !get().equals(before) || e.getAsString().equals(before);
        }
        return false;
    }
}
