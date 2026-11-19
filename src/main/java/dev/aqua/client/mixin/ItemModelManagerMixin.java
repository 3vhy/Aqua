package dev.aqua.client.mixin;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.cosmetic.Knives;
import dev.aqua.client.render.GlintAccess;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.util.HeldItemContext;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Marks swords held by your own player in hand contexts so they are drawn as knives. */
@Mixin(ItemModelManager.class)
public abstract class ItemModelManagerMixin {
    @Inject(method = "clearAndUpdate", at = @At("TAIL"))
    private void aqua$knife(ItemRenderState state, ItemStack stack, ItemDisplayContext context, World world, HeldItemContext holder, int seed,
                            CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (holder == null || mc.player == null || holder != mc.player || Aqua.modules() == null) {
            return;
        }
        if (context != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND && context != ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                && context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND && context != ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
            return;
        }
        Knives k = Aqua.modules().get(Knives.class);
        if (k != null && k.appliesTo(stack)) {
            ((GlintAccess) state).aqua$setKnife(context);
        }
    }
}
