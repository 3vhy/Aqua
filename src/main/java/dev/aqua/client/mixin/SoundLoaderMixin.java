package dev.aqua.client.mixin;

import dev.aqua.client.music.AquaMusicSound;
import dev.aqua.client.music.MusicPlayer;
import net.minecraft.client.sound.AudioStream;
import net.minecraft.client.sound.SoundLoader;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/** Resolves Aqua's local music sounds to files in config/aqua/music. */
@Mixin(SoundLoader.class)
public abstract class SoundLoaderMixin {
    @Inject(method = "loadStreamed", at = @At("HEAD"), cancellable = true)
    private void aqua$music(Identifier id, boolean repeatInstantly, CallbackInfoReturnable<CompletableFuture<AudioStream>> cir) {
        if (!AquaMusicSound.NAMESPACE.equals(id.getNamespace())) {
            return;
        }
        String path = id.getPath();
        int us = path.lastIndexOf('_');
        int dot = path.lastIndexOf('.');
        int token;
        try {
            token = Integer.parseInt(path.substring(us + 1, dot > us ? dot : path.length()));
        } catch (Exception e) {
            return;
        }
        cir.setReturnValue(CompletableFuture.supplyAsync(() -> {
            try {
                AudioStream s = MusicPlayer.openStream(token);
                if (s == null) {
                    throw new IOException("No Aqua music for token " + token);
                }
                return s;
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }, Util.getDownloadWorkerExecutor()));
    }
}
