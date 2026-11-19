package dev.aqua.client.mixin;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.perf.ParticleLimiter;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {
    @Inject(method = "addParticle(Lnet/minecraft/client/particle/Particle;)V", at = @At("HEAD"), cancellable = true)
    private void aqua$limit(Particle particle, CallbackInfo ci) {
        ParticleLimiter l = Aqua.modules() != null ? Aqua.modules().get(ParticleLimiter.class) : null;
        if (l != null && !l.allow()) {
            ci.cancel();
        }
    }
}
