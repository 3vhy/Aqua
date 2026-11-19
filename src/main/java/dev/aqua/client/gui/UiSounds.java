package dev.aqua.client.gui;

import dev.aqua.client.Aqua;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;

/** Interface feedback sounds using vanilla's UI click at a configurable volume. */
public final class UiSounds {
    private static long last;

    private UiSounds() {}

    public static void click(float pitch) {
        play(pitch, 1f);
    }

    public static void toggle(boolean on) {
        play(on ? 1.35f : 0.95f, 0.9f);
    }

    private static void play(float pitch, float vol) {
        if (Aqua.ui() == null || !Aqua.ui().sounds.on()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - last < 25) {
            return;
        }
        last = now;
        float v = (float) Aqua.ui().soundVolume.d() * vol * 0.6f;
        if (v <= 0.001f) {
            return;
        }
        MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.UI_BUTTON_CLICK.value(), pitch, v));
    }
}
