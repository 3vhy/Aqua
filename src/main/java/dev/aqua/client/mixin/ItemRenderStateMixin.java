package dev.aqua.client.mixin;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.cosmetic.Knives;
import dev.aqua.client.render.GlintAccess;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderState.class)
public abstract class ItemRenderStateMixin implements GlintAccess {
    @Shadow
    private int layerCount;
    @Shadow
    private ItemRenderState.LayerRenderState[] layers;
    @Unique
    @Nullable
    private ItemDisplayContext aqua$knifeContext;

    @Override
    public void aqua$forceGlint() {
        for (int i = 0; i < layerCount; i++) {
            layers[i].setGlint(ItemRenderState.Glint.STANDARD);
        }
    }

    @Override
    public void aqua$setKnife(@Nullable ItemDisplayContext context) {
        this.aqua$knifeContext = context;
    }

    @Override
    @Nullable
    public ItemDisplayContext aqua$knife() {
        return aqua$knifeContext;
    }

    @Inject(method = "clear", at = @At("HEAD"))
    private void aqua$clear(CallbackInfo ci) {
        aqua$knifeContext = null;
    }

    /** Knife skins replace the drawn model; the item itself is untouched. */
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void aqua$knife(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, int overlay, int outline, CallbackInfo ci) {
        if (aqua$knifeContext == null || Aqua.modules() == null) {
            return;
        }
        Knives k = Aqua.modules().get(Knives.class);
        if (k != null && k.isEnabled()) {
            k.render(matrices, queue, aqua$knifeContext, light);
            ci.cancel();
        }
    }
}
