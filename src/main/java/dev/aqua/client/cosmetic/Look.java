package dev.aqua.client.cosmetic;

import dev.aqua.client.Aqua;
import dev.aqua.client.cosmetic.character.Characters;
import dev.aqua.client.module.cosmetic.Cape;
import dev.aqua.client.module.cosmetic.CharacterSkin;
import dev.aqua.client.module.cosmetic.Halo;
import dev.aqua.client.module.cosmetic.Hats;
import dev.aqua.client.module.cosmetic.Wings;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.render.entity.state.EntityRenderState;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Which Aqua cosmetics to draw on a player render state. Only ever attached to your own player
 * (cosmetics are not networked). The wardrobe sets a temporary preview look while it renders cards.
 */
public record Look(@Nullable Characters character, @Nullable Wings.Style wings, @Nullable Hats.Style hat,
                   @Nullable Cape.Style cape, boolean halo) {

    public static final RenderStateDataKey<Look> KEY = RenderStateDataKey.create(() -> "aqua:look");
    private static final ThreadLocal<Look> PREVIEW = new ThreadLocal<>();

    public static final Look NONE = new Look(null, null, null, null, false);

    /** The look built from the enabled cosmetic modules. */
    public static Look current() {
        if (Aqua.modules() == null) {
            return NONE;
        }
        CharacterSkin cs = Aqua.modules().get(CharacterSkin.class);
        Wings w = Aqua.modules().get(Wings.class);
        Hats h = Aqua.modules().get(Hats.class);
        Cape c = Aqua.modules().get(Cape.class);
        Halo halo = Aqua.modules().get(Halo.class);
        return new Look(cs != null && cs.isEnabled() ? cs.character() : null,
                w != null && w.isEnabled() ? w.style() : null,
                h != null && h.isEnabled() ? h.style() : null,
                c != null && c.isEnabled() ? c.style() : null,
                halo != null && halo.isEnabled());
    }

    /** The look to apply right now (wardrobe preview overrides the modules). */
    public static Look effective() {
        Look p = PREVIEW.get();
        return p != null ? p : current();
    }

    /** Runs {@code action} (typically InventoryScreen.drawEntity) with a preview look. */
    public static void preview(Look look, Runnable action) {
        Look prev = PREVIEW.get();
        PREVIEW.set(look);
        try {
            action.run();
        } finally {
            PREVIEW.set(prev);
        }
    }

    public static <T> T previewing(Look look, Supplier<T> action) {
        Look prev = PREVIEW.get();
        PREVIEW.set(look);
        try {
            return action.get();
        } finally {
            PREVIEW.set(prev);
        }
    }

    @Nullable
    public static Look of(EntityRenderState state) {
        return ((FabricRenderState) state).getData(KEY);
    }

    public static void attach(EntityRenderState state, Look look) {
        ((FabricRenderState) state).setData(KEY, look);
    }

    public Look withCharacter(@Nullable Characters c) {
        return new Look(c, wings, hat, cape, halo);
    }

    public Look withWings(@Nullable Wings.Style s) {
        return new Look(character, s, hat, cape, halo);
    }

    public Look withHat(@Nullable Hats.Style s) {
        return new Look(character, wings, s, cape, halo);
    }

    public Look withCape(@Nullable Cape.Style s) {
        return new Look(character, wings, hat, s, halo);
    }
}
