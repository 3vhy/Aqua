package dev.aqua.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class EnumSetting<E extends Enum<E>> extends Setting<E> {
    private final E[] values;

    public EnumSetting(String id, String name, E def) {
        super(id, name, def);
        this.values = def.getDeclaringClass().getEnumConstants();
    }

    public E[] values() {
        return values;
    }

    public void cycle(int dir) {
        int i = (value.ordinal() + dir + values.length) % values.length;
        set(values[i]);
    }

    public boolean is(E e) {
        return value == e;
    }

    public static String display(Enum<?> e) {
        if (e instanceof Displayable d) {
            return d.displayName();
        }
        String s = e.name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    @Override
    protected E validate(E v) {
        return v;
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value.name());
    }

    @Override
    public boolean fromJson(JsonElement e) {
        if (e != null && e.isJsonPrimitive()) {
            String s = e.getAsString();
            for (E v : values) {
                if (v.name().equalsIgnoreCase(s)) {
                    set(v);
                    return true;
                }
            }
        }
        return false;
    }

    /** Optional friendly label for enum constants. */
    public interface Displayable {
        String displayName();
    }
}
