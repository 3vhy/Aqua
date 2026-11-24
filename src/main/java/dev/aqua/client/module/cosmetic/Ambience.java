package dev.aqua.client.module.cosmetic;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.world.Fx;
import dev.aqua.client.render.world.FxLayers;
import dev.aqua.client.render.world.WorldEffect;
import dev.aqua.client.setting.EnumSetting;
import dev.aqua.client.setting.NumberSetting;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Ambience (R8 "Cherry Petals", R6 "Halloween"): drifting petals, autumn leaves, snow or Halloween
 * bats and embers around you. Only you see them.
 */
public final class Ambience extends Module implements WorldEffect {
    public enum Theme implements EnumSetting.Displayable {
        CHERRY("Cherry petals"), AUTUMN("Autumn leaves"), SNOW("Snow"), HALLOWEEN("Halloween");
        final String l;

        Theme(String l) {
            this.l = l;
        }

        public String displayName() {
            return l;
        }
    }

    private final EnumSetting<Theme> theme = add(new EnumSetting<>("theme", "Theme", Theme.CHERRY));
    private final NumberSetting amount = add(new NumberSetting("amount", "Amount", 90, 20, 250, 5));

    private static final class Flake {
        double x, y, z;
        float vx, vy, vz, spin, spinSpeed, size, phase;
        int color;
        boolean bat;
        float age, life;
    }

    private final List<Flake> flakes = new ArrayList<>();
    private final Random random = Random.create();
    private long last;
    private Theme lastTheme;

    public Ambience() {
        super("ambience", "Ambience", "Drifting cherry petals, autumn leaves, snow or Halloween bats around you.", Category.COSMETICS, false,
                "cherry petals", "petals", "leaves", "autumn", "snow", "halloween", "bats", "seasonal", "ambient");
    }

    private Flake spawn(Vec3d around, boolean anywhere) {
        Flake f = new Flake();
        Theme t = theme.get();
        double a = random.nextDouble() * Math.PI * 2, r = 1.5 + random.nextDouble() * 10;
        f.x = around.x + Math.cos(a) * r;
        f.z = around.z + Math.sin(a) * r;
        f.y = around.y + (anywhere ? -1 + random.nextDouble() * 9 : 6 + random.nextDouble() * 4);
        f.spin = random.nextFloat() * 6.28f;
        f.spinSpeed = 0.6f + random.nextFloat() * 1.8f;
        f.phase = random.nextFloat() * 6.28f;
        f.life = 9 + random.nextFloat() * 6;
        switch (t) {
            case CHERRY -> {
                f.vy = -0.45f - random.nextFloat() * 0.3f;
                f.size = 0.13f + random.nextFloat() * 0.08f;
                f.color = Colors.lerp(0xFFFFB7D5, 0xFFFF7FB0, random.nextFloat());
            }
            case AUTUMN -> {
                f.vy = -0.6f - random.nextFloat() * 0.4f;
                f.size = 0.15f + random.nextFloat() * 0.08f;
                int[] cs = {0xFFE0702A, 0xFFC9361F, 0xFFE9B93A, 0xFF9C4A1A};
                f.color = cs[random.nextInt(cs.length)];
            }
            case SNOW -> {
                f.vy = -0.7f - random.nextFloat() * 0.5f;
                f.size = 0.035f + random.nextFloat() * 0.04f;
                f.color = 0xFFF4FAFF;
            }
            case HALLOWEEN -> {
                f.bat = random.nextFloat() < 0.3f;
                if (f.bat) {
                    double h = random.nextDouble() * Math.PI * 2;
                    f.vx = (float) Math.cos(h) * 2.2f;
                    f.vz = (float) Math.sin(h) * 2.2f;
                    f.vy = 0;
                    f.y = around.y + 2 + random.nextDouble() * 6;
                    f.size = 0.22f + random.nextFloat() * 0.12f;
                    f.color = 0xFF1A1020;
                } else {
                    f.vy = 0.25f + random.nextFloat() * 0.35f;
                    f.y = around.y - 1 + random.nextDouble() * 3;
                    f.size = 0.04f + random.nextFloat() * 0.04f;
                    f.color = random.nextBoolean() ? 0xFFFF8A1F : 0xFFB36BFF;
                }
            }
        }
        return f;
    }

    @Override
    public void renderWorld(Fx fx, VertexConsumerProvider vcp) {
        if (mc.player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        float dt = last == 0 ? 0f : Math.min(0.1f, (now - last) / 1000f);
        last = now;
        Vec3d around = mc.player.getLerpedPos(fx.tickDelta);
        if (lastTheme != theme.get()) {
            flakes.clear();
            lastTheme = theme.get();
        }
        int want = Math.round(amount.f() * Math.min(1.5f, Aqua.ui().effectIntensity.f()));
        while (flakes.size() < want) {
            flakes.add(spawn(around, flakes.size() < want / 2 && dt == 0f || flakes.isEmpty()));
        }
        VertexConsumer solid = vcp.getBuffer(FxLayers.COLOR);
        VertexConsumer glow = vcp.getBuffer(FxLayers.GLOW);
        float time = (now % 1_000_000L) / 1000f;
        for (int i = 0; i < flakes.size(); i++) {
            Flake f = flakes.get(i);
            f.age += dt;
            float sway = (float) Math.sin(time * 1.1f + f.phase);
            f.x += (f.vx + sway * (f.bat ? 0.4f : 0.35f)) * dt;
            f.z += (f.vz + (float) Math.cos(time * 0.9f + f.phase) * (f.bat ? 0.4f : 0.3f)) * dt;
            f.y += (f.vy + (f.bat ? (float) Math.sin(time * 2 + f.phase) * 0.6f : 0f)) * dt;
            f.spin += f.spinSpeed * dt;
            double dx = f.x - around.x, dz = f.z - around.z;
            if (f.age > f.life || f.y < around.y - 3 || f.y > around.y + 16 || dx * dx + dz * dz > 20 * 20) {
                flakes.set(i, spawn(around, false));
                continue;
            }
            float fade = Math.min(1f, Math.min(f.age / 1.2f, (f.life - f.age) / 1.5f));
            switch (theme.get()) {
                case SNOW -> fx.spark(f.x, f.y, f.z, f.size, Colors.fade(f.color, 0.85f * fade));
                case HALLOWEEN -> {
                    if (f.bat) {
                        bat(fx, solid, f, time, fade);
                    } else {
                        fx.spark(f.x, f.y, f.z, f.size, Colors.fade(f.color, 0.9f * fade));
                    }
                }
                default -> petal(fx, solid, f, fade);
            }
        }
        if (glow == null) {
            flakes.clear();
        }
    }

    /** A tumbling petal or leaf: a small diamond that turns in 3D as it falls. */
    private void petal(Fx fx, VertexConsumer vc, Flake f, float fade) {
        float s = f.size;
        Vector3f a = new Vector3f((float) Math.cos(f.spin), (float) Math.sin(f.spin * 0.7f) * 0.6f, (float) Math.sin(f.spin)).normalize().mul(s);
        Vector3f b = new Vector3f((float) -Math.sin(f.spin * 1.3f), 0.8f, (float) Math.cos(f.spin * 1.3f)).normalize().mul(s * 0.6f);
        float px = (float) (f.x - fx.cam.x), py = (float) (f.y - fx.cam.y), pz = (float) (f.z - fx.cam.z);
        var m = fx.matrices.peek().getPositionMatrix();
        int c = Colors.fade(f.color, fade * 0.95f);
        vc.vertex(m, px - a.x, py - a.y, pz - a.z).color(c);
        vc.vertex(m, px - b.x, py - b.y, pz - b.z).color(Colors.darken(c, 0.12f));
        vc.vertex(m, px + a.x, py + a.y, pz + a.z).color(c);
        vc.vertex(m, px + b.x, py + b.y, pz + b.z).color(Colors.brighten(c, 0.1f));
    }

    /** A bat: body with two flapping wing triangles, flying along its heading. */
    private void bat(Fx fx, VertexConsumer vc, Flake f, float time, float fade) {
        float flap = (float) Math.sin(time * 14 + f.phase) * 0.7f;
        double heading = Math.atan2(f.vz, f.vx);
        float sideX = (float) -Math.sin(heading), sideZ = (float) Math.cos(heading);
        float fwdX = (float) Math.cos(heading), fwdZ = (float) Math.sin(heading);
        float px = (float) (f.x - fx.cam.x), py = (float) (f.y - fx.cam.y), pz = (float) (f.z - fx.cam.z);
        var m = fx.matrices.peek().getPositionMatrix();
        int c = Colors.fade(f.color, fade * 0.95f);
        float s = f.size;
        for (int side = -1; side <= 1; side += 2) {
            float tipX = px + sideX * s * 1.6f * side, tipZ = pz + sideZ * s * 1.6f * side, tipY = py + flap * s * 1.2f;
            vc.vertex(m, px + fwdX * s * 0.4f, py, pz + fwdZ * s * 0.4f).color(c);
            vc.vertex(m, tipX, tipY, tipZ).color(c);
            vc.vertex(m, px - fwdX * s * 0.3f + sideX * s * 0.6f * side, py + flap * s * 0.4f - s * 0.25f, pz - fwdZ * s * 0.3f + sideZ * s * 0.6f * side).color(c);
            vc.vertex(m, px - fwdX * s * 0.4f, py, pz - fwdZ * s * 0.4f).color(c);
        }
        fx.spark(px + fx.cam.x + fwdX * s * 0.35f, py + fx.cam.y + s * 0.1f, pz + fx.cam.z + fwdZ * s * 0.35f, s * 0.12f,
                Colors.fade(0xFFFF5A1F, fade));
    }

    /** Number of particles currently simulated (used by the in-game tests). */
    public int count() {
        return flakes.size();
    }

    @Override
    protected void onDisable() {
        flakes.clear();
        last = 0;
    }
}
