package dev.aqua.client.module.cosmetic;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.world.Fx;
import dev.aqua.client.render.world.FxLayers;
import dev.aqua.client.render.world.WorldEffect;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.setting.EnumSetting;
import dev.aqua.client.setting.NumberSetting;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;

/**
 * Floating Relics (reference video): glowing orbs float around you and leave a trail as they
 * move. Only you see them.
 */
public final class FloatingRelics extends Module implements WorldEffect {
    public enum Shape implements EnumSetting.Displayable {
        ORBS("Orbs"), GEMS("Gems");
        final String l;

        Shape(String l) {
            this.l = l;
        }

        public String displayName() {
            return l;
        }
    }

    private final EnumSetting<Shape> shape = add(new EnumSetting<>("shape", "Shape", Shape.ORBS));
    private final NumberSetting count = add(new NumberSetting("count", "Relics", 4, 1, 8, 1));
    private final ColorSetting color = add(new ColorSetting("color", "Colour", 0xFFF2C94C, false, false));
    private final NumberSetting trail = add(new NumberSetting("trail", "Trail length", 14, 0, 30, 1));

    @SuppressWarnings("unchecked")
    private final ArrayDeque<Vec3d>[] trails = new ArrayDeque[8];
    private long drawn;

    public FloatingRelics() {
        super("floating_relics", "Floating Relics", "Glowing orbs float around you and leave a trail as they move.", Category.COSMETICS, false,
                "relics", "orbs", "floating", "trail", "gems");
        for (int i = 0; i < trails.length; i++) {
            trails[i] = new ArrayDeque<>();
        }
    }

    /** Frames drawn (used by the in-game tests). */
    public long drawn() {
        return drawn;
    }

    @Override
    public void renderWorld(Fx fx, VertexConsumerProvider vcp) {
        if (mc.player == null) {
            return;
        }
        drawn++;
        Vec3d c = mc.player.getLerpedPos(fx.tickDelta).add(0, mc.player.getHeight() * 0.6, 0);
        float t = (System.currentTimeMillis() % 1_000_000L) / 1000f;
        int n = (int) count.d();
        fx.sparks = vcp.getBuffer(dev.aqua.client.render.world.FxLayers.spark());
        VertexConsumer glow = vcp.getBuffer(FxLayers.GLOW);
        int col = color.color();
        float intensity = Math.min(1.5f, Aqua.ui().effectIntensity.f());
        for (int i = 0; i < n; i++) {
            double phase = i * Math.PI * 2 / n;
            double r = 0.9 + 0.15 * Math.sin(t * 0.7 + i);
            double a = t * 0.9 + phase;
            Vec3d p = c.add(Math.cos(a) * r, Math.sin(t * 1.3 + i * 1.7) * 0.45, Math.sin(a) * r);
            ArrayDeque<Vec3d> tr = trails[i];
            tr.addFirst(p);
            while (tr.size() > Math.max(1, (int) trail.d())) {
                tr.removeLast();
            }
            Vec3d prev = null;
            int k = 0;
            for (Vec3d q : tr) {
                if (prev != null) {
                    float f = 1f - k / (float) tr.size();
                    fx.ribbon(glow, prev, q, 0.09f * f, 0.09f * f * 0.85f, Colors.fade(col, 0.55f * f * intensity), Colors.fade(col, 0.45f * f * intensity));
                }
                prev = q;
                k++;
            }
            if (shape.get() == Shape.ORBS) {
                fx.spark(p.x, p.y, p.z, 0.16f, Colors.fade(Colors.brighten(col, 0.4f), Math.min(1f, intensity)));
                fx.spark(p.x, p.y, p.z, 0.34f, Colors.fade(col, 0.35f * intensity));
            } else {
                fx.matrices.push();
                fx.matrices.translate(p.x - fx.cam.x, p.y - fx.cam.y, p.z - fx.cam.z);
                fx.matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotation(t * 2 + i));
                fx.matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotation(0.785f));
                fx.cubeLocal(glow, 0.08f, Colors.fade(col, 0.9f));
                fx.matrices.pop();
                fx.spark(p.x, p.y, p.z, 0.3f, Colors.fade(col, 0.3f * intensity));
            }
        }
    }

    @Override
    protected void onDisable() {
        for (ArrayDeque<Vec3d> t : trails) {
            t.clear();
        }
    }
}
