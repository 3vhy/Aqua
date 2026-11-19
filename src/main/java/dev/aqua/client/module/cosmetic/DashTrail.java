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
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;

/** Glowing ribbon that follows your movement (R1 "Dash Trail"). Best seen in third person. */
public final class DashTrail extends Module implements WorldEffect {
    private final ColorSetting color = add(new ColorSetting("color", "Colour", 0xFF52E4F2, false, true));
    private final NumberSetting length = add(new NumberSetting("length", "Length", 0.6, 0.2, 2.0, 0.05).unit(" s"));
    private final NumberSetting width = add(new NumberSetting("width", "Width", 0.35, 0.1, 1.0, 0.05));

    private record Point(Vec3d pos, long time) {}

    private final ArrayDeque<Point> points = new ArrayDeque<>();

    public DashTrail() {
        super("dash_trail", "Dash Trail", "A glowing ribbon trailing behind you.", Category.COSMETICS, false, "trail", "dash", "ribbon");
    }

    @Override
    protected void onDisable() {
        points.clear();
    }

    @Override
    public void renderWorld(Fx fx, VertexConsumerProvider vcp) {
        if (mc.player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        Vec3d p = mc.player.getLerpedPos(fx.tickDelta).add(0, mc.player.getHeight() * 0.55, 0);
        if (points.isEmpty() || points.peekLast().pos.squaredDistanceTo(p) > 0.0025) {
            points.addLast(new Point(p, now));
        }
        long maxAge = Math.round(length.d() * 1000);
        while (!points.isEmpty() && (now - points.peekFirst().time > maxAge || points.size() > 160)) {
            points.removeFirst();
        }
        if (points.size() < 2) {
            return;
        }
        VertexConsumer vc = vcp.getBuffer(FxLayers.GLOW);
        Point prev = null;
        int c = color.color();
        float w = width.f() * 0.5f;
        for (Point pt : points) {
            if (prev != null) {
                float a0 = 1f - (now - prev.time) / (float) maxAge;
                float a1 = 1f - (now - pt.time) / (float) maxAge;
                fx.ribbon(vc, prev.pos, pt.pos, w * a0, w * a1, Colors.fade(c, a0 * 0.55f), Colors.fade(c, a1 * 0.55f));
            }
            prev = pt;
        }
    }
}
