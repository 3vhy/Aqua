package dev.aqua.client.music;

import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.Sound;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.sound.WeightedSoundSet;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.floatprovider.ConstantFloatProvider;
import net.minecraft.util.math.random.Random;

/**
 * Non-positional streamed sound for a local music file. Its location uses the {@code aqua_music}
 * namespace, which the SoundLoader mixin resolves to the file on disk.
 */
public final class AquaMusicSound extends AbstractSoundInstance {
    public static final String NAMESPACE = "aqua_music";

    public AquaMusicSound(int token, float volume) {
        super(Identifier.of(NAMESPACE, "track_" + token), SoundCategory.RECORDS, Random.create());
        this.volume = volume;
        this.pitch = 1f;
        this.relative = true;
        this.attenuationType = SoundInstance.AttenuationType.NONE;
        this.repeat = false;
        this.x = 0;
        this.y = 0;
        this.z = 0;
    }

    @Override
    public WeightedSoundSet getSoundSet(SoundManager soundManager) {
        this.sound = new Sound(this.id, ConstantFloatProvider.create(1f), ConstantFloatProvider.create(1f), 1,
                Sound.RegistrationType.FILE, true, false, 16);
        return new WeightedSoundSet(this.id, null);
    }

    public void setVolume(float v) {
        this.volume = v;
    }
}
