package dev.aqua.client.mixin;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.perf.UnfocusedFps;
import net.minecraft.client.option.InactivityFpsLimiter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InactivityFpsLimiter.class)
public abstract class InactivityFpsLimiterMixin {
    @Inject(method = "update", at = @At("RETURN"), cancellable = true)
    private void aqua$unfocused(CallbackInfoReturnable<Integer> cir) {
        UnfocusedFps m = Aqua.modules() != null ? Aqua.modules().get(UnfocusedFps.class) : null;
        if (m != null) {
            cir.setReturnValue(m.limit(cir.getReturnValueI()));
        }
    }
}
