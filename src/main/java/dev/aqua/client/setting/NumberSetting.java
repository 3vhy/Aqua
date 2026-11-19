package dev.aqua.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class NumberSetting extends Setting<Double> {
    private final double min;
    private final double max;
    private final double step;
    private String unit = "";
    private int decimals;

    public NumberSetting(String id, String name, double def, double min, double max, double step) {
        super(id, name, def);
        this.min = min;
        this.max = max;
        this.step = step;
        this.decimals = step >= 1 ? 0 : (step >= 0.1 ? 1 : 2);
    }

    public NumberSetting unit(String unit) {
        this.unit = unit;
        return this;
    }

    public NumberSetting decimals(int d) {
        this.decimals = d;
        return this;
    }

    public double min() {
        return min;
    }

    public double max() {
        return max;
    }

    public double step() {
        return step;
    }

    public float f() {
        return value.floatValue();
    }

    public int i() {
        return (int) Math.round(value);
    }

    public double d() {
        return value;
    }

    public String format() {
        String s = decimals == 0 ? String.valueOf(Math.round(value)) : String.format(java.util.Locale.ROOT, "%." + decimals + "f", value);
        return s + unit;
    }

    public double normalized() {
        return (value - min) / (max - min);
    }

    public void setNormalized(double t) {
        set(min + (max - min) * Math.max(0, Math.min(1, t)));
    }

    @Override
    protected Double validate(Double v) {
        if (v == null || v.isNaN() || v.isInfinite()) {
            return null;
        }
        double c = Math.max(min, Math.min(max, v));
        if (step > 0) {
            c = min + Math.round((c - min) / step) * step;
            c = Math.max(min, Math.min(max, c));
            c = Math.round(c * 1e6) / 1e6;
        }
        return c;
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value);
    }

    @Override
    public boolean fromJson(JsonElement e) {
        if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) {
            set(e.getAsDouble());
            return true;
        }
        return false;
    }
}
