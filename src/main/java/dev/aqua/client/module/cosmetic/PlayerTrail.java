package dev.aqua.client.module.cosmetic;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.world.Fx;
import dev.aqua.client.render.world.FxLayers;
import dev.aqua.client.render.world.Particles;
import dev.aqua.client.render.world.WorldEffect;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.setting.EnumSetting;
import dev.aqua.client.setting.NumberSetting;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.Iterator;

/**
 * Player Trail (R6 "Player Trail", R7 sparkle trail, R8 icy footprints and afterimages): marks where
 * you walked. Only you see it.
 */
public final class PlayerTrail extends Module implements WorldEffect {
    public enum Style implements EnumSetting.Displayable {
        FOOTPRINTS("Ice footprints"), AFTERIMAGES("Afterimages"), SPARKLES("Sparkles");
        final String l;

        Style(String l) {
            this.l = l;
        }

        public String displayName() {
            return l;
        }
    }

    private final EnumSetting<Style> style = add(new EnumSetting<>("style", "Style", Style.FOOTPRINTS));
    private final ColorSetting color = add(new ColorSetting("color", "Colour", 0xFF7FDBFF, false, true));
    private final NumberSetting lifetime = add(new NumberSetting("lifetime", "Lasts", 3.0, 0.5, 8.0, 0.5).unit(" s"));

    private record Print(Vec3d pos, float yaw, boolean left, long time) {}

    private record Ghost(Vec3d pos, float bodyYaw, float headYaw, float limb, float limbAmount, boolean sneaking, long time) {}

    private final ArrayDeque<Print> prints = new ArrayDeque<>();
    private final ArrayDeque<Ghost> ghosts = new ArrayDeque<>();
    private Vec3d lastPrint;
    private boolean nextLeft;
    private long lastGhost;
    private final Random random = Random.create();

    public PlayerTrail() {
        super("player_trail", "Player Trail", "Ice footprints, afterimages or sparkles where you walk (you only).", Category.COSMETICS, false,
                "trail", "player trail", "footprints", "afterimage", "ghost", "sparkles", "stars");
    }

    @Override
    public void onTick() {
        var p = mc.player;
        if (p == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long life = (long) (lifetime.f() * 1000);
        while (!prints.isEmpty() && now - prints.peekFirst().time > life) {
            prints.removeFirst();
        }
        while (!ghosts.isEmpty() && now - ghosts.peekFirst().time > Math.min(life, 900)) {
            ghosts.removeFirst();
        }
        Vec3d pos = p.getEntityPos();
        double speed = p.getVelocity().horizontalLength();
        switch (style.get()) {
            case FOOTPRINTS -> {
                if (!p.isOnGround()) {
                    break;
                }
                if (lastPrint == null) {
                    lastPrint = pos;
                } else if (pos.subtract(lastPrint).horizontalLength() > 0.55) {
                    float yaw = p.getBodyYaw();
                    double rad = Math.toRadians(yaw);
                    double sx = Math.cos(rad) * 0.14 * (nextLeft ? 1 : -1), sz = Math.sin(rad) * 0.14 * (nextLeft ? 1 : -1);
                    prints.addLast(new Print(pos.add(sx, 0.015, sz), yaw, nextLeft, now));
                    nextLeft = !nextLeft;
                    lastPrint = pos;
                    while (prints.size() > 80) {
                        prints.removeFirst();
                    }
                }
            }
            case AFTERIMAGES -> {
                if (speed > 0.06 && now - lastGhost > 110) {
                    lastGhost = now;
                    ghosts.addLast(new Ghost(pos, p.getBodyYaw(), p.getHeadYaw(), p.limbAnimator.getAnimationProgress(1f),
                            p.limbAnimator.getAmplitude(1f), p.isInSneakingPose(), now));
                    while (ghosts.size() > 10) {
                        ghosts.removeFirst();
                    }
                }
            }
            case SPARKLES -> {
                if (speed > 0.04 || !p.isOnGround()) {
                    int n = 2;
                    for (int i = 0; i < n; i++) {
                        double a = random.nextDouble() * Math.PI * 2;
                        double r = 0.25 + random.nextDouble() * 0.25;
                        int c = random.nextFloat() < 0.25f ? 0xFFFFFFFF : color.color();
                        Particles.spawn(pos.x + Math.cos(a) * r, pos.y + 0.1 + random.nextDouble() * 1.4, pos.z + Math.sin(a) * r,
                                (float) (random.nextGaussian() * 0.1), 0.15f + random.nextFloat() * 0.3f, (float) (random.nextGaussian() * 0.1),
                                0.7f + random.nextFloat() * 0.6f, 0.05f + random.nextFloat() * 0.05f, c, -0.2f, 1.5f);
                    }
                }
            }
        }
    }

    @Override
    public void renderWorld(Fx fx, VertexConsumerProvider vcp) {
        long now = System.currentTimeMillis();
        float life = lifetime.f() * 1000f;
        int col = color.color();
        if (!prints.isEmpty()) {
            VertexConsumer glow = vcp.getBuffer(FxLayers.GLOW);
            VertexConsumer solid = vcp.getBuffer(FxLayers.COLOR);
            for (Print pr : prints) {
                float a = 1f - (now - pr.time) / life;
                if (a <= 0) {
                    continue;
                }
                fx.matrices.push();
                fx.matrices.translate(pr.pos.x - fx.cam.x, pr.pos.y - fx.cam.y, pr.pos.z - fx.cam.z);
                fx.matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(-pr.yaw));
                Matrix4f m = fx.matrices.peek().getPositionMatrix();
                float mirror = pr.left ? -1 : 1;
                // sole and toes as flat crystal shapes
                flat(solid, m, mirror, -0.055f, -0.13f, 0.055f, 0.06f, Colors.fade(Colors.brighten(col, 0.35f), a * 0.8f));
                flat(solid, m, mirror, -0.045f, 0.08f, 0.045f, 0.15f, Colors.fade(Colors.brighten(col, 0.35f), a * 0.8f));
                flat(glow, m, mirror, -0.09f, -0.17f, 0.09f, 0.19f, Colors.fade(col, a * 0.35f));
                fx.matrices.pop();
                if (a > 0.7f && random.nextFloat() < 0.04f) {
                    fx.spark(pr.pos.x, pr.pos.y + 0.05, pr.pos.z, 0.05f, Colors.fade(0xFFFFFFFF, a));
                }
            }
        }
        if (!ghosts.isEmpty()) {
            VertexConsumer vc = vcp.getBuffer(FxLayers.GLOW);
            for (Ghost g : ghosts) {
                float a = 1f - (now - g.time) / Math.min(life, 900f);
                if (a <= 0) {
                    continue;
                }
                drawGhost(fx, vc, g, Colors.fade(col, a * 0.28f));
            }
        }
    }

    private static void flat(VertexConsumer vc, Matrix4f m, float mirror, float x0, float z0, float x1, float z1, int c) {
        x0 *= mirror;
        x1 *= mirror;
        vc.vertex(m, x0, 0, z0).color(c);
        vc.vertex(m, x0, 0, z1).color(c);
        vc.vertex(m, x1, 0, z1).color(c);
        vc.vertex(m, x1, 0, z0).color(c);
    }

    /** A see-through glowing copy of the player body (head, body, arms and legs posed like the walk). */
    private void drawGhost(Fx fx, VertexConsumer vc, Ghost g, int c) {
        fx.matrices.push();
        fx.matrices.translate(g.pos.x - fx.cam.x, g.pos.y - fx.cam.y, g.pos.z - fx.cam.z);
        fx.matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(180f - g.bodyYaw));
        float s = 1 / 16f;
        fx.matrices.scale(s, s, s);
        float swing = MathHelper.cos(g.limb * 0.6662f) * 1.4f * g.limbAmount;
        float lift = g.sneaking ? -3 : 0;
        part(fx, vc, 0, 24 + lift, 0, 0, -4, 0, -4, 4, 8, 4, c);           // head
        part(fx, vc, 0, 12 + lift, 0, 0, -4, 0, -2, 4, 12, 2, c);          // body
        part(fx, vc, -6, 22 + lift, 0, swing, -2, -10, -2, 2, 2, 2, c);    // right arm
        part(fx, vc, 6, 22 + lift, 0, -swing, -2, -10, -2, 2, 2, 2, c);    // left arm
        part(fx, vc, -2, 12, 0, -swing, -2, -12, -2, 2, 0, 2, c);          // right leg
        part(fx, vc, 2, 12, 0, swing, -2, -12, -2, 2, 0, 2, c);            // left leg
        fx.matrices.pop();
    }

    private static void part(Fx fx, VertexConsumer vc, float px, float py, float pz, float pitch, float x0, float y0, float z0,
                             float x1, float y1, float z1, int c) {
        fx.matrices.push();
        fx.matrices.translate(px, py, pz);
        fx.matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotation(pitch));
        Matrix4f m = fx.matrices.peek().getPositionMatrix();
        float[][] f = {
                {x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0}, {x0, y0, z1, x0, y1, z1, x1, y1, z1, x1, y0, z1},
                {x0, y0, z0, x0, y1, z0, x0, y1, z1, x0, y0, z1}, {x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0},
                {x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1}, {x0, y0, z0, x0, y0, z1, x1, y0, z1, x1, y0, z0}};
        for (float[] q : f) {
            vc.vertex(m, q[0], q[1], q[2]).color(c);
            vc.vertex(m, q[3], q[4], q[5]).color(c);
            vc.vertex(m, q[6], q[7], q[8]).color(c);
            vc.vertex(m, q[9], q[10], q[11]).color(c);
        }
        fx.matrices.pop();
    }

    /** Footprints and afterimages currently shown (used by the in-game tests). */
    public int marks() {
        return prints.size() + ghosts.size();
    }

    @Override
    protected void onDisable() {
        prints.clear();
        ghosts.clear();
        lastPrint = null;
    }

    static float unused() {
        return Aqua.ui().effectIntensity.f();
    }
}
