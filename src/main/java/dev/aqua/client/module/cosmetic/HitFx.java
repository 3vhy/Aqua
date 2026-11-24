package dev.aqua.client.module.cosmetic;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Anim;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.R2D;
import dev.aqua.client.render.font.AquaFont;
import dev.aqua.client.render.world.Fx;
import dev.aqua.client.render.world.FxLayers;
import dev.aqua.client.render.world.Particles;
import dev.aqua.client.render.world.WorldEffect;
import dev.aqua.client.render.world.WorldText;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.setting.EnumSetting;
import dev.aqua.client.setting.NumberSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Hit FX (R6 "HitFX", "TargetAura", "Damage Markers", "Hit Sounds"): effects when you hit something - a
 * hit marker on the crosshair, a slash or burst at the hit, a fading ring at the target's feet, a
 * floating HIT/CRIT marker and a sound only you hear. Visual and local only: it shows no health,
 * damage numbers or anything the game does not already show, and the ring fades after the hit.
 */
public final class HitFx extends Module implements WorldEffect {
    public enum Style implements EnumSetting.Displayable {
        SLASH("Slash"), BURST("Burst"), STAR("Star"), NONE("None");
        final String l;

        Style(String l) {
            this.l = l;
        }

        public String displayName() {
            return l;
        }
    }

    public enum Sound implements EnumSetting.Displayable {
        NONE("None"), CLICK("Click"), DING("Ding"), CHIME("Chime"), BELL("Bell"), PLING("Pling"), BONK("Bonk"), CRYSTAL("Crystal"),
        ARROW("Arrow hit");
        final String l;

        Sound(String l) {
            this.l = l;
        }

        public String displayName() {
            return l;
        }
    }

    private final EnumSetting<Style> style = add(new EnumSetting<>("style", "Effect", Style.SLASH));
    private final BoolSetting marker = add(new BoolSetting("marker", "Hit marker on crosshair", true));
    private final BoolSetting ring = add(new BoolSetting("ring", "Ring at target's feet", true)
            .describe("A ring that fades out a second after each hit (R6 \"TargetAura\")"));
    private final BoolSetting floating = add(new BoolSetting("floating", "Floating HIT / CRIT markers", true)
            .describe("Shows that you hit or landed a critical hit; no damage numbers or health"));
    private final EnumSetting<Sound> sound = add(new EnumSetting<>("sound", "Hit sound", Sound.CLICK));
    private final NumberSetting volume = add(new NumberSetting("volume", "Sound volume", 0.6, 0.0, 1.0, 0.05)
            .visibleWhen(() -> !sound.is(Sound.NONE)));
    private final ColorSetting color = add(new ColorSetting("color", "Colour", 0xFF52E4F2, false, true));
    private final ColorSetting critColor = add(new ColorSetting("crit_color", "Critical hit colour", 0xFFFFC23D, false, false));

    private record Hit(Entity target, Vec3d at, boolean crit, long start, float seed) {}

    private final List<Hit> hits = new ArrayList<>();
    private final Random random = Random.create();
    private long markerAt = -1;
    private boolean markerCrit;

    public HitFx() {
        super("hit_fx", "Hit FX", "Hit marker, slash, ring, floating CRIT markers and a hit sound when you hit.", Category.COSMETICS, false,
                "hit fx", "hitfx", "hit marker", "hit sound", "hit sounds", "damage markers", "target aura", "crit", "slash");
    }

    /** Called when you attack an entity (before the attack is sent). */
    public void onHit(Entity target) {
        if (mc.player == null) {
            return;
        }
        boolean crit = isCritical(target);
        Vec3d at = new Vec3d(target.getX(), target.getY() + target.getHeight() * 0.6, target.getZ());
        hits.add(new Hit(target, at, crit, System.currentTimeMillis(), random.nextFloat()));
        while (hits.size() > 10) {
            hits.remove(0);
        }
        markerAt = System.currentTimeMillis();
        markerCrit = crit;
        int c = crit ? critColor.color() : color.color();
        if (style.is(Style.BURST)) {
            int n = Math.round((crit ? 26 : 16) * Aqua.ui().effectIntensity.f());
            for (int i = 0; i < n; i++) {
                double a = random.nextDouble() * Math.PI * 2;
                float sp = 2f + random.nextFloat() * 3f;
                Particles.spawn(at.x, at.y, at.z, (float) Math.cos(a) * sp, (random.nextFloat() - 0.3f) * 3f, (float) Math.sin(a) * sp,
                        0.3f + random.nextFloat() * 0.25f, 0.05f, c, 4f, 4f);
            }
        }
        playSound(crit);
    }

    /** The same conditions under which the game makes a hit critical (all of them known to your client). */
    private boolean isCritical(Entity target) {
        var p = mc.player;
        return target instanceof LivingEntity && p.fallDistance > 0 && !p.isOnGround() && !p.isClimbing() && !p.isTouchingWater()
                && !p.hasStatusEffect(StatusEffects.BLINDNESS) && !p.hasVehicle() && !p.isSprinting()
                && p.getAttackCooldownProgress(0.5f) > 0.9f;
    }

    private void playSound(boolean crit) {
        SoundEvent ev = switch (sound.get()) {
            case NONE -> null;
            case CLICK -> SoundEvents.BLOCK_NOTE_BLOCK_HAT.value();
            case DING -> SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
            case CHIME -> SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value();
            case BELL -> SoundEvents.BLOCK_NOTE_BLOCK_BELL.value();
            case PLING -> SoundEvents.BLOCK_NOTE_BLOCK_PLING.value();
            case BONK -> SoundEvents.BLOCK_NOTE_BLOCK_BASEDRUM.value();
            case CRYSTAL -> SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME;
            case ARROW -> SoundEvents.ENTITY_ARROW_HIT_PLAYER;
        };
        if (ev == null) {
            return;
        }
        float pitch = crit ? 1.35f : 1.0f + random.nextFloat() * 0.1f;
        mc.getSoundManager().play(new PositionedSoundInstance(ev, SoundCategory.PLAYERS, volume.f(), pitch, Random.create(),
                mc.player.getX(), mc.player.getEyeY(), mc.player.getZ()));
    }

    @Override
    public void renderWorld(Fx fx, VertexConsumerProvider vcp) {
        long now = System.currentTimeMillis();
        VertexConsumer glow = vcp.getBuffer(FxLayers.GLOW);
        Iterator<Hit> it = hits.iterator();
        while (it.hasNext()) {
            Hit h = it.next();
            float age = (now - h.start) / 1000f;
            if (age > 1.4f) {
                it.remove();
                continue;
            }
            int c = h.crit ? critColor.color() : color.color();
            // Ring at the feet: follows the target while it fades.
            if (ring.on()) {
                float t = Math.min(1f, age / 1.4f);
                Entity e = h.target;
                double x = e.isRemoved() ? h.at.x : e.getLerpedPos(fx.tickDelta).x;
                double y = e.isRemoved() ? h.at.y - e.getHeight() * 0.6 : e.getLerpedPos(fx.tickDelta).y;
                double z = e.isRemoved() ? h.at.z : e.getLerpedPos(fx.tickDelta).z;
                float rad = Math.max(0.45f, e.getWidth() * 0.9f) * (0.85f + 0.15f * Anim.easeOutCubic(Math.min(1f, age * 4)));
                float a = (1f - t) * (1f - t);
                fx.ring(glow, x, y + 0.03, z, rad, 0.06f, Colors.fade(c, a * 0.95f), 48);
                fx.ring(glow, x, y + 0.03, z, rad * 0.97f, 0.28f, Colors.fade(c, a * 0.2f), 48);
            }
            // Slash: a bright crescent across the target, drawn as a sweep.
            if (style.is(Style.SLASH) && age < 0.35f) {
                float t = age / 0.35f;
                drawSlash(fx, glow, h, c, t);
            }
            if (style.is(Style.STAR) && age < 0.45f) {
                float t = age / 0.45f;
                float size = (0.25f + 0.55f * Anim.easeOutCubic(t)) * (h.crit ? 1.3f : 1f);
                float a = 1f - t;
                Vec3d p = h.at;
                for (int k = 0; k < 4; k++) {
                    double ang = Math.PI / 4 * k + h.seed;
                    Vec3d off = new Vec3d(fx.right.x * Math.cos(ang) + fx.up.x * Math.sin(ang), fx.right.y * Math.cos(ang) + fx.up.y * Math.sin(ang),
                            fx.right.z * Math.cos(ang) + fx.up.z * Math.sin(ang)).multiply(size);
                    fx.ribbon(glow, p.subtract(off), p.add(off), 0.0f, 0.0f, 0, 0);
                    fx.ribbon(glow, p.subtract(off), p, 0.0f, 0.06f, Colors.fade(c, 0f), Colors.fade(c, a));
                    fx.ribbon(glow, p, p.add(off), 0.06f, 0.0f, Colors.fade(c, a), Colors.fade(c, 0f));
                }
                fx.spark(p.x, p.y, p.z, 0.25f * (1 - t) + 0.05f, Colors.fade(0xFFFFFFFF, a));
            }
            if (floating.on() && age < 1.1f) {
                float t = age / 1.1f;
                float a = t < 0.15f ? t / 0.15f : 1f - Anim.clamp01((t - 0.55f) / 0.45f);
                double rise = 0.25 + 0.6 * Anim.easeOutCubic(t);
                double side = (h.seed - 0.5) * 0.6;
                String label = h.crit ? "CRIT!" : "HIT";
                WorldText.draw(fx, vcp, label, h.at.x + fx.right.x * side, h.at.y + 0.35 + rise, h.at.z + fx.right.z * side,
                        h.crit ? 0.32f : 0.22f, AquaFont.Weight.BOLD, Colors.fade(h.crit ? critColor.color() : 0xFFFFFFFF, a), c, a * 0.8f);
            }
        }
    }

    private void drawSlash(Fx fx, VertexConsumer glow, Hit h, int c, float t) {
        float len = h.crit ? 1.5f : 1.15f;
        double tilt = (h.seed - 0.5) * 1.2 + 0.6;
        float rx = fx.right.x, ry = fx.right.y, rz = fx.right.z;
        float ux = fx.up.x, uy = fx.up.y, uz = fx.up.z;
        int segs = 14;
        float head = Anim.easeOutCubic(Math.min(1f, t * 1.6f));
        float tail = Anim.easeInOutCubic(Math.max(0f, (t - 0.25f) / 0.75f));
        Vec3d prev = null;
        for (int i = 0; i <= segs; i++) {
            float k = i / (float) segs;
            if (k < tail || k > head) {
                prev = null;
                continue;
            }
            double ang = tilt + (k - 0.5) * 1.6;
            double bend = Math.sin(k * Math.PI) * 0.22;
            double sx = Math.cos(ang) * (k - 0.5) * len * 2, sy = Math.sin(ang) * (k - 0.5) * len * 2;
            double bx = -Math.sin(ang) * bend, by = Math.cos(ang) * bend;
            Vec3d p = h.at.add(rx * (sx + bx) + ux * (sy + by), ry * (sx + bx) + uy * (sy + by), rz * (sx + bx) + uz * (sy + by));
            if (prev != null) {
                float w = (float) Math.sin(k * Math.PI) * 0.09f + 0.01f;
                fx.ribbon(glow, prev, p, w, w, Colors.fade(c, 0.95f), Colors.fade(c, 0.95f));
                fx.ribbon(glow, prev, p, w * 3, w * 3, Colors.fade(c, 0.25f), Colors.fade(c, 0.25f));
                fx.ribbon(glow, prev, p, w * 0.35f, w * 0.35f, 0xFFFFFFFF, 0xFFFFFFFF);
            }
            prev = p;
        }
    }

    /** Crosshair hit marker (four short diagonal strokes that pop and fade). */
    public void drawMarker(DrawContext ctx) {
        if (!isEnabled() || !marker.on() || markerAt < 0 || mc.options.hudHidden) {
            return;
        }
        float t = (System.currentTimeMillis() - markerAt) / 260f;
        if (t >= 1f) {
            markerAt = -1;
            return;
        }
        float cx = ctx.getScaledWindowWidth() / 2f, cy = ctx.getScaledWindowHeight() / 2f;
        int c = Colors.fade(markerCrit ? critColor.color() : 0xFFFFFFFF, 1f - t * t);
        float gap = 3f + 2f * Anim.easeOutCubic(t);
        float len = markerCrit ? 4.5f : 3.5f;
        for (int k = 0; k < 4; k++) {
            float dx = (k % 2 == 0) ? 1 : -1, dy = (k < 2) ? 1 : -1;
            float x0 = cx + dx * gap * 0.707f, y0 = cy + dy * gap * 0.707f;
            float x1 = cx + dx * (gap + len) * 0.707f, y1 = cy + dy * (gap + len) * 0.707f;
            R2D.line(ctx, x0, y0, x1, y1, markerCrit ? 1.3f : 1f, c);
        }
    }
}
