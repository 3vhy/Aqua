package dev.aqua.client.module.cosmetic;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Anim;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.world.Fx;
import dev.aqua.client.render.world.FxLayers;
import dev.aqua.client.render.world.WorldEffect;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.setting.NumberSetting;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.math.random.Random;

/** Ambient glowing specks drifting around you (R1 0:18 "Fireflies", R3 pink particles). */
public final class Fireflies extends Module implements WorldEffect {
    private final ColorSetting color = add(new ColorSetting("color", "Colour", 0xFFFFE27A, false, false));
    private final NumberSetting amount = add(new NumberSetting("amount", "Amount", 60, 10, 200, 5));
    private final NumberSetting radius = add(new NumberSetting("radius", "Radius", 14, 6, 32, 1).unit(" blocks"));
    private final BoolSetting nightOnly = add(new BoolSetting("night", "Night only", false));

    private static final int MAX = 200;
    private final double[] x = new double[MAX], y = new double[MAX], z = new double[MAX];
    private final float[] phase = new float[MAX], vx = new float[MAX], vy = new float[MAX], vz = new float[MAX];
    private boolean seeded;
    private final Random random = Random.create();

    public Fireflies() {
        super("fireflies", "Fireflies", "Glowing specks floating around you.", Category.COSMETICS, false, "fireflies", "ambient", "particles");
    }

    @Override
    protected void onEnable() {
        seeded = false;
    }

    private void respawn(int i, double cx, double cy, double cz, float r) {
        double a = random.nextDouble() * Math.PI * 2;
        double d = Math.sqrt(random.nextDouble()) * r;
        x[i] = cx + Math.cos(a) * d;
        z[i] = cz + Math.sin(a) * d;
        y[i] = cy - 1 + random.nextDouble() * 6;
        phase[i] = random.nextFloat() * 10f;
        vx[i] = (random.nextFloat() - 0.5f) * 0.6f;
        vy[i] = (random.nextFloat() - 0.5f) * 0.25f;
        vz[i] = (random.nextFloat() - 0.5f) * 0.6f;
    }

    @Override
    public void renderWorld(Fx fx, VertexConsumerProvider vcp) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        if (nightOnly.on()) {
            long t = mc.world.getTimeOfDay() % 24000L;
            if (t < 12500 || t > 23500) {
                return;
            }
        }
        double cx = mc.player.getX(), cy = mc.player.getY(), cz = mc.player.getZ();
        float r = radius.f();
        int n = Math.min(MAX, Math.round(amount.f() * Aqua.ui().particleBudget()));
        if (!seeded) {
            for (int i = 0; i < MAX; i++) {
                respawn(i, cx, cy, cz, r);
            }
            seeded = true;
        }
        fx.sparks = vcp.getBuffer(FxLayers.spark());
        float dt = Anim.delta();
        float time = (System.currentTimeMillis() % 1000000L) / 1000f;
        int base = color.color();
        for (int i = 0; i < n; i++) {
            vx[i] += (float) Math.sin(time * 0.7f + phase[i]) * 0.02f;
            vz[i] += (float) Math.cos(time * 0.6f + phase[i] * 1.3f) * 0.02f;
            vx[i] *= 0.99f;
            vz[i] *= 0.99f;
            x[i] += vx[i] * dt;
            y[i] += vy[i] * dt * (float) Math.sin(time + phase[i]);
            z[i] += vz[i] * dt;
            double dx = x[i] - cx, dz = z[i] - cz;
            if (dx * dx + dz * dz > r * r * 1.2 || Math.abs(y[i] - cy) > 8) {
                respawn(i, cx, cy, cz, r);
            }
            float pulse = 0.35f + 0.65f * (0.5f + 0.5f * (float) Math.sin(time * 2.1f + phase[i] * 3f));
            fx.spark(x[i], y[i], z[i], 0.07f, Colors.fade(base, pulse));
        }
    }
}
