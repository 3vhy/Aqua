package dev.aqua.client.hud;

import dev.aqua.client.Aqua;
import dev.aqua.client.AquaClient;
import dev.aqua.client.module.cosmetic.WorldJoinEffect;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Aqua draws its HUD as a Fabric HUD layer. Some HUD mods and modded clients replace Minecraft's
 * HUD drawing completely, and then Fabric's layers (Aqua's included) never run. This notices a frame
 * in which the HUD was drawn without Aqua's layer and draws Aqua's HUD directly instead.
 */
public final class HudFallback {
    private static boolean layerRan;
    private static boolean reported;
    private static boolean active;

    private HudFallback() {}

    /** Called from Aqua's Fabric HUD layer. */
    public static void layerRan() {
        layerRan = true;
    }

    /** Whether Aqua is currently drawing its HUD itself (shown in Settings for troubleshooting). */
    public static boolean active() {
        return active;
    }

    public static void beforeVanillaHud() {
        layerRan = false;
    }

    public static void afterVanillaHud(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (layerRan || mc.options.hudHidden || mc.world == null || Aqua.hud() == null) {
            active = false;
            return;
        }
        if (!reported) {
            reported = true;
            AquaClient.LOGGER.warn("Fabric HUD layers did not run (another mod replaces the vanilla HUD); Aqua is drawing its HUD directly");
        }
        active = true;
        draw(ctx);
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
