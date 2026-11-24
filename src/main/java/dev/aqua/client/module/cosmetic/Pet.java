package dev.aqua.client.module.cosmetic;

import dev.aqua.client.cosmetic.Mesh;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.world.Fx;
import dev.aqua.client.render.world.FxLayers;
import dev.aqua.client.render.world.Particles;
import dev.aqua.client.render.world.WorldEffect;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.setting.EnumSetting;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

/**
 * Pet (R8: the floating cube with a sparkle trail): a little companion that floats beside you and
 * follows you around. Only you see it.
 */
public final class Pet extends Module implements WorldEffect {
    public enum Kind implements EnumSetting.Displayable {
        CUBE("Cube"), CRYSTAL("Crystal"), ORB("Orb");
        final String l;

        Kind(String l) {
            this.l = l;
        }

        public String displayName() {
            return l;
        }
    }

    private final EnumSetting<Kind> kind = add(new EnumSetting<>("kind", "Pet", Kind.CUBE));
    private final ColorSetting color = add(new ColorSetting("color", "Colour", 0xFFFF5A5F, false, true));

    private Vec3d pos;
    private Vec3d vel = Vec3d.ZERO;
    private long last;
    private final Random random = Random.create();

    public Pet() {
        super("pet", "Pet", "A little floating companion that follows you.", Category.COSMETICS, false, "pet", "companion", "cube", "buddy");
    }

    /** Where the pet is (used by the in-game tests). */
    public Vec3d position() {
        return pos;
    }

    @Override
    public void renderWorld(Fx fx, VertexConsumerProvider vcp) {
        if (mc.player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        float dt = last == 0 ? 0.016f : Math.min(0.1f, (now - last) / 1000f);
        last = now;
        Vec3d p = mc.player.getLerpedPos(fx.tickDelta);
        double yaw = Math.toRadians(mc.player.getBodyYaw());
        // Float behind your right shoulder.
        Vec3d target = p.add(-Math.cos(yaw) * 0.85 + Math.sin(yaw) * 0.55, mc.player.getHeight() + 0.25, -Math.sin(yaw) * 0.85 - Math.cos(yaw) * 0.55);
        if (pos == null || pos.squaredDistanceTo(target) > 64) {
            pos = target;
            vel = Vec3d.ZERO;
        }
        // Critically damped spring towards the target.
        Vec3d acc = target.subtract(pos).multiply(38).subtract(vel.multiply(11));
        vel = vel.add(acc.multiply(dt));
        pos = pos.add(vel.multiply(dt));
        float time = (now % 1_000_000L) / 1000f;
        float bob = (float) Math.sin(time * 2.2f) * 0.06f;
        int c = color.color();
        if (vel.lengthSquared() > 0.5 && random.nextFloat() < 0.6f) {
            Particles.spawn(pos.x + (random.nextFloat() - 0.5) * 0.2, pos.y + bob + (random.nextFloat() - 0.5) * 0.2, pos.z + (random.nextFloat() - 0.5) * 0.2,
                    0, -0.1f, 0, 0.8f, 0.04f, random.nextBoolean() ? 0xFFFFFFFF : Colors.brighten(c, 0.3f), -0.3f, 2f);
        }
        if (mc.options.getPerspective().isFirstPerson() && pos.squaredDistanceTo(fx.cam) < 0.6) {
            return;
        }
        float light = Mesh.lightFactor(mc.getEntityRenderDispatcher().getLight(mc.player, fx.tickDelta), 0f);
        fx.matrices.push();
        fx.matrices.translate(pos.x - fx.cam.x, pos.y + bob - fx.cam.y, pos.z - fx.cam.z);
        fx.matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(time * 60f));
        fx.matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) Math.sin(time) * 15f));
        fx.matrices.scale(1 / 16f, 1 / 16f, 1 / 16f);
        Mesh m = new Mesh(fx.matrices.peek(), vcp.getBuffer(FxLayers.SOLID), light);
        switch (kind.get()) {
            case CUBE -> {
                m.box(-2.6f, -2.6f, -2.6f, 2.6f, 2.6f, 2.6f, c);
                m.box(-1.6f, -1.2f, -2.75f, -0.6f, 0.2f, -2.6f, 0xFFFFFFFF);
                m.box(0.6f, -1.2f, -2.75f, 1.6f, 0.2f, -2.6f, 0xFFFFFFFF);
                m.box(-1.2f, -0.8f, -2.8f, -0.8f, 0.0f, -2.7f, 0xFF1A1A22);
                m.box(0.8f, -0.8f, -2.8f, 1.2f, 0.0f, -2.7f, 0xFF1A1A22);
            }
            case CRYSTAL -> {
                m.cone(0, 0, -4.5f, 0, 2.6f, 4, c);
                m.cone(0, 0, 4.5f, 0, 2.6f, 4, Colors.darken(c, 0.2f));
            }
            case ORB -> {
                for (int i = 0; i < 3; i++) {
                    float r = 2.8f - i * 0.5f;
                    m.box(-r, -r * 0.6f, -r * 0.6f, r, r * 0.6f, r * 0.6f, Colors.brighten(c, i * 0.1f));
                    m.box(-r * 0.6f, -r, -r * 0.6f, r * 0.6f, r, r * 0.6f, Colors.brighten(c, i * 0.1f));
                    m.box(-r * 0.6f, -r * 0.6f, -r, r * 0.6f, r * 0.6f, r, Colors.brighten(c, i * 0.1f));
                }
            }
        }
        fx.matrices.pop();
        fx.spark(pos.x, pos.y + bob, pos.z, 0.28f, Colors.fade(c, 0.35f));
    }

    @Override
    protected void onDisable() {
        pos = null;
        last = 0;
    }
}
