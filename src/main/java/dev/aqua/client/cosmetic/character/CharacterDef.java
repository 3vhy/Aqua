package dev.aqua.client.cosmetic.character;

import dev.aqua.client.cosmetic.Mesh;

/**
 * Geometry for one 3D character. Each method draws in the space of the matching player-model part
 * (pixels, y down, front = -z), so the character inherits walking, swinging, sneaking and emote poses.
 * Part pivots: head (0,0,0), body (0,0,0), right arm (-5,2,0), left arm (5,2,0), legs (+-1.9,12,0).
 */
public interface CharacterDef {
    default void head(Mesh m, float t) {}

    default void body(Mesh m, float t) {}

    default void rightArm(Mesh m, float t) {}

    default void leftArm(Mesh m, float t) {}

    default void rightLeg(Mesh m, float t) {}

    default void leftLeg(Mesh m, float t) {}

    /** Geometry fixed to the model root (does not follow body pitch). */
    default void root(Mesh m, float t) {}

    /** Vertical bob (pixels) for floating characters. */
    default float bob(float t) {
        return 0f;
    }
}
