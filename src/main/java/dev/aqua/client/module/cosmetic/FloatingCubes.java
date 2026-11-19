package dev.aqua.client.module.cosmetic;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.world.Fx;
import dev.aqua.client.render.world.FxLayers;
import dev.aqua.client.render.world.WorldEffect;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.setting.NumberSetting;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;

/** Translucent cubes drifting high in the sky around you (R1 "Floating Cubes"). */
public final class FloatingCubes extends Module implements WorldEffect {
    private final ColorSetting color = add(new ColorSetting("color", "Colour", 0x55FFFFFF, true, false));
    private final NumberSetting amount = add(new NumberSetting("amount", "Amount", 14, 4, 40, 1));
    private final NumberSetting size = add(new NumberSetting("size", "Size", 1.6, 0.5, 4.0, 0.1));

    private static final int MAX = 40;
    private final float[] ox = new float[MAX], oy = new float[MAX], oz = new float[MAX], ph = new float[MAX], sc = new float[MAX];

    public FloatingCubes() {
        super("floating_cubes", "Floating Cubes", "Slowly turning translucent cubes in the sky.", Category.COSMETICS, false,
                "cubes", "ambient", "sky decoration");
        Random r = Random.create(42);
        for (int i = 0; i < MAX; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            double d = 18 + r.nextDouble() * 22;
            ox[i] = (float) (Math.cos(a) * d);
            oz[i] = (float) (Math.sin(a) * d);
            oy[i] = 10 + r.nextFloat() * 18;
            ph[i] = r.nextFloat() * 10;
            sc[i] = 0.6f + r.nextFloat() * 0.8f;
        }
    }

    @Override
    public void renderWorld(Fx fx, VertexConsumerProvider vcp) {
        if (mc.player == null) {
            return;
        }
        VertexConsumer vc = vcp.getBuffer(FxLayers.COLOR);
        float time = (System.currentTimeMillis() % 10000000L) / 1000f;
        int n = Math.min(MAX, Math.round(amount.f() * Math.max(0.3f, Aqua.ui().particleBudget())));
        double px = mc.player.getX(), py = mc.player.getY(), pz = mc.player.getZ();
        for (int i = 0; i < n; i++) {
            fx.matrices.push();
            double wx = Math.floor(px / 64) * 64 + ox[i];
            double wz = Math.floor(pz / 64) * 64 + oz[i];
            // Keep cubes around the player without visible popping: wrap relative offsets.
            wx = px + ((wx - px) % 64 + 96) % 64 - 32 + ox[i] * 0.2;
            wz = pz + ((wz - pz) % 64 + 96) % 64 - 32 + oz[i] * 0.2;
            double wy = py + oy[i] + Math.sin(time * 0.4 + ph[i]) * 0.8;
            fx.matrices.translate(wx - fx.cam.x, wy - fx.cam.y, wz - fx.cam.z);
            fx.matrices.multiply(RotationAxis.POSITIVE_Y.rotation(time * 0.25f + ph[i]));
            fx.matrices.multiply(RotationAxis.POSITIVE_X.rotation(time * 0.18f + ph[i] * 0.5f));
            float s = size.f() * sc[i];
            fx.cubeLocal(vc, s * 0.5f, color.color());
            fx.cubeLocal(vc, s * 0.52f, Colors.fade(color.color(), 0.35f));
            fx.matrices.pop();
        }
    }
}
