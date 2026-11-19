package dev.aqua.client.mixin;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.visual.Animations;
import dev.aqua.client.module.visual.ItemGlow;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Animations (hand position, swing style, equip) and Item Glow for the first-person view. */
@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
    @ModifyVariable(method = "renderFirstPersonItem", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private float aqua$swing(float swingProgress) {
        Animations a = Aqua.modules().get(Animations.class);
        return a != null && a.isEnabled() ? a.swingProgress(swingProgress) : swingProgress;
    }

    @ModifyVariable(method = "renderFirstPersonItem", at = @At("HEAD"), argsOnly = true, ordinal = 3)
    private float aqua$equip(float equipProgress) {
        Animations a = Aqua.modules().get(Animations.class);
        return a != null && a.isEnabled() ? a.equipProgress(equipProgress) : equipProgress;
    }

    @Inject(method = "renderFirstPersonItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;push()V",
            shift = At.Shift.AFTER, ordinal = 0))
    private void aqua$transform(AbstractClientPlayerEntity player, float tickProgress, float pitch, Hand hand, float swingProgress,
                                ItemStack item, float equipProgress, MatrixStack matrices, OrderedRenderCommandQueue queue, int light,
                                CallbackInfo ci) {
        Animations a = Aqua.modules().get(Animations.class);
        if (a != null && a.isEnabled()) {
            a.apply(matrices, hand == Hand.MAIN_HAND);
        }
    }

    @Redirect(method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/ItemRenderState;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;III)V"))
    private void aqua$glint(ItemRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, int overlay, int seed,
                            LivingEntity entity, ItemStack stack, ItemDisplayContext context) {
        ItemGlow glow = Aqua.modules().get(ItemGlow.class);
        if (glow != null && glow.isEnabled() && entity == net.minecraft.client.MinecraftClient.getInstance().player
                && (context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND)) {
            boolean main = (context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
                    == (entity.getMainArm() == net.minecraft.util.Arm.RIGHT);
            if (main || glow.offhand()) {
                ((dev.aqua.client.render.GlintAccess) state).aqua$forceGlint();
            }
        }
        state.render(matrices, queue, light, overlay, seed);
    }
}
