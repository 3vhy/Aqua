package dev.aqua.client.mixin;

import dev.aqua.client.Aqua;
import dev.aqua.client.gui.AquaTitleScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    /** Swaps the vanilla title screen for Aqua's (when enabled). */
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen aqua$swapTitle(Screen screen) {
        if (screen instanceof TitleScreen && !(screen instanceof AquaTitleScreen) && Aqua.ui() != null && Aqua.ui().customTitle.on()) {
            if (AquaTitleScreen.vanillaRequested) {
                AquaTitleScreen.vanillaRequested = false;
                return screen;
            }
            return new AquaTitleScreen();
        }
        return screen;
    }
}
