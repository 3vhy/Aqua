package dev.aqua.client.mixin;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.visual.CustomGlint;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.TextureTransform;
import net.minecraft.util.Util;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Custom Glint: the enchantment glint's speed and pattern size. */
@Mixin(TextureTransform.class)
public abstract class TextureTransformMixin {
    @Inject(method = "getGlintTransformation", at = @At("HEAD"), cancellable = true)
    private static void aqua$glint(float scale, CallbackInfoReturnable<Matrix4f> cir) {
        CustomGlint g = Aqua.modules() != null ? Aqua.modules().get(CustomGlint.class) : null;
        if (g == null || !g.isEnabled()) {
            return;
        }
        long l = (long) (Util.getMeasuringTimeMs() * MinecraftClient.getInstance().options.getGlintSpeed().getValue() * 8.0 * g.speedFactor());
        float f = (float) (l % 110000L) / 110000.0F;
        float h = (float) (l % 30000L) / 30000.0F;
        Matrix4f m = new Matrix4f().translation(-f, h, 0.0F);
        m.rotateZ((float) (Math.PI / 18)).scale(scale * g.sizeFactor());
        cir.setReturnValue(m);
    }
}
