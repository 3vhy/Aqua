package dev.aqua.client.module.cosmetic;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.world.Particles;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.setting.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.random.Random;

/** Themed sparks where you hit something (R4 0:54 "Hit Particles"). Visual only. */
public final class HitParticles extends Module {
    private final ColorSetting color = add(new ColorSetting("color", "Colour", 0xFF52E4F2, false, true));
    private final NumberSetting amount = add(new NumberSetting("amount", "Particles", 14, 4, 40, 1));
    private final Random random = Random.create();

    public HitParticles() {
        super("hit_particles", "Hit Particles", "Sparks in your theme colour when you hit.", Category.COSMETICS, false,
                "hit", "crit", "sparks", "particles");
    }

    public void onHit(Entity target) {
        int n = Math.round(amount.f() * Aqua.ui().effectIntensity.f());
        for (int i = 0; i < n; i++) {
            float sp = 1.5f + random.nextFloat() * 2.5f;
            double a = random.nextDouble() * Math.PI * 2;
            Particles.spawn(target.getX(), target.getY() + target.getHeight() * 0.55, target.getZ(),
                    (float) Math.cos(a) * sp, 0.5f + random.nextFloat() * 2f, (float) Math.sin(a) * sp,
                    0.35f + random.nextFloat() * 0.3f, 0.04f + random.nextFloat() * 0.03f, color.color(), 6f, 3.5f);
        }
    }
}
