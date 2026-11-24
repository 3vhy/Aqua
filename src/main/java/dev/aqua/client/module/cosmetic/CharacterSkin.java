package dev.aqua.client.module.cosmetic;

import dev.aqua.client.cosmetic.character.Characters;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.EnumSetting;

/**
 * 3D character skins (inspired by the 67 Client wardrobe, R4 0:57-2:17) using Aqua's own original
 * characters. Replaces how your player looks on your screen only; hitbox, camera and gameplay are unchanged.
 */
public final class CharacterSkin extends Module {
    public enum Choice implements EnumSetting.Displayable {
        DROPLET(Characters.DROPLET), ROBO(Characters.ROBO), PENGUIN(Characters.PENGUIN), SEA_DRAGON(Characters.SEA_DRAGON),
        GHOST(Characters.GHOST), KNIGHT(Characters.KNIGHT), SHARK(Characters.SHARK), SNOWMAN(Characters.SNOWMAN), BLOB(Characters.BLOB);

        public final Characters character;

        Choice(Characters c) {
            this.character = c;
        }

        public String displayName() {
            return character.label;
        }
    }

    private final EnumSetting<Choice> choice = add(new EnumSetting<>("character", "Character", Choice.DROPLET));

    public CharacterSkin() {
        super("character_skin", "3D Skin", "Turns your player into a 3D character (you only).", Category.COSMETICS, false,
                "3d skin", "character", "model", "wardrobe", "avatar", "morph");
    }

    public Characters character() {
        return choice.get().character;
    }

    public void select(Characters c) {
        for (Choice ch : Choice.values()) {
            if (ch.character == c) {
                choice.set(ch);
            }
        }
    }
}
