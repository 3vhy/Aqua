package dev.aqua.client.mixin;

import dev.aqua.client.Hooks;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin {
    /** While vanilla advances its stored time, report the real value (not the visual override). */
    @Inject(method = "tickTime", at = @At("HEAD"))
    private void aqua$tickStart(CallbackInfo ci) {
        Hooks.inTimeTick = true;
    }

    @Inject(method = "tickTime", at = @At("RETURN"))
    private void aqua$tickEnd(CallbackInfo ci) {
        Hooks.inTimeTick = false;
    }
}
