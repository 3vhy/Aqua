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
            fx.ring(vc, r.pos.x, r.pos.y + 0.02, r.pos.z, rad, 0.08f + 0.06f * (1 - t), c, 40);
            fx.ring(vc, r.pos.x, r.pos.y + 0.02, r.pos.z, rad * 0.98f, 0.35f * (1 - t), Colors.fade(c, 0.25f), 40);
        }
    }
}
