package dev.aqua.client.music;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.ActionSetting;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.EnumSetting;
import dev.aqua.client.setting.NumberSetting;
import net.minecraft.util.Util;

/** Settings and controls for local music playback (R2 music capsule, R3 music widget). */
public final class MusicModule extends Module {
    public enum Repeat implements EnumSetting.Displayable {
        ALL("Repeat all"), ONE("Repeat one"), OFF("Stop at end");
        final String l;

        Repeat(String l) {
            this.l = l;
        }

        public String displayName() {
            return l;
        }
    }

    private final NumberSetting volume = add(new NumberSetting("volume", "Volume", 0.8, 0.0, 1.0, 0.05)
            .describe("Multiplied by Minecraft's Jukebox/Note Blocks volume").onChange(v -> MusicPlayer.get().applyVolume()));
    private final BoolSetting shuffle = add(new BoolSetting("shuffle", "Shuffle", false));
    private final EnumSetting<Repeat> repeat = add(new EnumSetting<>("repeat", "Repeat", Repeat.ALL));

    public MusicModule() {
        super("music", "Music Player", "Plays your own .ogg files from config/aqua/music with .lrc lyrics.", Category.QOL, true,
                "music", "songs", "lyrics", "spotify", "audio", "player");
        add(new ActionSetting("play_pause", "Playback", "Play / Pause", () -> MusicPlayer.get().togglePause()));
        add(new ActionSetting("next", "Next track", "Next", () -> MusicPlayer.get().next()));
        add(new ActionSetting("previous", "Previous track", "Previous", () -> MusicPlayer.get().previous()));
        add(new ActionSetting("rescan", "Library", "Rescan folder", () -> MusicPlayer.get().rescan()));
        add(new ActionSetting("open", "Music folder", "Open folder", () -> Util.getOperatingSystem().open(MusicPlayer.get().folder())));
        hideFromModuleList();
    }

    @Override
    protected void onEnable() {
        MusicPlayer.get().rescan();
    }

    @Override
    protected void onDisable() {
        MusicPlayer.get().stop();
    }

    public float volume() {
        return volume.f();
    }

    public boolean shuffle() {
        return shuffle.on();
    }

    public boolean repeatOne() {
        return repeat.is(Repeat.ONE);
    }

    public boolean autoAdvance() {
        return !repeat.is(Repeat.OFF);
    }
}
