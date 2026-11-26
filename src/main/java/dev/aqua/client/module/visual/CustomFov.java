package dev.aqua.client.module.visual;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.NumberSetting;

/** Custom Fov (reference video): your field of view, including values beyond the vanilla slider. */
public final class CustomFov extends Module {
    private final NumberSetting fov = add(new NumberSetting("fov", "Field of view", 90, 30, 140, 1).unit("°"));

    public CustomFov() {
        super("custom_fov", "Custom Fov", "Changes your field of view (FOV).", Category.VISUALS, false, "fov", "field of view", "custom fov");
    }

    /** The base FOV to use instead of the options value. */
    public int fov() {
        return (int) Math.round(fov.d());
    }
}
