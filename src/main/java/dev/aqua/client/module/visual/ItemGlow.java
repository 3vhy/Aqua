package dev.aqua.client.module.visual;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.BoolSetting;

/**
 * Glowing held item (R1/R2): forces the enchantment glint on items rendered in your own first-person
 * hands. Only the render state is touched; item data, attributes and behaviour are unchanged.
 */
public final class ItemGlow extends Module {
    private final BoolSetting offhand = add(new BoolSetting("offhand", "Off-hand too", true));

    public ItemGlow() {
        super("item_glow", "Item Glow", "Adds a shimmering glint to items in your hands.", Category.VISUALS, false,
                "glint", "item chams", "shimmer", "enchant glow");
    }

    public boolean offhand() {
        return offhand.on();
    }
}
