package dev.aqua.client.module.visual;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.ColorSetting;

/**
 * Hand Glow (R7 "Hand aura"): a glowing outline around the item in your hand in first person, drawn with the same
 * outline effect the game uses for glowing entities.
 */
public final class HandAura extends Module {
    private final ColorSetting color = add(new ColorSetting("color", "Colour", 0xFF3CFF7A, false, true));
    private final BoolSetting offhand = add(new BoolSetting("offhand", "Off-hand too", false));

    public HandAura() {
        super("hand_glow", "Hand Glow", "A glowing outline around the item in your hand.", Category.VISUALS, false,
                "hand aura", "item outline", "glow outline", "hand glow");
    }

    /** Outline colour for this hand's item, or 0 for none. */
    public int outline(boolean mainHand) {
        return isEnabled() && (mainHand || offhand.on()) ? 0xFF000000 | (color.color() & 0xFFFFFF) : 0;
    }
}
