package dev.aqua.client.module.qol;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.EnumSetting;
import dev.aqua.client.setting.NumberSetting;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/**
 * Creamy Keys (R7): a soft mechanical-keyboard "thock" for each key you press. The sounds were made
 * for Aqua (generated, not recorded) and play only for you.
 */
public final class CreamyKeys extends Module {
    public enum When implements EnumSetting.Displayable {
        TYPING("While typing in chat"), MENUS("In chat and menus"), ALWAYS("Every key press");
        final String l;

        When(String l) {
            this.l = l;
        }

        public String displayName() {
            return l;
        }
    }

    public static final SoundEvent THOCK = SoundEvent.of(Identifier.of("aqua", "key.thock"));

    private final EnumSetting<When> when = add(new EnumSetting<>("when", "Play", When.TYPING));
    private final NumberSetting volume = add(new NumberSetting("volume", "Volume", 0.5, 0.05, 1.0, 0.05));

    private int presses;
    private long lastAt;

    public CreamyKeys() {
        super("creamy_keys", "Creamy Keys", "Every key press sounds like a creamy mechanical keyboard.", Category.QOL, false,
                "creamy keys", "keyboard sounds", "typing sounds", "thock", "asmr");
    }

    /** Called for every key press (not repeats). */
    public void onKeyPress() {
        if (!isEnabled()) {
            return;
        }
        boolean chat = mc.currentScreen instanceof ChatScreen;
        boolean ok = switch (when.get()) {
            case TYPING -> chat;
            case MENUS -> mc.currentScreen != null;
            case ALWAYS -> true;
        };
        long now = System.currentTimeMillis();
        if (!ok || now - lastAt < 25) {
            return;
        }
        lastAt = now;
        presses++;
        float pitch = 0.92f + (float) Math.random() * 0.16f;
        mc.getSoundManager().play(PositionedSoundInstance.ui(THOCK, pitch, volume.f()));
    }

    /** Key sounds played so far (used by the in-game tests). */
    public int presses() {
        return presses;
    }
}
