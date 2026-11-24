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
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.Iterator;

/** Expanding rings at your feet when you jump and a shockwave when you land (R3/R4, R2 "Shockwave"). */
public final class JumpCircles extends Module implements WorldEffect {
    public enum Style implements dev.aqua.client.setting.EnumSetting.Displayable {
        CLASSIC("Classic"), RUNE("Rune circle"), ELECTRIC("Electric");
        final String l;

        Style(String l) {
            this.l = l;
        }

        public String displayName() {
            return l;
        }
    }

    private final dev.aqua.client.setting.EnumSetting<Style> style = add(new dev.aqua.client.setting.EnumSetting<>("style", "Style", Style.CLASSIC));
    private final ColorSetting color = add(new ColorSetting("color", "Colour", 0xFF52E4F2, false, true));
    private final BoolSetting onJump = add(new BoolSetting("jump", "On jump", true));
    private final BoolSetting onLand = add(new BoolSetting("land", "Landing shockwave", true));
    private final NumberSetting radius = add(new NumberSetting("radius", "Size", 1.2, 0.5, 3.0, 0.1).unit(" blocks"));
    private final NumberSetting duration = add(new NumberSetting("duration", "Duration", 700, 200, 2000, 50).unit(" ms"));

    private record Ring(Vec3d pos, long start, boolean landing) {}

    private final ArrayDeque<Ring> rings = new ArrayDeque<>();
    private boolean wasOnGround = true;
    private double fallStart;

    public JumpCircles() {
        super("jump_circles", "Jump Circles", "Rings at your feet when you jump and land.", Category.COSMETICS, false,
                "jump", "circles", "shockwave", "rings");
    }

    @Override
    public void onTick() {
        if (mc.player == null) {
            return;
        }
        boolean ground = mc.player.isOnGround();
        if (wasOnGround && !ground && mc.player.getVelocity().y > 0.2 && onJump.on()) {
            add(new Ring(mc.player.getEntityPos(), System.currentTimeMillis(), false));
            fallStart = mc.player.getY();
        }
        if (!wasOnGround && ground && onLand.on() && mc.player.fallDistance > 1.5) {
            add(new Ring(mc.player.getEntityPos(), System.currentTimeMillis(), true));
        }
        wasOnGround = ground;
    }

    private void add(Ring r) {
        rings.addLast(r);
        while (rings.size() > 12) {
            rings.removeFirst();
        }
    }

    /** R7: double ring with clock-like tick marks and a turning inner ring. */
    private void rune(Fx fx, VertexConsumer vc, Ring r, float rad, int c, float t) {
        double y = r.pos.y + 0.02;
        fx.ring(vc, r.pos.x, y, r.pos.z, rad, 0.05f, c, 56);
        fx.ring(vc, r.pos.x, y, r.pos.z, rad * 0.99f, 0.3f, Colors.fade(c, 0.22f), 56);
        fx.ring(vc, r.pos.x, y, r.pos.z, rad * 0.72f, 0.03f, c, 48);
        double spin = t * 1.4;
        for (int k = 0; k < 24; k++) {
            double a = Math.PI * 2 * k / 24 + spin;
            float len = k % 6 == 0 ? 0.12f : 0.06f;
            double r0 = rad * 0.78, r1 = r0 + len * rad;
            fx.line(vc, r.pos.x + Math.cos(a) * r0, y + 0.005, r.pos.z + Math.sin(a) * r0,
                    r.pos.x + Math.cos(a) * r1, y + 0.005, r.pos.z + Math.sin(a) * r1, 0.012f, c);
        }
        fx.spark(r.pos.x, y + 0.05, r.pos.z, 0.25f * rad, Colors.fade(c, 0.35f));
    }

    /** R8: a crackling blue-white ring that flickers. */
    private void electric(Fx fx, VertexConsumer vc, Ring r, float rad, int c, long now) {
        double y = r.pos.y + 0.03;
        int segs = 36;
        long seed = now / 45;
        java.util.Random rnd = new java.util.Random(seed * 31 + r.start);
        double prevX = 0, prevZ = 0, prevY = 0;
        for (int i = 0; i <= segs; i++) {
            double a = Math.PI * 2 * i / segs;
            double jitter = i == segs ? 0 : (rnd.nextDouble() - 0.5) * 0.18 * rad;
            double x = r.pos.x + Math.cos(a) * (rad + jitter), z = r.pos.z + Math.sin(a) * (rad + jitter);
            double yy = y + rnd.nextDouble() * 0.06;
            if (i > 0) {
                fx.line(vc, prevX, prevY, prevZ, x, yy, z, 0.02f, Colors.brighten(c, 0.4f));
                fx.line(vc, prevX, prevY, prevZ, x, yy, z, 0.07f, Colors.fade(c, 0.3f));
            }
            prevX = x;
            prevZ = z;
            prevY = yy;
        }
        fx.ring(vc, r.pos.x, y - 0.01, r.pos.z, rad * 1.02f, 0.35f, Colors.fade(c, 0.18f), 40);
    }

    @Override
    public void renderWorld(Fx fx, VertexConsumerProvider vcp) {
        long now = System.currentTimeMillis();
        VertexConsumer vc = vcp.getBuffer(FxLayers.GLOW);
        float dur = duration.f();
        Iterator<Ring> it = rings.iterator();
        while (it.hasNext()) {
            Ring r = it.next();
            float t = (now - r.start) / (r.landing ? dur * 1.3f : dur);
            if (t >= 1f) {
                it.remove();
                continue;
            }
            float e = Anim.easeOutCubic(t);
            float rad = radius.f() * (r.landing ? 1.6f : 1f) * (0.25f + 0.75f * e) * Math.min(1.5f, Aqua.ui().effectIntensity.f() + 0.3f);
            int c = Colors.fade(color.color(), (1f - t) * 0.9f);
            switch (style.get()) {
                case CLASSIC -> {
                    fx.ring(vc, r.pos.x, r.pos.y + 0.02, r.pos.z, rad, 0.08f + 0.06f * (1 - t), c, 40);
                    fx.ring(vc, r.pos.x, r.pos.y + 0.02, r.pos.z, rad * 0.98f, 0.35f * (1 - t), Colors.fade(c, 0.25f), 40);
                }
                case RUNE -> rune(fx, vc, r, rad, c, t);
                case ELECTRIC -> electric(fx, vc, r, rad, c, now);
            }
        }
    }
}
