package dev.aqua.client;

import dev.aqua.client.cosmetic.CosmeticFeature;
import dev.aqua.client.gui.AquaMenuScreen;
import dev.aqua.client.hud.widgets.EffectsWidget;
import dev.aqua.client.hud.widgets.ScoreboardWidget;
import dev.aqua.client.module.Module;
import dev.aqua.client.module.cosmetic.HitParticles;
import dev.aqua.client.module.cosmetic.PopEffect;
import dev.aqua.client.module.cosmetic.WorldJoinEffect;
import dev.aqua.client.module.perf.EffectBudget;
import dev.aqua.client.module.visual.BlockOutline;
import dev.aqua.client.module.visual.CustomCrosshair;
import dev.aqua.client.module.visual.CustomFog;
import dev.aqua.client.module.visual.CustomSky;
import dev.aqua.client.module.visual.TimeChanger;
import dev.aqua.client.module.visual.WeatherChanger;
import dev.aqua.client.module.visual.WorldTint;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.world.Fx;
import dev.aqua.client.render.world.FxBuffers;
import dev.aqua.client.render.world.FxLayers;
import dev.aqua.client.render.world.Particles;
import dev.aqua.client.render.world.WorldEffect;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.joml.Vector4f;

/** Glue between Fabric events / mixins and Aqua features. */
public final class Hooks {
    /** True while ClientWorld advances its stored time (see ClientWorldMixin). */
    public static volatile boolean inTimeTick;

    private Hooks() {}

    private static <M extends Module> M on(Class<M> type) {
        if (Aqua.modules() == null) {
            return null;
        }
        M m = Aqua.modules().get(type);
        return m != null && m.isEnabled() ? m : null;
    }

    // ------------------------------------------------------------------ HUD

    @SuppressWarnings("UnstableApiUsage")
    static void registerVanillaHudReplacements() {
        HudElementRegistry.addFirst(Identifier.of(AquaClient.MOD_ID, "world_tint"), (ctx, tick) -> {
            WorldTint t = on(WorldTint.class);
            if (t != null && MinecraftClient.getInstance().world != null) {
                t.draw(ctx);
            }
        });
        HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, vanilla -> (ctx, tick) -> {
            CustomCrosshair c = on(CustomCrosshair.class);
            if (c != null) {
                c.draw(ctx);
            } else {
                vanilla.render(ctx, tick);
            }
            drawCrosshairExtras(ctx);
        });
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, Identifier.of(AquaClient.MOD_ID, "media_hider"), (ctx, tick) -> {
            dev.aqua.client.module.qol.MediaHider mh = Aqua.modules().get(dev.aqua.client.module.qol.MediaHider.class);
            if (mh != null) {
                mh.drawHotbarCover(ctx);
            }
        });
        HudElementRegistry.attachElementBefore(VanillaHudElements.CROSSHAIR, Identifier.of(AquaClient.MOD_ID, "damage_fx"), (ctx, tick) -> {
            dev.aqua.client.module.cosmetic.DamageFx d = Aqua.modules().get(dev.aqua.client.module.cosmetic.DamageFx.class);
            if (d != null) {
                d.draw(ctx);
            }
        });
        HudElementRegistry.replaceElement(VanillaHudElements.SCOREBOARD, vanilla -> (ctx, tick) -> {
            if (on(ScoreboardWidget.class) == null) {
                vanilla.render(ctx, tick);
            }
        });
        HudElementRegistry.replaceElement(VanillaHudElements.STATUS_EFFECTS, vanilla -> (ctx, tick) -> {
            EffectsWidget e = on(EffectsWidget.class);
            if (e == null || !e.hideVanilla.on()) {
                vanilla.render(ctx, tick);
            }
        });
        HudElementRegistry.addLast(Identifier.of(AquaClient.MOD_ID, "join_effect"), (ctx, tick) -> {
            WorldJoinEffect j = Aqua.modules().get(WorldJoinEffect.class);
            if (j != null) {
                j.render(ctx);
            }
        });
    }

    /** Hit marker and attack indicator, drawn with the crosshair. */
    public static void drawCrosshairExtras(net.minecraft.client.gui.DrawContext ctx) {
        dev.aqua.client.module.cosmetic.HitFx hf = Aqua.modules().get(dev.aqua.client.module.cosmetic.HitFx.class);
        if (hf != null) {
            hf.drawMarker(ctx);
        }
        dev.aqua.client.module.visual.AttackIndicator ai = Aqua.modules().get(dev.aqua.client.module.visual.AttackIndicator.class);
        if (ai != null) {
            ai.draw(ctx);
        }
    }

    // ------------------------------------------------------------------ world rendering

    static void registerWorldRendering() {
        WorldRenderEvents.BEFORE_ENTITIES.register(ctx -> renderPhase(ctx, WorldEffect.Phase.SKY));
        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> renderPhase(ctx, WorldEffect.Phase.WORLD));
        WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register((ctx, outline) -> {
            BlockOutline b = on(BlockOutline.class);
            if (b == null || outline == null) {
                return true;
            }
            try {
                Fx fx = fx(ctx);
                VertexConsumerProvider vcp = FxBuffers.provider();
                b.draw(fx, vcp, outline.pos(), outline.shape());
                FxBuffers.flush();
            } catch (Throwable t) {
                dev.aqua.client.util.Problems.report("Block outline failed; disabling it", t);
                b.setEnabled(false, false);
                return true;
            }
            return false;
        });
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, renderer, helper, context) -> {
            if (renderer instanceof PlayerEntityRenderer<?> p) {
                helper.register(new CosmeticFeature(p));
            }
        });
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient() && player == MinecraftClient.getInstance().player) {
                HitParticles h = on(HitParticles.class);
                if (h != null) {
                    h.onHit(entity);
                }
                dev.aqua.client.module.cosmetic.HitFx hf = on(dev.aqua.client.module.cosmetic.HitFx.class);
                if (hf != null) {
                    hf.onHit(entity);
                }
                dev.aqua.client.module.cosmetic.KillEffects k = on(dev.aqua.client.module.cosmetic.KillEffects.class);
                if (k != null) {
                    k.onAttack(entity);
                }
            }
            return ActionResult.PASS;
        });
    }

    private static Fx fx(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        MatrixStack ms = ctx.matrices() != null ? ctx.matrices() : new MatrixStack();
        return new Fx(ms, mc.gameRenderer.getCamera(), mc.getRenderTickCounter().getTickProgress(true));
    }

    private static final java.util.Set<Module> SUSPENDED = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    private static void renderPhase(WorldRenderContext ctx, WorldEffect.Phase phase) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || Aqua.modules() == null) {
            return;
        }
        EffectBudget budget = Aqua.modules().get(EffectBudget.class);
        if (phase == WorldEffect.Phase.WORLD && budget != null && budget.pauseInMenus() && mc.currentScreen instanceof AquaMenuScreen) {
            return;
        }
        VertexConsumerProvider vcp = FxBuffers.provider();
        Fx fx = fx(ctx);
        fx.glow = vcp.getBuffer(FxLayers.GLOW);
        fx.color = vcp.getBuffer(FxLayers.COLOR);
        for (Module m : Aqua.modules().all()) {
            if (m.isEnabled() && m instanceof WorldEffect e && e.phase() == phase) {
                if (SUSPENDED.contains(m)) {
                    continue;
                }
                try {
                    fx.glow = vcp.getBuffer(FxLayers.GLOW);
                    fx.sparks = null;
                    e.renderWorld(fx, vcp);
                } catch (Throwable t) {
                    // Paused for this session only: the saved on/off state is left alone.
                    SUSPENDED.add(m);
                    dev.aqua.client.util.Problems.report("World effect " + m.id() + " failed; pausing it until restart", t);
                    Aqua.notifications().warn(m.name(), "Hit an error and is paused until restart");
                }
            }
        }
        if (phase == WorldEffect.Phase.WORLD) {
            fx.sparks = vcp.getBuffer(FxLayers.spark());
            Particles.renderAll(fx);
        }
        FxBuffers.flush();
    }

    // ------------------------------------------------------------------ events

    static void onJoin() {
        WorldJoinEffect j = Aqua.modules().get(WorldJoinEffect.class);
        if (j != null) {
            MinecraftClient.getInstance().execute(j::trigger);
        }
    }

    static void tick(MinecraftClient mc) {
    }

    /** Adds the Aqua badge to player nametags. */
    public static void decoratePlayerState(net.minecraft.entity.PlayerLikeEntity entity, net.minecraft.client.render.entity.state.PlayerEntityRenderState state) {
        if (state.displayName == null || Aqua.modules() == null) {
            return;
        }
        dev.aqua.client.module.qol.AquaBadge b = Aqua.modules().get(dev.aqua.client.module.qol.AquaBadge.class);
        if (b != null && b.onNametags()) {
            state.displayName = dev.aqua.client.module.qol.AquaBadge.decorate(state.displayName, entity.getUuid());
        }
    }

    public static void onEntityStatus(EntityStatusS2CPacket packet) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (packet.getStatus() == 3 && mc.world != null) {
            dev.aqua.client.module.cosmetic.KillEffects k = on(dev.aqua.client.module.cosmetic.KillEffects.class);
            Entity dead = packet.getEntity(mc.world);
            if (k != null && dead != null) {
                k.onDeath(dead);
            }
        }
        if (packet.getStatus() == 35 && mc.world != null) {
            PopEffect p = on(PopEffect.class);
            Entity e = packet.getEntity(mc.world);
            if (p != null && e != null) {
                p.onPop(e);
            }
        }
    }

    // ------------------------------------------------------------------ time / weather / fog

    public static long visualTimeOfDay(long real) {
        if (inTimeTick) {
            return real;
        }
        TimeChanger t = on(TimeChanger.class);
        if (t == null) {
            return real;
        }
        long day = Math.floorDiv(real, 24000L);
        return day * 24000L + t.timeOfDay();
    }

    public static Float rainOverride() {
        WeatherChanger w = on(WeatherChanger.class);
        return w != null ? w.rain() : null;
    }

    public static Float thunderOverride() {
        WeatherChanger w = on(WeatherChanger.class);
        return w != null ? w.thunder() : null;
    }

    private static boolean atmospheric() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.gameRenderer.getCamera().getSubmersionType() == CameraSubmersionType.NONE;
    }

    public static Vector4f fogColor(Vector4f color) {
        if (!atmospheric()) {
            return color;
        }
        int argb = Colors.argb(Math.round(color.w * 255), Math.round(color.x * 255), Math.round(color.y * 255), Math.round(color.z * 255));
        boolean changed = false;
        CustomSky sky = on(CustomSky.class);
        if (sky != null && sky.matchFog()) {
            argb = Colors.withAlpha(sky.fogColor(), Colors.alpha(argb));
            changed = true;
        }
        CustomFog fog = on(CustomFog.class);
        if (fog != null) {
            argb = fog.apply(argb);
            changed = true;
        }
        if (!changed) {
            return color;
        }
        return new Vector4f(Colors.r(argb), Colors.g(argb), Colors.b(argb), color.w);
    }

    public static float fogEnd(float end) {
        CustomFog fog = on(CustomFog.class);
        return fog != null && atmospheric() ? fog.end(end) : end;
    }

    public static float fogStart(float start) {
        CustomFog fog = on(CustomFog.class);
        return fog != null && atmospheric() ? Math.min(start, fog.end(start)) : start;
    }
}
