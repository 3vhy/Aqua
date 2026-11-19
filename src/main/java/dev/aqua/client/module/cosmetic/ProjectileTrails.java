package dev.aqua.client.module.cosmetic;

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
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Trails behind projectiles you threw or shot (adapted from the pearl trail in R2). It only draws
 * where the projectile has already been - there is no trajectory or landing prediction.
 */
public final class ProjectileTrails extends Module implements WorldEffect {
    private final ColorSetting color = add(new ColorSetting("color", "Colour", 0xFF5BE38C, false, true));
    private final NumberSetting length = add(new NumberSetting("length", "Length", 1.2, 0.3, 4.0, 0.1).unit(" s"));

    private record Point(Vec3d pos, long time) {}

    private final Map<Integer, ArrayDeque<Point>> trails = new HashMap<>();

    public ProjectileTrails() {
        super("projectile_trails", "Projectile Trails", "Trails behind your own arrows, pearls and other projectiles.",
                Category.COSMETICS, false, "pearl trail", "arrow trail", "projectiles");
    }

    @Override
    protected void onDisable() {
        trails.clear();
    }

    @Override
    public void onTick() {
        if (mc.world == null || mc.player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        for (Entity e : mc.world.getEntities()) {
            if (e instanceof ProjectileEntity p && p.getOwner() == mc.player && e.squaredDistanceTo(mc.player) < 160 * 160) {
                ArrayDeque<Point> d = trails.computeIfAbsent(e.getId(), k -> new ArrayDeque<>());
                Vec3d pos = e.getEntityPos();
                if (d.isEmpty() || d.peekLast().pos.squaredDistanceTo(pos) > 0.01) {
                    d.addLast(new Point(pos, now));
                    if (d.size() > 120) {
                        d.removeFirst();
                    }
                }
            }
        }
        long maxAge = Math.round(length.d() * 1000);
        Iterator<Map.Entry<Integer, ArrayDeque<Point>>> it = trails.entrySet().iterator();
        while (it.hasNext()) {
            ArrayDeque<Point> d = it.next().getValue();
            while (!d.isEmpty() && now - d.peekFirst().time > maxAge) {
                d.removeFirst();
            }
            if (d.isEmpty()) {
                it.remove();
            }
        }
    }

    @Override
    public void renderWorld(Fx fx, VertexConsumerProvider vcp) {
        long now = System.currentTimeMillis();
        long maxAge = Math.round(length.d() * 1000);
        VertexConsumer vc = vcp.getBuffer(FxLayers.GLOW);
        int c = color.color();
        for (ArrayDeque<Point> d : trails.values()) {
            Point prev = null;
            for (Point p : d) {
                if (prev != null) {
                    float a0 = 1f - (now - prev.time) / (float) maxAge;
                    float a1 = 1f - (now - p.time) / (float) maxAge;
                    fx.ribbon(vc, prev.pos, p.pos, 0.07f * a0, 0.07f * a1, Colors.fade(c, a0 * 0.8f), Colors.fade(c, a1 * 0.8f));
                }
                prev = p;
            }
        }
    }
}
