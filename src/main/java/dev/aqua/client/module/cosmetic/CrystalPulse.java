package dev.aqua.client.module.cosmetic;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.world.Fx;
import dev.aqua.client.render.world.FxLayers;
import dev.aqua.client.render.world.WorldEffect;
import dev.aqua.client.setting.ColorSetting;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;

/**
 * Pearlescent halo rings on end crystals (R4 0:54 "Crystal Pulse"), adapted: rings are depth tested so
 * they never show crystals through walls.
 */
public final class CrystalPulse extends Module implements WorldEffect {
    private final ColorSetting color = add(new ColorSetting("color", "Colour", 0xFFDCC9FF, false, false));

    public CrystalPulse() {
        super("crystal_pulse", "Crystal Pulse", "Pulsing halos on end crystals you can see.", Category.COSMETICS, false,
                "end crystal", "halo", "pulse");
    }

    @Override
    public void renderWorld(Fx fx, VertexConsumerProvider vcp) {
        if (mc.world == null || mc.player == null) {
            return;
        }
        VertexConsumer vc = vcp.getBuffer(FxLayers.GLOW);
        float time = (System.currentTimeMillis() % 100000L) / 1000f;
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof EndCrystalEntity) || e.squaredDistanceTo(mc.player) > 64 * 64) {
                continue;
            }
            double x = e.getLerpedPos(fx.tickDelta).x, y = e.getLerpedPos(fx.tickDelta).y, z = e.getLerpedPos(fx.tickDelta).z;
            for (int k = 0; k < 3; k++) {
                float t = (time * 0.6f + k / 3f) % 1f;
                int c = Colors.fade(Colors.lerp(color.color(), 0xFFFFFFFF, t * 0.4f), (1f - t) * 0.8f);
                fx.ring(vc, x, y + 0.2 + t * 1.6, z, 0.6f + t * 0.9f, 0.05f, c, 32);
            }
        }
    }
}
