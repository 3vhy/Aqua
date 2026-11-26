package dev.aqua.client.module.cosmetic;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.world.Particles;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.setting.EnumSetting;
import dev.aqua.client.setting.NumberSetting;
import net.minecraft.util.math.random.Random;

/**
 * Particles (reference video): soft light streaks and motes rise around you. Only you see them.
 */
public final class AuraParticles extends Module {
    public enum Style implements EnumSetting.Displayable {
        STREAKS("Rising streaks"), MOTES("Glowing motes"), EMBERS("Embers");
        final String l;

        Style(String l) {
            this.l = l;
        }

        public String displayName() {
            return l;
        }
    }

    private final EnumSetting<Style> style = add(new EnumSetting<>("style", "Style", Style.STREAKS));
    private final ColorSetting color = add(new ColorSetting("color", "Colour", 0xFFE8F6FF, false, false));
    private final NumberSetting amount = add(new NumberSetting("amount", "Amount", 3, 1, 10, 1).unit(" per tick"));
    private final NumberSetting radius = add(new NumberSetting("radius", "Radius", 2.5, 0.8, 6, 0.1).unit(" blocks"));
    private final Random random = Random.create();
    private long spawned;

    public AuraParticles() {
        super("particles", "Particles", "Light streaks and motes rise around you.", Category.COSMETICS, false,
                "particles", "aura", "ambient", "streaks", "motes", "embers");
    }

    /** Particles spawned (used by the in-game tests). */
    public long spawned() {
        return spawned;
    }

    @Override
    public void onTick() {
        if (mc.player == null) {
            return;
        }
        float budget = Aqua.ui().particleBudget();
        int n = Math.max(1, Math.round((float) amount.d() * Math.min(1.5f, budget)));
        for (int i = 0; i < n; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double r = 0.6 + random.nextDouble() * radius.d();
            double x = mc.player.getX() + Math.cos(a) * r, z = mc.player.getZ() + Math.sin(a) * r;
            double y = mc.player.getY() + random.nextDouble() * 0.6;
            int c = color.color();
            switch (style.get()) {
                case STREAKS -> Particles.spawn(x, y, z, 0, 0.6f + random.nextFloat() * 0.5f, 0, 1.4f + random.nextFloat(), 0.045f, c, 0f, 0.2f);
                case MOTES -> Particles.spawn(x, y + random.nextDouble(), z, (float) (random.nextGaussian() * 0.05), 0.15f + random.nextFloat() * 0.2f,
                        (float) (random.nextGaussian() * 0.05), 2f + random.nextFloat(), 0.07f, c, 0f, 0.5f);
                case EMBERS -> Particles.spawn(x, y, z, (float) (random.nextGaussian() * 0.08), 0.35f + random.nextFloat() * 0.4f,
                        (float) (random.nextGaussian() * 0.08), 1.2f + random.nextFloat(), 0.05f, random.nextBoolean() ? 0xFFFF8A1F : 0xFFFFC24D, -0.1f, 0.8f);
            }
            spawned++;
        }
    }
}
