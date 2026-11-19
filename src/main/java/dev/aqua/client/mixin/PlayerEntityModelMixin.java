package dev.aqua.client.mixin;

import dev.aqua.client.Aqua;
import dev.aqua.client.cosmetic.Emotes;
import dev.aqua.client.module.cosmetic.EmotesModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Emotes: poses your own player model (local rendering only). */
@Mixin(PlayerEntityModel.class)
public abstract class PlayerEntityModelMixin {
    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    private void aqua$emote(PlayerEntityRenderState state, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        EmotesModule mod = Aqua.modules() != null ? Aqua.modules().get(EmotesModule.class) : null;
        if (mod == null || !mod.isEnabled() || mc.player == null || state.id != mc.player.getId()) {
            return;
        }
        Emotes.Emote e = Emotes.current();
        if (e == null) {
            return;
        }
        float w = Emotes.weight();
        float t = Emotes.time();
        BipedEntityModel<?> m = (BipedEntityModel<?>) (Object) this;
        float s;
        switch (e) {
            case WAVE -> {
                m.rightArm.pitch = lerp(m.rightArm.pitch, -2.7f, w);
                m.rightArm.roll = lerp(m.rightArm.roll, -0.25f + (float) Math.sin(t * 9) * 0.35f, w);
                m.head.roll = lerp(m.head.roll, (float) Math.sin(t * 4.5f) * 0.08f, w);
            }
            case CHEER -> {
                s = (float) Math.sin(t * 10);
                m.rightArm.pitch = lerp(m.rightArm.pitch, -2.9f + s * 0.2f, w);
                m.leftArm.pitch = lerp(m.leftArm.pitch, -2.9f - s * 0.2f, w);
                m.rightArm.roll = lerp(m.rightArm.roll, -0.35f, w);
                m.leftArm.roll = lerp(m.leftArm.roll, 0.35f, w);
                m.head.pitch = lerp(m.head.pitch, -0.3f, w);
            }
            case DANCE -> {
                s = (float) Math.sin(t * 7.5f);
                float c = (float) Math.cos(t * 7.5f);
                m.rightArm.pitch = lerp(m.rightArm.pitch, -0.6f + s * 1.1f, w);
                m.leftArm.pitch = lerp(m.leftArm.pitch, -0.6f - s * 1.1f, w);
                m.rightArm.roll = lerp(m.rightArm.roll, 0.3f + c * 0.3f, w);
                m.leftArm.roll = lerp(m.leftArm.roll, -0.3f + c * 0.3f, w);
                m.rightLeg.pitch = lerp(m.rightLeg.pitch, s * 0.45f, w);
                m.leftLeg.pitch = lerp(m.leftLeg.pitch, -s * 0.45f, w);
                m.head.roll = lerp(m.head.roll, c * 0.18f, w);
                m.body.yaw = lerp(m.body.yaw, s * 0.25f, w);
            }
            case CLAP -> {
                s = (float) Math.abs(Math.sin(t * 6));
                m.rightArm.pitch = lerp(m.rightArm.pitch, -1.35f, w);
                m.leftArm.pitch = lerp(m.leftArm.pitch, -1.35f, w);
                m.rightArm.yaw = lerp(m.rightArm.yaw, -0.2f - s * 0.45f, w);
                m.leftArm.yaw = lerp(m.leftArm.yaw, 0.2f + s * 0.45f, w);
            }
            case BOW -> {
                s = Math.min(1f, t * 2f);
                m.body.pitch = lerp(m.body.pitch, 0.5f * s, w);
                m.head.pitch = lerp(m.head.pitch, 0.6f * s, w);
                m.rightArm.pitch = lerp(m.rightArm.pitch, 0.3f * s, w);
                m.leftArm.pitch = lerp(m.leftArm.pitch, 0.3f * s, w);
            }
            case POINT -> {
                m.rightArm.pitch = lerp(m.rightArm.pitch, -1.57f + m.head.pitch, w);
                m.rightArm.yaw = lerp(m.rightArm.yaw, m.head.yaw, w);
            }
        }
        // Sleeves, pants, jacket and hat are child parts in 1.21.11, so they follow automatically.
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
