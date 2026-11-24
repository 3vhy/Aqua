package dev.aqua.client.module.visual;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.NumberSetting;

/**
 * Low Fire and Low Shield (R8): moves the first-person fire overlay and a held shield lower so they
 * cover less of the screen. Fire still shows and the shield still blocks exactly as before.
 */
public final class LowOverlays extends Module {
    private final BoolSetting fire = add(new BoolSetting("fire", "Low fire", true));
    private final NumberSetting fireHeight = add(new NumberSetting("fire_height", "Fire lowered by", 0.3, 0.1, 0.5, 0.05)
            .visibleWhen(fire::on));
    private final BoolSetting shield = add(new BoolSetting("shield", "Low shield", true));
    private final NumberSetting shieldHeight = add(new NumberSetting("shield_height", "Shield lowered by", 0.2, 0.05, 0.4, 0.05)
            .visibleWhen(shield::on));

    public LowOverlays() {
        super("low_overlays", "Low Fire & Shield", "Lowers the burning overlay and your held shield in first person.", Category.VISUALS, false,
                "low fire", "low shield", "fire+", "fire overlay", "shield");
    }

    public float fireOffset() {
        return isEnabled() && fire.on() ? fireHeight.f() : 0f;
    }

    public float shieldOffset() {
        return isEnabled() && shield.on() ? shieldHeight.f() : 0f;
    }
}
