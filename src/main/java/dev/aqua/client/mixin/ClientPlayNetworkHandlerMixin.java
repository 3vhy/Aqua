package dev.aqua.client.mixin;

import dev.aqua.client.Hooks;
import dev.aqua.client.util.TpsTracker;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onWorldTimeUpdate", at = @At("TAIL"))
    private void aqua$tps(WorldTimeUpdateS2CPacket packet, CallbackInfo ci) {
        TpsTracker.onWorldTimePacket();
    }

    @Inject(method = "onEntityStatus", at = @At("TAIL"))
    private void aqua$entityStatus(EntityStatusS2CPacket packet, CallbackInfo ci) {
        Hooks.onEntityStatus(packet);
    }
}
