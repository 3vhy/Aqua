package dev.aqua.client.mixin;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.qol.SkinProtect;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Skin Protect: your head in the tab list. */
@Mixin(PlayerListEntry.class)
public abstract class PlayerListEntryMixin {
    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void aqua$skin(CallbackInfoReturnable<SkinTextures> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (Aqua.modules() == null || mc.player == null || !((PlayerListEntry) (Object) this).getProfile().id().equals(mc.player.getUuid())) {
            return;
        }
        SkinProtect sp = Aqua.modules().get(SkinProtect.class);
        SkinTextures o = sp != null ? sp.override() : null;
        if (o != null) {
            cir.setReturnValue(o);
        }
    }
}
