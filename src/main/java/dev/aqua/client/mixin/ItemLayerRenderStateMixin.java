package dev.aqua.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.aqua.client.render.HandAuraRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/** Hand Aura: draws a glowing shell from the held item's own model faces right after the item. */
@Mixin(ItemRenderState.LayerRenderState.class)
public abstract class ItemLayerRenderStateMixin {
    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitItem(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/item/ItemDisplayContext;III[ILjava/util/List;Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/render/item/ItemRenderState$Glint;)V"))
    private void aqua$handAura(OrderedRenderCommandQueue queue, MatrixStack matrices, ItemDisplayContext ctx, int light, int overlay,
                               int outline, int[] tints, List<BakedQuad> quads, RenderLayer layer, ItemRenderState.Glint glint,
                               Operation<Void> original) {
        original.call(queue, matrices, ctx, light, overlay, outline, tints, quads, layer, glint);
        HandAuraRenderer.afterItem(queue, matrices, quads);
    }
}
