package dev.aqua.client.module.cosmetic;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Anim;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.world.Fx;
import dev.aqua.client.render.world.FxLayers;
import dev.aqua.client.render.world.WorldEffect;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.setting.NumberSetting;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

/** Faint wind streaks drifting past you (R1 "Wind"). */
public final class Wind extends Module implements WorldEffect {
    private final ColorSetting color = add(new ColorSetting("color", "Colour", 0x66FFFFFF, true, false));
    private final NumberSetting amount = add(new NumberSetting("amount", "Streaks", 24, 4, 80, 2));
    private final NumberSetting direction = add(new NumberSetting("direction", "Direction", 45, 0, 359, 5).unit("°"));
    private final NumberSetting speed = add(new NumberSetting("speed", "Speed", 6, 1, 20, 0.5).unit(" b/s"));

    private static final int MAX = 80;
    private final double[] x = new double[MAX], y = new double[MAX], z = new double[MAX];
    private final float[] life = new float[MAX], len = new float[MAX];
    private final Random random = Random.create();
    private boolean seeded;

    public Wind() {
        super("wind", "Wind", "Soft wind streaks drifting through the air.", Category.COSMETICS, false, "wind", "breeze", "streaks");
    }

    private void spawn(int i, double cx, double cy, double cz) {
        x[i] = cx + (random.nextDouble() - 0.5) * 36;
        y[i] = cy + random.nextDouble() * 10 - 2;
        z[i] = cz + (random.nextDouble() - 0.5) * 36;
        life[i] = random.nextFloat() * 3f;
        len[i] = 1.5f + random.nextFloat() * 3f;
    }

    @Override
    public void renderWorld(Fx fx, VertexConsumerProvider vcp) {
        if (mc.player == null) {
            return;
        }
        double cx = mc.player.getX(), cy = mc.player.getY(), cz = mc.player.getZ();
        if (!seeded) {
            for (int i = 0; i < MAX; i++) {
                spawn(i, cx, cy, cz);
            }
            seeded = true;
        }
        double a = Math.toRadians(direction.d());
        float dx = (float) Math.cos(a), dz = (float) Math.sin(a);
        float sp = speed.f();
        float dt = Anim.delta();
        VertexConsumer vc = vcp.getBuffer(FxLayers.GLOW);
        int n = Math.min(MAX, Math.round(amount.f() * Aqua.ui().particleBudget()));
        for (int i = 0; i < n; i++) {
            life[i] += dt;
            if (life[i] > 3f) {
                spawn(i, cx, cy, cz);
                life[i] = 0;
            }
            x[i] += dx * sp * dt;
            z[i] += dz * sp * dt;
            float t = life[i] / 3f;
            float fade = (float) Math.sin(t * Math.PI);
            Vec3d tail = new Vec3d(x[i], y[i], z[i]);
            Vec3d head = tail.add(dx * len[i], Math.sin(life[i] * 2 + i) * 0.15, dz * len[i]);
            fx.ribbon(vc, tail, head, 0.0f, 0.025f, Colors.fade(color.color(), 0f), Colors.fade(color.color(), fade));
        }
    }
}
