package dev.aqua.client.module.cosmetic;

import dev.aqua.client.Aqua;
import dev.aqua.client.cosmetic.Emotes;
import dev.aqua.client.gui.EmoteWheelScreen;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.BoolSetting;
import net.minecraft.client.option.Perspective;

/**
 * Emote wheel (R4 1:13 "Emotes", adapted): original emotes played on your own model. They are not sent
 * to the server, so only you see them.
 */
public final class EmotesModule extends Module {
    private final BoolSetting thirdPerson = add(new BoolSetting("third_person", "Switch to third person while emoting", true));
    private Perspective restore;

    public EmotesModule() {
        super("emotes", "Emotes", "Play emotes on your own player from a wheel (you only).", Category.COSMETICS, true,
                "emote", "dance", "wave", "wheel");
        hideFromModuleList();
    }

    @Override
    public void onTick() {
        while (Aqua.keybinds().emoteWheel.wasPressed()) {
            if (mc.currentScreen == null && mc.player != null) {
                mc.setScreen(new EmoteWheelScreen());
            }
        }
        Emotes.Emote e = Emotes.current();
        if (e != null && thirdPerson.on() && restore == null && mc.options.getPerspective().isFirstPerson()) {
            restore = mc.options.getPerspective();
            mc.options.setPerspective(Perspective.THIRD_PERSON_FRONT);
        }
        if (e == null && restore != null) {
            mc.options.setPerspective(restore);
            restore = null;
        }
        if (e != null && mc.player != null && (mc.player.handSwinging || mc.player.isUsingItem())) {
            Emotes.stop();
        }
    }

    @Override
    protected void onDisable() {
        Emotes.stop();
        if (restore != null) {
            mc.options.setPerspective(restore);
            restore = null;
        }
    }
}
