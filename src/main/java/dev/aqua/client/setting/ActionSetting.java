package dev.aqua.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

/** A button inside a module's settings. Not persisted. */
public final class ActionSetting extends Setting<Boolean> {
    private final Runnable action;
    private final String label;

    public ActionSetting(String id, String name, String label, Runnable action) {
        super(id, name, false);
        this.action = action;
        this.label = label;
    }

    public void run() {
        action.run();
    }

    public String label() {
        return label;
    }

    @Override
    protected Boolean validate(Boolean v) {
        return v;
    }

    @Override
    public JsonElement toJson() {
        return JsonNull.INSTANCE;
    }

    @Override
    public boolean fromJson(JsonElement e) {
        return true;
    }

    @Override
    public boolean persistent() {
        return false;
    }
}
