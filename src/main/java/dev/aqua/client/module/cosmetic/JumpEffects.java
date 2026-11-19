package dev.aqua.client.module.cosmetic;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.world.Particles;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.setting.NumberSetting;
import net.minecraft.util.math.random.Random;

/** Particle burst at your feet when you jump (R1 "Jump Effects"). */
public final class JumpEffects extends Module {
    private final ColorSetting color = add(new ColorSetting("color", "Colour", 0xFFA8F3FF, false, true));
    private final NumberSetting amount = add(new NumberSetting("amount", "Particles", 18, 4, 48, 1));
    private final Random random = Random.create();
    private boolean wasOnGround = true;

    public JumpEffects() {
        super("jump_effects", "Jump Effects", "A burst of sparks when you jump.", Category.COSMETICS, false, "jump", "particles", "sparks");
    }

    @Override
    public void onTick() {
        if (mc.player == null) {
            return;
        }
        boolean ground = mc.player.isOnGround();
        if (wasOnGround && !ground && mc.player.getVelocity().y > 0.2) {
            int n = Math.round(amount.f() * Aqua.ui().effectIntensity.f());
            for (int i = 0; i < n; i++) {
                double a = random.nextDouble() * Math.PI * 2;
                float sp = 1.2f + random.nextFloat() * 1.6f;
                Particles.spawn(mc.player.getX(), mc.player.getY() + 0.05, mc.player.getZ(),
                        (float) Math.cos(a) * sp, 0.6f + random.nextFloat() * 1.4f, (float) Math.sin(a) * sp,
                        0.5f + random.nextFloat() * 0.4f, 0.05f + random.nextFloat() * 0.03f, color.color(), 4f, 3f);
            }
        }
        wasOnGround = ground;
    }
}
