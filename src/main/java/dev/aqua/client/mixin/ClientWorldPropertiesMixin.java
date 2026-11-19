package dev.aqua.client.mixin;

import dev.aqua.client.Hooks;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Time Changer: visual time of day (the real server time keeps ticking underneath). */
@Mixin(ClientWorld.Properties.class)
public abstract class ClientWorldPropertiesMixin {
    @Inject(method = "getTimeOfDay", at = @At("RETURN"), cancellable = true)
    private void aqua$visualTime(CallbackInfoReturnable<Long> cir) {
        long t = Hooks.visualTimeOfDay(cir.getReturnValueJ());
        if (t != cir.getReturnValueJ()) {
            cir.setReturnValue(t);
        }
    }
}
