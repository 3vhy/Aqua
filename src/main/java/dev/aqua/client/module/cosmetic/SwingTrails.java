package dev.aqua.client.module.cosmetic;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Anim;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.world.Fx;
import dev.aqua.client.render.world.FxLayers;
import dev.aqua.client.render.world.Particles;
import dev.aqua.client.render.world.WorldEffect;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.setting.EnumSetting;
import dev.aqua.client.setting.NumberSetting;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.Arm;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Swing Trails (R6 "Astral Weapon Trails"): a glowing arc follows each swing of your arm, in first and
 * third person. Purely visual; it does not change the swing, reach or timing.
 */
public final class SwingTrails extends Module implements WorldEffect {
    public enum Style implements EnumSetting.Displayable {
        ASTRAL("Astral"), BLADE("Blade"), RAINBOW("Rainbow");
        final String l;

        Style(String l) {
            this.l = l;
        }

        public String displayName() {
            return l;
        }
    }

    private final EnumSetting<Style> style = add(new EnumSetting<>("style", "Style", Style.ASTRAL));
    private final ColorSetting color = add(new ColorSetting("color", "Colour", 0xFFFF7A2E, false, true));
    private final NumberSetting length = add(new NumberSetting("length", "Arc length", 1.0, 0.5, 1.6, 0.05).unit("x"));
    private final BoolSetting firstPerson = add(new BoolSetting("first_person", "Show in first person", true));

    private record Swing(Vec3d eye, Vec3d fwd, Vec3d right, Vec3d up, long start, boolean leftArm) {}

    private final List<Swing> swings = new ArrayList<>();
    private boolean wasSwinging;
    private float lastProgress;

    public SwingTrails() {
        super("swing_trails", "Swing Trails", "A glowing arc follows each swing of your arm.", Category.COSMETICS, false,
                "weapon trails", "astral weapon trails", "sword trail", "swing", "slash trail");
    }

    @Override
    public void onTick() {
        if (mc.player == null) {
            return;
        }
        float p = mc.player.getHandSwingProgress(1f);
        boolean started = (mc.player.handSwinging && !wasSwinging) || (mc.player.handSwinging && p < lastProgress - 0.25f);
        wasSwinging = mc.player.handSwinging;
        lastProgress = p;
        if (started) {
            float yaw = (float) Math.toRadians(mc.player.getYaw());
            float pitch = (float) Math.toRadians(mc.player.getPitch());
            Vec3d fwd = new Vec3d(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
            Vec3d right = new Vec3d(-Math.cos(yaw), 0, -Math.sin(yaw));
            Vec3d up = right.crossProduct(fwd).normalize();
            boolean left = (mc.player.preferredHand == net.minecraft.util.Hand.OFF_HAND) == (mc.player.getMainArm() == Arm.RIGHT);
            swings.add(new Swing(mc.player.getEyePos(), fwd, right, up, System.currentTimeMillis(), left));
            while (swings.size() > 4) {
                swings.remove(0);
            }
        }
    }

    private int colorAt(float k, long now) {
        return switch (style.get()) {
            case ASTRAL -> Colors.lerp(color.color(), 0xFFFFF2C0, Math.max(0f, k - 0.6f) * 2.5f);
            case BLADE -> Colors.lerp(0xFFB9D6E8, 0xFFFFFFFF, k);
            case RAINBOW -> java.awt.Color.HSBtoRGB(((now % 3000L) / 3000f + k * 0.4f) % 1f, 0.75f, 1f) | 0xFF000000;
        };
    }

    @Override
    public void renderWorld(Fx fx, VertexConsumerProvider vcp) {
        if (mc.player == null) {
            return;
        }
        boolean fp = mc.options.getPerspective().isFirstPerson();
        if (fp && !firstPerson.on()) {
            swings.clear();
            return;
        }
        long now = System.currentTimeMillis();
        VertexConsumer glow = vcp.getBuffer(FxLayers.GLOW);
        Iterator<Swing> it = swings.iterator();
        while (it.hasNext()) {
            Swing s = it.next();
            float t = (now - s.start) / 320f;
            if (t >= 1f) {
                it.remove();
                continue;
            }
            // Follow the player while the arc fades (the arc is anchored to where you were looking).
            Vec3d base = mc.player.getLerpedPos(fx.tickDelta).add(0, mc.player.getStandingEyeHeight(), 0);
            float side = s.leftArm ? -1f : 1f;
            float dist = fp ? 0.85f : 1.0f;
            float len = length.f();
            float head = Anim.easeOutCubic(Math.min(1f, t * 1.8f));
            float tail = Anim.easeInOutCubic(Math.max(0f, (t - 0.2f) / 0.8f));
            int segs = 18;
            Vec3d prev = null;
            float prevK = 0;
            for (int i = 0; i <= segs; i++) {
                float k = i / (float) segs;
                if (k < tail || k > head) {
                    prev = null;
                    continue;
                }
                // Arc from upper outside to lower inside, like a sword swing.
                double ang = Math.toRadians(60 - k * 150 * len);
                double r = 0.75 + 0.1 * Math.sin(k * Math.PI);
                double x = side * Math.cos(ang) * r * 0.9;
                double y = Math.sin(ang) * r * 0.6 - 0.25;
                Vec3d p = base.add(s.fwd.multiply(dist + 0.15 * Math.sin(k * Math.PI))).add(s.right.multiply(x)).add(s.up.multiply(y));
                if (prev != null) {
                    float fade = (1f - t) * (float) Math.sin(Math.min(1f, (k - tail) / Math.max(0.05f, head - tail)) * Math.PI * 0.5 + 0.2);
                    int c0 = Colors.fade(colorAt(prevK, now), fade), c1 = Colors.fade(colorAt(k, now), fade);
                    float w = 0.05f + 0.08f * (float) Math.sin(k * Math.PI);
                    fx.ribbon(glow, prev, p, w, w, c0, c1);
                    fx.ribbon(glow, prev, p, w * 2.6f, w * 2.6f, Colors.fade(c0, 0.25f), Colors.fade(c1, 0.25f));
                    if (style.is(Style.ASTRAL) && i % 4 == 0 && t < 0.3f && Particles.count() < Particles.budget() - 50) {
                        Particles.spawn(p.x, p.y, p.z, (float) (Math.random() - 0.5) * 0.6f, 0.4f, (float) (Math.random() - 0.5) * 0.6f,
                                0.4f, 0.03f, c1, -0.5f, 2f);
                    }
                }
                prev = p;
                prevK = k;
            }
        }
    }

    /** Number of arcs currently drawn (used by the in-game tests). */
    public int active() {
        return swings.size();
    }

    @Override
    protected void onDisable() {
        swings.clear();
    }

    static int unused() {
        return Aqua.VERSION.length();
    }
}
