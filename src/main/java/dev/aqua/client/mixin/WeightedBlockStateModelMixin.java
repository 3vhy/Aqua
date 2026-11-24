package dev.aqua.client.mixin;

import dev.aqua.client.module.visual.NoBlockRotation;
import net.minecraft.client.render.model.WeightedBlockStateModel;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** No Block Rotation: always pick the same model variant. */
@Mixin(WeightedBlockStateModel.class)
public abstract class WeightedBlockStateModelMixin {
    @ModifyVariable(method = "addParts", at = @At("HEAD"), argsOnly = true)
    private Random aqua$fixedVariant(Random random) {
        return NoBlockRotation.pick(random);
    }
}
