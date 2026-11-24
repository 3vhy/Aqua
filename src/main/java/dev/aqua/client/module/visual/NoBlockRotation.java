package dev.aqua.client.module.visual;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import net.minecraft.util.math.random.Random;

/**
 * No Block Rotation (R6): blocks with random texture variants (grass, sand, stone, ...) all use the
 * same variant, for a cleaner, tiled look. Only how blocks look changes.
 */
public final class NoBlockRotation extends Module {
    private static final ThreadLocal<Random> FIXED = ThreadLocal.withInitial(() -> Random.create(0L));
    private static volatile boolean active;

    public NoBlockRotation() {
        super("no_block_rotation", "No Block Rotation", "Blocks like grass and sand use one texture direction everywhere.", Category.VISUALS,
                false, "no block rotation", "texture rotation", "random rotation", "tiled");
    }

    /** The random source used to pick a block's model variant (fixed while this is on). */
    public static Random pick(Random original) {
        if (!active) {
            return original;
        }
        Random r = FIXED.get();
        r.setSeed(0L);
        return r;
    }

    @Override
    protected void onEnable() {
        active = true;
        reloadChunks();
    }

    @Override
    protected void onDisable() {
        active = false;
        reloadChunks();
    }

    static void reloadChunks() {
        if (mc.worldRenderer != null && mc.world != null) {
            mc.execute(() -> mc.worldRenderer.reload());
        }
    }
}
