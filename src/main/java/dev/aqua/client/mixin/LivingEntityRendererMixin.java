package dev.aqua.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.aqua.client.cosmetic.Look;
import dev.aqua.client.cosmetic.character.CharacterRenderer;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 3D skins: draws the selected character instead of the player model (your own player only). */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @WrapOperation(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/texture/Sprite;ILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V"))
    private void aqua$character(OrderedRenderCommandQueue queue, Model<?> model, Object state, MatrixStack matrices, RenderLayer layer, int light,
                                int overlay, int tint, Sprite sprite, int outline, ModelCommandRenderer.CrumblingOverlayCommand crumbling,
                                Operation<Void> original) {
        if (state instanceof PlayerEntityRenderState ps && model instanceof PlayerEntityModel pm) {
            Look look = Look.of(ps);
            if (look != null && look.character() != null) {
                CharacterRenderer.render(matrices, queue, pm, ps, look.character());
                return;
            }
            dev.aqua.client.module.cosmetic.GlassSkin glass = look != null && dev.aqua.client.Aqua.modules() != null
                    ? dev.aqua.client.Aqua.modules().get(dev.aqua.client.module.cosmetic.GlassSkin.class) : null;
            if (glass != null && glass.isEnabled() && ps.skinTextures != null && !ps.invisible) {
                // Your own player only (a Look is attached to your player's render state alone).
                RenderLayer clear = net.minecraft.client.render.RenderLayers.entityTranslucent(ps.skinTextures.body().texturePath());
                original.call(queue, model, state, matrices, clear, light, overlay, glass.tint(), sprite, outline, crumbling);
                glass.edges(matrices, queue, pm, ps);
                return;
            }
        }
        original.call(queue, model, state, matrices, layer, light, overlay, tint, sprite, outline, crumbling);
    }

    /**
     * With a 3D skin, armour, cape, elytra and the other player layers would not fit the character, so
     * only the held-item layer (which follows the posed arms) is kept.
     */
    @WrapOperation(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/feature/FeatureRenderer;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/EntityRenderState;FF)V"))
    private void aqua$characterFeatures(FeatureRenderer<?, ?> feature, MatrixStack matrices, OrderedRenderCommandQueue queue, int light,
                                        EntityRenderState state, float limbAngle, float limbDistance, Operation<Void> original) {
        if (state instanceof PlayerEntityRenderState ps && !(feature instanceof HeldItemFeatureRenderer<?, ?>)) {
            Look look = Look.of(ps);
            if (look != null && look.character() != null) {
                return;
            }
        }
        original.call(feature, matrices, queue, light, state, limbAngle, limbDistance);
    }
}
