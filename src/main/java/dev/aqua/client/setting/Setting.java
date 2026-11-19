package dev.aqua.client.setting;

import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** A typed, validated, persisted option belonging to a module or to the client. */
public abstract class Setting<T> {
    private final String id;
    private final String name;
    private String description = "";
    protected T value;
    protected final T defaultValue;
    private BooleanSupplier visible = () -> true;
    private final List<Consumer<T>> listeners = new ArrayList<>();
    private Runnable dirtyHook = () -> {};

    protected Setting(String id, String name, T defaultValue) {
        this.id = id;
        this.name = name;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
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

    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S describe(String text) {
        this.description = text;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S visibleWhen(BooleanSupplier supplier) {
        this.visible = supplier;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S onChange(Consumer<T> listener) {
        this.listeners.add(listener);
        return (S) this;
    }

    public boolean isVisible() {
        return visible.getAsBoolean();
    }

    public T get() {
        return value;
    }

    public T defaultValue() {
        return defaultValue;
    }

    public void set(T v) {
        T validated = validate(v);
        if (validated == null) {
            return;
        }
        if (!validated.equals(this.value)) {
            this.value = validated;
            for (Consumer<T> l : listeners) {
                l.accept(validated);
            }
            dirtyHook.run();
        }
    }

    public void reset() {
        set(defaultValue);
    }

    public boolean isDefault() {
        return value.equals(defaultValue);
    }

    public void setDirtyHook(Runnable hook) {
        this.dirtyHook = hook;
    }

    /** Returns a valid value derived from {@code v}, or null if it must be rejected. */
    protected abstract T validate(T v);

    public abstract JsonElement toJson();

    /** Loads a value; malformed input keeps the current value and returns false. */
    public abstract boolean fromJson(JsonElement e);

    /** Whether this setting is stored in configuration files. */
    public boolean persistent() {
        return true;
    }
}
