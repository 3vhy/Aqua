package dev.aqua.client.module.cosmetic;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.world.Fx;
import dev.aqua.client.render.world.FxLayers;
import dev.aqua.client.render.world.WorldEffect;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.setting.NumberSetting;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.math.Vec3d;

/** Glowing orbs circling your player. */
public final class Orbit extends Module implements WorldEffect {
    private final ColorSetting color = add(new ColorSetting("color", "Colour", 0xFF52E4F2, false, true));
    private final NumberSetting orbs = add(new NumberSetting("orbs", "Orbs", 3, 1, 8, 1));
    private final NumberSetting speed = add(new NumberSetting("speed", "Speed", 1.0, 0.2, 3.0, 0.1).unit("x"));

    public Orbit() {
        super("orbit", "Orbit", "Glowing orbs that circle around you.", Category.COSMETICS, false, "orbs", "spiral", "particles");
    }

    @Override
    public void renderWorld(Fx fx, VertexConsumerProvider vcp) {
        if (mc.player == null || (mc.options.getPerspective().isFirstPerson() && mc.getCameraEntity() == mc.player)) {
            return;
        }
        fx.sparks = vcp.getBuffer(FxLayers.spark());
        Vec3d p = mc.player.getLerpedPos(fx.tickDelta);
        float t = (System.currentTimeMillis() % 1000000L) / 1000f * speed.f();
        int n = orbs.i();
        for (int i = 0; i < n; i++) {
            double a = t * 2.2 + i * Math.PI * 2 / n;
            double y = 1.0 + Math.sin(t * 1.6 + i) * 0.55;
            double x = p.x + Math.cos(a) * 0.85, z = p.z + Math.sin(a) * 0.85;
            fx.spark(x, p.y + y, z, 0.09f, color.color());
            for (int k = 1; k <= 6; k++) {
                double ta = a - k * 0.16;
                double ty = 1.0 + Math.sin((t - k * 0.07) * 1.6 + i) * 0.55;
                fx.spark(p.x + Math.cos(ta) * 0.85, p.y + ty, p.z + Math.sin(ta) * 0.85, 0.06f * (1 - k / 7f), Colors.fade(color.color(), 0.7f - k * 0.1f));
            }
        }
    }
}
