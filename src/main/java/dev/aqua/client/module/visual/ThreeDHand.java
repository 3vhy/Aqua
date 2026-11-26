package dev.aqua.client.module.visual;

import dev.aqua.client.Aqua;
import dev.aqua.client.cosmetic.character.Characters;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.module.cosmetic.CharacterSkin;
import org.jetbrains.annotations.Nullable;

/**
 * 3D Hand (reference video): in first person you see your 3D Character's own arm instead of the
 * skin arm. Needs 3D Character to be on.
 */
public final class ThreeDHand extends Module {
    private static long drawn;

    public ThreeDHand() {
        super("three_d_hand", "3D Hand", "Shows your 3D Character's arm in first person (needs 3D Character).", Category.VISUALS, false,
                "3d hand", "arm", "first person", "character hand");
    }

    /** The character whose arm to draw, or null for the normal arm. */
    @Nullable
    public Characters character() {
        if (!isEnabled()) {
            return null;
        }
        CharacterSkin cs = Aqua.modules().get(CharacterSkin.class);
        return cs != null && cs.isEnabled() ? cs.character() : null;
    }

    public static void noteDrawn() {
        drawn++;
    }

    /** First-person character arms drawn (used by the in-game tests). */
    public static long drawn() {
        return drawn;
    }
}
