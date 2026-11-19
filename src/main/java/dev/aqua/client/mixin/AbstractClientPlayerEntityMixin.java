package dev.aqua.client.mixin;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.qol.SkinProtect;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Skin Protect: your own skin, on your own screen (third person, first-person arm, previews). */
@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void aqua$skin(CallbackInfoReturnable<SkinTextures> cir) {
        if (Aqua.modules() == null || (Object) this != MinecraftClient.getInstance().player) {
            return;
        }
        SkinProtect sp = Aqua.modules().get(SkinProtect.class);
        SkinTextures o = sp != null ? sp.override() : null;
        if (o != null) {
            cir.setReturnValue(o);
        }
    }
}
