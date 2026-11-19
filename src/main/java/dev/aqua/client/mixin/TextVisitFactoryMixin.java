package dev.aqua.client.mixin;

import dev.aqua.client.module.qol.NameProtect;
import net.minecraft.text.TextVisitFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Name Protect: swaps your username for the alias in every string the game draws. */
@Mixin(TextVisitFactory.class)
public abstract class TextVisitFactoryMixin {
    @ModifyVariable(method = "visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static String aqua$formatted(String text) {
        return NameProtect.filter(text);
    }

    @ModifyVariable(method = "visitForwards(Ljava/lang/String;Lnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static String aqua$forwards(String text) {
        return NameProtect.filter(text);
    }

    @ModifyVariable(method = "visitBackwards(Ljava/lang/String;Lnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static String aqua$backwards(String text) {
        return NameProtect.filter(text);
    }

}
