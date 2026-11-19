package dev.aqua.client.render;

import net.minecraft.item.ItemDisplayContext;
import org.jetbrains.annotations.Nullable;

/** Extra per-item-render-state data added by ItemRenderStateMixin. */
public interface GlintAccess {
    void aqua$forceGlint();

    /** Marks this state to be drawn as an Aqua knife (null clears). */
    void aqua$setKnife(@Nullable ItemDisplayContext context);

    @Nullable
    ItemDisplayContext aqua$knife();
}
