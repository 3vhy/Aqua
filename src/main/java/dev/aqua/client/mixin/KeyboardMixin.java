package dev.aqua.client.mixin;

import dev.aqua.client.Aqua;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public abstract class KeyboardMixin {
    @Inject(method = "onKey", at = @At("HEAD"))
    private void aqua$moduleBinds(long window, int action, KeyInput input, CallbackInfo ci) {
        if (Aqua.keybinds() == null || window != MinecraftClient.getInstance().getWindow().getHandle()) {
            return;
        }
        if (action == InputUtil.GLFW_PRESS) {
            Aqua.keybinds().onInput(InputUtil.Type.KEYSYM, input.key(), true);
        }
    }
}
