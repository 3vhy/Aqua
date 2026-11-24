package dev.aqua.client.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Position and size of a container screen's background (used by Media Hider). */
@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {
    @Accessor("x")
    int aqua$x();

    @Accessor("y")
    int aqua$y();

    @Accessor("backgroundWidth")
    int aqua$width();

    @Accessor("backgroundHeight")
    int aqua$height();
}
