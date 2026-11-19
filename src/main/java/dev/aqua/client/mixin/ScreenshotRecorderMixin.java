package dev.aqua.client.mixin;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.qol.ScreenshotHelper;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.io.File;
import java.util.function.Consumer;

@Mixin(ScreenshotRecorder.class)
public abstract class ScreenshotRecorderMixin {
    @ModifyVariable(method = "saveScreenshot(Ljava/io/File;Ljava/lang/String;Lnet/minecraft/client/gl/Framebuffer;ILjava/util/function/Consumer;)V",
            at = @At("HEAD"), argsOnly = true)
    private static Consumer<Text> aqua$notify(Consumer<Text> receiver) {
        return text -> {
            ScreenshotHelper helper = Aqua.modules() != null ? Aqua.modules().get(ScreenshotHelper.class) : null;
            File file = null;
            if (text.getContent() instanceof TranslatableTextContent tr && "screenshot.success".equals(tr.getKey())) {
                for (Object arg : tr.getArgs()) {
                    if (arg instanceof Text t && t.getStyle().getClickEvent() instanceof ClickEvent.OpenFile open) {
                        file = open.file();
                    }
                }
            }
            if (helper != null && file != null) {
                helper.onSaved(file);
                if (helper.hideChatMessage()) {
                    return;
                }
            }
            receiver.accept(text);
        };
    }
}
