package dev.aqua.client.mixin;

import dev.aqua.client.Hooks;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Weather Changer: visual rain/thunder on the client world only. */
@Mixin(World.class)
public abstract class WorldMixin {
    @Inject(method = "getRainGradient", at = @At("RETURN"), cancellable = true)
    private void aqua$rain(float tickProgress, CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof ClientWorld) {
            Float v = Hooks.rainOverride();
            if (v != null) {
                cir.setReturnValue(v);
            }
        }
    }

    @Inject(method = "getThunderGradient", at = @At("RETURN"), cancellable = true)
    private void aqua$thunder(float tickProgress, CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof ClientWorld) {
            Float v = Hooks.thunderOverride();
            if (v != null) {
                cir.setReturnValue(v);
            }
        }
    }
}
