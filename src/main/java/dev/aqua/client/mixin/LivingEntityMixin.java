package dev.aqua.client.mixin;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.visual.Animations;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Visual swing length for your own client-side player (attack cooldown is unaffected). */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "getHandSwingDuration", at = @At("RETURN"), cancellable = true)
    private void aqua$swingDuration(CallbackInfoReturnable<Integer> cir) {
        if (Aqua.modules() == null || (Object) this != MinecraftClient.getInstance().player) {
            return;
        }
        Animations a = Aqua.modules().get(Animations.class);
        if (a != null && a.isEnabled()) {
            cir.setReturnValue(a.swingDuration(cir.getReturnValueI()));
        }
    }
}
