package dev.aqua.client.module.cosmetic;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.world.Particles;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.setting.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.random.Random;

/** Themed burst when a totem of undying is used nearby (R2 0:09, 0:16). Vanilla particles still play. */
public final class PopEffect extends Module {
    private final ColorSetting color = add(new ColorSetting("color", "Colour", 0xFF5BE38C, false, true));
    private final NumberSetting amount = add(new NumberSetting("amount", "Particles", 60, 10, 160, 5));
    private final Random random = Random.create();

    public PopEffect() {
        super("pop_effect", "Totem Pop Effect", "A themed burst of light when a totem is used.", Category.COSMETICS, false,
                "totem", "pop", "burst");
    }

    public void onPop(Entity e) {
        int n = Math.round(amount.f() * Aqua.ui().effectIntensity.f());
        for (int i = 0; i < n; i++) {
            double th = random.nextDouble() * Math.PI * 2;
            double ph = Math.acos(2 * random.nextDouble() - 1);
            float sp = 2.5f + random.nextFloat() * 3f;
            Particles.spawn(e.getX(), e.getY() + e.getHeight() * 0.6, e.getZ(),
                    (float) (Math.sin(ph) * Math.cos(th)) * sp, (float) Math.cos(ph) * sp + 1f, (float) (Math.sin(ph) * Math.sin(th)) * sp,
                    0.7f + random.nextFloat() * 0.6f, 0.06f + random.nextFloat() * 0.05f, color.color(), 3f, 2.2f);
        }
    }
}
