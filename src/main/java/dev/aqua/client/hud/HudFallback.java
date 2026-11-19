package dev.aqua.client.hud;

import dev.aqua.client.Aqua;
import dev.aqua.client.AquaClient;
import dev.aqua.client.module.cosmetic.WorldJoinEffect;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import org.jetbrains.annotations.Nullable;

/**
 * Aqua draws its HUD as a Fabric HUD layer. Some HUD mods and modded clients replace Minecraft's
 * HUD drawing completely, and then Fabric's layers (Aqua's included) never run. At the end of each
 * frame's HUD pass this checks whether Aqua's layer ran, and if Minecraft drew its HUD without it,
 * draws Aqua's HUD directly.
 */
public final class HudFallback {
    private static boolean layerRan;
    private static boolean reported;
    private static boolean active;
    @Nullable
    private static DrawContext frameContext;
    private static long layerRuns;
    private static long fallbackFrames;

    private HudFallback() {}

    /** Called from Aqua's Fabric HUD layer. */
    public static void layerRan() {
        layerRan = true;
        layerRuns++;
    }

    /** Whether Aqua drew its HUD itself in the last frame. */
    public static boolean active() {
        return active;
    }

    public static long layerRuns() {
        return layerRuns;
    }

    public static long fallbackFrames() {
        return fallbackFrames;
    }

    /** Before Minecraft's in-game HUD is drawn. */
    public static void beforeVanillaHud() {
        layerRan = false;
        frameContext = null;
    }

    /** After Minecraft's in-game HUD call; remembers the frame's draw context. */
    public static void afterVanillaHud(DrawContext ctx) {
        frameContext = ctx;
    }

    /**
     * End of the frame's HUD pass (after subtitles deferred behind a screen were drawn, which is
     * where Fabric's last HUD layers can run).
     */
    public static void endOfHud() {
        DrawContext ctx = frameContext;
        frameContext = null;
        if (ctx == null) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        // Minecraft itself skips the HUD while it is hidden (F1) and behind the world loading screen.
        if (layerRan || mc.options.hudHidden || mc.world == null || Aqua.hud() == null
                || mc.currentScreen instanceof LevelLoadingScreen) {
            active = false;
            return;
        }
        active = true;
        fallbackFrames++;
        if (!reported) {
            reported = true;
            AquaClient.LOGGER.warn("Fabric HUD layers did not run (another mod replaces the vanilla HUD); Aqua is drawing its HUD directly");
        }
        // This runs after any open screen was drawn, so only draw over the world or the chat box.
        if (mc.currentScreen == null || mc.currentScreen instanceof ChatScreen) {
            draw(ctx);
        }
    }

    /** Everything Aqua adds to the HUD, in the same order as the Fabric layers. */
    public static void draw(DrawContext ctx) {
        Aqua.hud().render(ctx);
        Aqua.notifications().render(ctx);
        WorldJoinEffect j = Aqua.modules().get(WorldJoinEffect.class);
        if (j != null) {
            j.render(ctx);
        }
    }
}
