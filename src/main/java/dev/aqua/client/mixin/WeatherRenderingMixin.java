package dev.aqua.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.aqua.client.Aqua;
import dev.aqua.client.module.visual.CustomRain;
import net.minecraft.client.render.WeatherRendering;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Custom Rain: tints rain and snow streaks. */
@Mixin(WeatherRendering.class)
public abstract class WeatherRenderingMixin {
    @ModifyExpressionValue(method = "renderPieces", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/math/ColorHelper;getWhite(F)I"))
    private int aqua$tint(int original) {
        CustomRain r = Aqua.modules() != null ? Aqua.modules().get(CustomRain.class) : null;
        return r != null && r.isEnabled() ? r.tint(original) : original;
    }
}
