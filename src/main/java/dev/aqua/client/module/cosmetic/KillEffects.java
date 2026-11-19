package dev.aqua.client.module.cosmetic;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.world.Fx;
import dev.aqua.client.render.world.FxLayers;
import dev.aqua.client.render.world.Particles;
import dev.aqua.client.render.world.WorldEffect;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.setting.EnumSetting;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Plays an effect where something you hit dies: lightning, a rising soul, a burst, a splash, hearts or
 * fireworks. Purely visual and local (nobody else sees or hears it); it never affects combat.
 */
public final class KillEffects extends Module implements WorldEffect {
    public enum Effect implements EnumSetting.Displayable {
        LIGHTNING("Lightning"), SOUL("Soul"), BURST("Burst"), BLOOD("Blood splash"), HEARTS("Hearts"), FIREWORK("Firework");
        final String l;

        Effect(String l) {
            this.l = l;
        }

        public String displayName() {
            return l;
        }
    }

    private final EnumSetting<Effect> effect = add(new EnumSetting<>("effect", "Effect", Effect.LIGHTNING));
    private final ColorSetting color = add(new ColorSetting("color", "Colour", 0xFF52E4F2, false, true));
    private final BoolSetting sound = add(new BoolSetting("sound", "Play a sound (only you hear it)", true));
    private final BoolSetting playersOnly = add(new BoolSetting("players_only", "Players only", false));

    private final Map<Integer, Long> recentlyHit = new HashMap<>();
    private final Random random = Random.create();

    private record Active(Effect effect, Vec3d pos, float height, long start, long seed) {}

    private final List<Active> active = new ArrayList<>();
    private int triggered;

    public KillEffects() {
        super("kill_effects", "Kill Effects", "An effect where something you hit is defeated (you only).", Category.COSMETICS, false,
                "kill effect", "death effect", "lightning", "soul", "firework");
    }

    /** Remember entities you hit for a few seconds so their death can be attributed to you. */
    public void onAttack(Entity target) {
        recentlyHit.put(target.getId(), System.currentTimeMillis());
    }

    /** Called for entity status 3 (death). */
    public void onDeath(Entity e) {
        Long t = recentlyHit.remove(e.getId());
        if (t == null || System.currentTimeMillis() - t > 6000 || e == mc.player) {
            return;
        }
        if (playersOnly.on() && !(e instanceof PlayerEntity)) {
            return;
        }
        trigger(effect.get(), e.getEntityPos(), e.getHeight());
    }

    /** Starts an effect at a position (also used by the wardrobe preview). */
    public void trigger(Effect fx, Vec3d pos, float height) {
        triggered++;
        active.add(new Active(fx, pos, height, System.currentTimeMillis(), random.nextLong()));
        while (active.size() > 8) {
            active.remove(0);
        }
        int c = color.color();
        float budget = Aqua.ui().effectIntensity.f();
        switch (fx) {
            case BURST -> burst(pos.add(0, height * 0.5, 0), Math.round(70 * budget), c, 4.5f, 2.5f);
            case BLOOD -> {
                for (int i = 0; i < Math.round(60 * budget); i++) {
                    double a = random.nextDouble() * Math.PI * 2;
                    float sp = 1f + random.nextFloat() * 3f;
                    Particles.spawn(pos.x, pos.y + height * 0.6, pos.z, (float) Math.cos(a) * sp, 1.5f + random.nextFloat() * 3f, (float) Math.sin(a) * sp,
                            0.8f + random.nextFloat() * 0.6f, 0.07f, random.nextBoolean() ? 0xFFB3122A : 0xFF7A0A18, 12f, 1.5f);
                }
            }
            case HEARTS -> {
                for (int i = 0; i < 9; i++) {
                    mc.world.addParticleClient(ParticleTypes.HEART, pos.x + (random.nextDouble() - 0.5) * 1.2,
                            pos.y + height * 0.5 + random.nextDouble() * height, pos.z + (random.nextDouble() - 0.5) * 1.2, 0, 0.1, 0);
                }
                burst(pos.add(0, height * 0.6, 0), Math.round(25 * budget), 0xFFFF5FAE, 2f, 1f);
            }
            case FIREWORK -> {
                int[] cols = {c, 0xFFFFD25A, 0xFFFF5FAE, 0xFFB48CFF, 0xFF5BE38C};
                for (int i = 0; i < Math.round(120 * budget); i++) {
                    double th = random.nextDouble() * Math.PI * 2;
                    double ph = Math.acos(2 * random.nextDouble() - 1);
                    float sp = 5f + random.nextFloat() * 1.5f;
                    Particles.spawn(pos.x, pos.y + height + 1.5, pos.z, (float) (Math.sin(ph) * Math.cos(th)) * sp,
                            (float) Math.cos(ph) * sp, (float) (Math.sin(ph) * Math.sin(th)) * sp, 1.1f + random.nextFloat() * 0.4f, 0.06f,
                            cols[random.nextInt(cols.length)], 2.5f, 2.2f);
                }
            }
            default -> {
            }
        }
        if (sound.on()) {
            SoundEvent ev = switch (fx) {
                case LIGHTNING -> SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER;
                case SOUL -> SoundEvents.PARTICLE_SOUL_ESCAPE.value();
                case FIREWORK -> SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST;
                case HEARTS -> SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
                default -> SoundEvents.ENTITY_PLAYER_LEVELUP;
            };
            float vol = fx == Effect.LIGHTNING ? 0.35f : 0.6f;
            mc.getSoundManager().play(new PositionedSoundInstance(ev, SoundCategory.PLAYERS, vol, 1f, net.minecraft.util.math.random.Random.create(),
                    pos.x, pos.y, pos.z));
        }
    }

    private void burst(Vec3d p, int n, int c, float speed, float life) {
        for (int i = 0; i < n; i++) {
            double th = random.nextDouble() * Math.PI * 2;
            double ph = Math.acos(2 * random.nextDouble() - 1);
            float sp = speed * (0.5f + random.nextFloat());
            Particles.spawn(p.x, p.y, p.z, (float) (Math.sin(ph) * Math.cos(th)) * sp, (float) Math.cos(ph) * sp,
                    (float) (Math.sin(ph) * Math.sin(th)) * sp, life * (0.4f + random.nextFloat() * 0.3f), 0.06f, c, 2f, 2.5f);
        }
    }

    /** Number of effects started since launch (used by the in-game tests). */
    public int triggered() {
        return triggered;
    }

    @Override
    public void onTick() {
        long now = System.currentTimeMillis();
        recentlyHit.values().removeIf(t -> now - t > 6000);
    }

    @Override
    public void renderWorld(Fx fx, VertexConsumerProvider vcp) {
        long now = System.currentTimeMillis();
        VertexConsumer glow = vcp.getBuffer(FxLayers.GLOW);
        Iterator<Active> it = active.iterator();
        while (it.hasNext()) {
            Active a = it.next();
            float t = (now - a.start) / 1000f;
            switch (a.effect) {
                case LIGHTNING -> {
                    if (t > 0.6f) {
                        it.remove();
                        continue;
                    }
                    float alpha = t < 0.08f ? 1f : Math.max(0f, 1f - (t - 0.08f) / 0.5f);
                    Random r = Random.create(a.seed + (long) (t * 12));
                    Vec3d top = a.pos.add(0, 30, 0);
                    Vec3d prev = top;
                    int segs = 14;
                    for (int i = 1; i <= segs; i++) {
                        float f = i / (float) segs;
                        Vec3d next = i == segs ? a.pos : new Vec3d(a.pos.x + (r.nextDouble() - 0.5) * 2.2 * (1 - f * 0.6),
                                top.y - (top.y - a.pos.y) * f, a.pos.z + (r.nextDouble() - 0.5) * 2.2 * (1 - f * 0.6));
                        fx.ribbon(glow, prev, next, 0.35f, 0.35f, Colors.fade(color.color(), alpha * 0.45f), Colors.fade(color.color(), alpha * 0.45f));
                        fx.ribbon(glow, prev, next, 0.09f, 0.09f, Colors.fade(0xFFFFFFFF, alpha), Colors.fade(0xFFFFFFFF, alpha));
                        prev = next;
                    }
                    fx.ring(glow, a.pos.x, a.pos.y + 0.03, a.pos.z, 0.6f + t * 5f, 0.25f, Colors.fade(color.color(), alpha * 0.7f), 32);
                }
                case SOUL -> {
                    if (t > 2f) {
                        it.remove();
                        continue;
                    }
                    float alpha = Math.min(1f, t * 4f) * (1f - t / 2f);
                    double y = a.pos.y + a.height * 0.5 + t * 2.2;
                    double sway = Math.sin(t * 5) * 0.25;
                    fx.sparks = vcp.getBuffer(FxLayers.spark());
                    fx.spark(a.pos.x + sway, y, a.pos.z, 0.35f, Colors.fade(color.color(), alpha));
                    for (int k = 1; k < 8; k++) {
                        double ty = y - k * 0.18;
                        fx.spark(a.pos.x + Math.sin((t - k * 0.06) * 5) * 0.25, ty, a.pos.z, 0.22f * (1 - k / 8f), Colors.fade(color.color(), alpha * 0.6f));
                    }
                    if (random.nextFloat() < 0.4f) {
                        Particles.spawn(a.pos.x + sway, y, a.pos.z, (random.nextFloat() - 0.5f) * 0.6f, 0.3f, (random.nextFloat() - 0.5f) * 0.6f,
                                0.8f, 0.05f, color.color(), -0.2f, 0.5f);
                    }
                }
                default -> {
                    if (t > 1.5f) {
                        it.remove();
                        continue;
                    }
                    if (a.effect == Effect.BURST || a.effect == Effect.FIREWORK) {
                        float alpha = 1f - t / 1.5f;
                        fx.ring(glow, a.pos.x, a.pos.y + 0.03, a.pos.z, 0.4f + t * 3f, 0.15f, Colors.fade(color.color(), alpha * 0.6f), 28);
                    }
                }
            }
        }
    }
}
