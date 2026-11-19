package dev.aqua.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class StringSetting extends Setting<String> {
    private final int maxLength;

    public StringSetting(String id, String name, String def, int maxLength) {
        super(id, name, def);
        this.maxLength = maxLength;
    }

    public int maxLength() {
        return maxLength;
    }

    @Override
    protected String validate(String v) {
        if (v == null) {
            return null;
        }
        String s = v.replaceAll("[\\p{Cntrl}]", "");
        return s.length() > maxLength ? s.substring(0, maxLength) : s;
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value);
    }

    @Override
    public boolean fromJson(JsonElement e) {
        if (e != null && e.isJsonPrimitive()) {
            set(e.getAsString());
            return true;
        }
        return false;
    }
}
