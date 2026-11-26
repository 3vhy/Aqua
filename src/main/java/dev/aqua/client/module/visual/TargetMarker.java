package dev.aqua.client.module.visual;

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
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * Target Marker (the reference video's "Target ESP", without seeing through walls): a glowing marker
 * on the entity you are fighting, for a few seconds after you hit it. It is hidden behind blocks
 * like everything else in the world.
 */
public final class TargetMarker extends Module implements WorldEffect {
    public enum Style implements EnumSetting.Displayable {
        RING("Spinning ring"), ORBS("Orbiting orbs"), DIAMOND("Diamond");
        final String l;

        Style(String l) {
            this.l = l;
        }

        public String displayName() {
            return l;
        }
    }

    private final EnumSetting<Style> style = add(new EnumSetting<>("style", "Style", Style.RING));
    private final ColorSetting color = add(new ColorSetting("color", "Colour", 0xFFFF4D6D, false, true));
    private final NumberSetting keep = add(new NumberSetting("keep", "Shown for", 4, 1, 10, 0.5).unit(" s"));
    @Nullable private Entity target;
    private long hitAt;
    private long drawn;

    public TargetMarker() {
        super("target_marker", "Target Marker", "Marks the entity you are fighting with a glowing ring (not through walls).",
                Category.VISUALS, false, "target", "target esp", "marker", "ring");
    }

    public void onHit(Entity e) {
        if (e instanceof LivingEntity) {
            target = e;
            hitAt = System.currentTimeMillis();
        }
    }

    @Nullable
    public Entity target() {
        return target;
    }

    /** Frames drawn (used by the in-game tests). */
    public long drawn() {
        return drawn;
    }

    @Override
    public void renderWorld(Fx fx, VertexConsumerProvider vcp) {
        Entity e = target;
        long since = System.currentTimeMillis() - hitAt;
        if (e == null || e.isRemoved() || !e.isAlive() || since > keep.d() * 1000) {
            target = null;
            return;
        }
        drawn++;
        float fade = (float) Math.min(1, Math.min(since / 150.0, (keep.d() * 1000 - since) / 400.0));
        Vec3d p = e.getLerpedPos(fx.tickDelta);
        float h = e.getHeight(), w = e.getWidth();
        float t = (System.currentTimeMillis() % 100000L) / 1000f;
        int c = color.color();
        fx.sparks = vcp.getBuffer(FxLayers.spark());
        VertexConsumer glow = vcp.getBuffer(FxLayers.GLOW);
        switch (style.get()) {
            case RING -> {
                double y = p.y + h * (0.5 + 0.45 * Math.sin(t * 2.2));
                fx.ring(glow, p.x, y, p.z, w * 0.85f, 0.06f, Colors.fade(c, fade), 40);
                fx.ring(glow, p.x, y, p.z, w * 0.85f, 0.18f, Colors.fade(c, 0.25f * fade), 40);
            }
            case ORBS -> {
                for (int i = 0; i < 4; i++) {
                    double a = t * 3 + i * Math.PI / 2;
                    double y = p.y + h * 0.5 + Math.sin(t * 2 + i) * h * 0.35;
                    fx.spark(p.x + Math.cos(a) * w * 0.9, y, p.z + Math.sin(a) * w * 0.9, 0.16f, Colors.fade(c, fade));
                }
            }
            case DIAMOND -> {
                double y = p.y + h + 0.45 + Math.sin(t * 3) * 0.08;
                fx.matrices.push();
                fx.matrices.translate(p.x - fx.cam.x, y - fx.cam.y, p.z - fx.cam.z);
                fx.matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotation(t * 2));
                fx.cubeLocal(glow, 0.13f, Colors.fade(c, 0.9f * fade));
                fx.matrices.pop();
            }
        }
    }
}
