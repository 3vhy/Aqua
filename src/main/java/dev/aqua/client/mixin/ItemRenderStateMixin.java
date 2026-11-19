package dev.aqua.client.mixin;

import net.minecraft.client.render.item.ItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ItemRenderState.class)
public abstract class ItemRenderStateMixin implements dev.aqua.client.render.GlintAccess {
    @Shadow
    private int layerCount;
    @Shadow
    private ItemRenderState.LayerRenderState[] layers;

    @Override
    public void aqua$forceGlint() {
        for (int i = 0; i < layerCount; i++) {
            layers[i].setGlint(ItemRenderState.Glint.STANDARD);
        }
    }
}
