package dev.aqua.client.mixin;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.visual.HandShader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {
    /** 3D skins replace the vanilla model, so vanilla layers (armour, held items, cape) are skipped too. */
    /** Hand Shader: restyle the first-person arm (this method is only used for first-person arms). */
    @Redirect(method = "renderArm", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModelPart(Lnet/minecraft/client/model/ModelPart;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IILnet/minecraft/client/texture/Sprite;)V"))
    private void aqua$handShader(OrderedRenderCommandQueue queue, ModelPart part, MatrixStack matrices, RenderLayer layer, int light,
                                 int overlay, Sprite sprite) {
        HandShader hs = Aqua.modules() != null ? Aqua.modules().get(HandShader.class) : null;
        if (hs == null || !hs.isEnabled()) {
            queue.submitModelPart(part, matrices, layer, light, overlay, sprite);
            return;
        }
        RenderLayer l = layer;
        if (hs.style() != HandShader.Style.TINT) {
            Identifier tex = hs.texture();
            l = RenderLayers.entityTranslucent(tex);
        }
        queue.submitModelPart(part, matrices, l, light, OverlayTexture.DEFAULT_UV, null, hs.tint(), null);
    }

    /** Streamer Mode: hide your own name above your head and optionally your skin (your screen only). */
    @Inject(method = "updateRenderState(Lnet/minecraft/entity/PlayerLikeEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V",
            at = @At("TAIL"))
    private void aqua$streamer(PlayerLikeEntity entity, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && entity == mc.player && Aqua.modules() != null) {
            dev.aqua.client.cosmetic.Look look = dev.aqua.client.cosmetic.Look.effective();
            dev.aqua.client.cosmetic.Look.attach(state, look);
            if (look.cape() != null) {
                state.capeVisible = false;
            }
        }
        dev.aqua.client.Hooks.decoratePlayerState(entity, state);
    }

    /** Aqua Badge: optionally show your own nametag (with badge) in third person. */
    @Inject(method = "hasLabel(Lnet/minecraft/entity/PlayerLikeEntity;D)Z", at = @At("HEAD"), cancellable = true)
    private void aqua$ownLabel(PlayerLikeEntity entity, double distance, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && entity == mc.player && !mc.options.getPerspective().isFirstPerson() && MinecraftClient.isHudEnabled()
                && Aqua.modules() != null) {
            dev.aqua.client.module.qol.AquaBadge b = Aqua.modules().get(dev.aqua.client.module.qol.AquaBadge.class);
            if (b != null && b.ownNametag() && !mc.player.isInvisible()) {
                cir.setReturnValue(true);
            }
        }
    }
}
