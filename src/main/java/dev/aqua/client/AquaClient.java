package dev.aqua.client;

import dev.aqua.client.config.ConfigManager;
import dev.aqua.client.config.UiSettings;
import dev.aqua.client.hud.HudManager;
import dev.aqua.client.hud.HudWidget;
import dev.aqua.client.module.KeybindManager;
import dev.aqua.client.module.Module;
import dev.aqua.client.module.ModuleManager;
import dev.aqua.client.music.MusicPlayer;
import dev.aqua.client.notify.Notifications;
import dev.aqua.client.render.AquaPipelines;
import dev.aqua.client.render.Anim;
import dev.aqua.client.render.IconAtlas;
import dev.aqua.client.render.font.AquaFont;
import dev.aqua.client.theme.ThemeManager;
import dev.aqua.client.util.FrameStats;
import dev.aqua.client.util.Session;
import dev.aqua.client.util.TpsTracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AquaClient implements ClientModInitializer {
    public static final String MOD_ID = "aqua";
    public static final Logger LOGGER = LoggerFactory.getLogger("Aqua");

    private static int lastScale = -1;

    @Override
    public void onInitializeClient() {
        AquaPipelines.init();
        Aqua.ui = new UiSettings();
        Aqua.themes = new ThemeManager();
        Aqua.modules = new ModuleManager();
        Aqua.hud = new HudManager();
        Aqua.config = new ConfigManager();
        Aqua.notifications = new Notifications();
        Aqua.keybinds = new KeybindManager();

        for (Module m : Modules.create()) {
            Aqua.modules.register(m);
            if (m instanceof HudWidget w) {
                Aqua.hud.register(w);
            }
        }
        AquaFont.load();
        Aqua.config.load();
        dev.aqua.client.net.Presence.init();

        HudElementRegistry.addLast(Identifier.of(MOD_ID, "hud"), (ctx, tick) -> {
            dev.aqua.client.hud.HudFallback.layerRan();
            Aqua.hud.render(ctx);
            Aqua.notifications.render(ctx);
        });
        Hooks.registerVanillaHudReplacements();

        ClientTickEvents.END_CLIENT_TICK.register(AquaClient::tick);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            Session.join();
            TpsTracker.reset();
            Hooks.onJoin();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            Session.leave();
            TpsTracker.reset();
        });
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            // World-space text atlases (sky lyrics, waypoint labels) are built once here, not mid-frame.
            AquaFont.worldAtlas(AquaFont.Weight.BOLD, 64);
            AquaFont.worldAtlas(AquaFont.Weight.SEMIBOLD, 64);
            for (String w : Aqua.config.drainWarnings()) {
                Aqua.notifications.warn("Aqua config", w);
            }
            MusicPlayer.get().rescan();
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            MusicPlayer.get().stop();
            dev.aqua.client.music.external.ExternalMedia.get().stop();
            Aqua.config.save();
        });
        Hooks.registerWorldRendering();
        LOGGER.info("Aqua Client {} initialised with {} modules", Aqua.VERSION, Aqua.modules.all().size());
    }

    /** Per-frame bookkeeping, called at the start of GUI rendering. */
    public static void onFrame() {
        Anim.frame();
        FrameStats.frame();
        MinecraftClient mc = MinecraftClient.getInstance();
        int scale = mc.getWindow().getScaleFactor();
        if (scale != lastScale) {
            lastScale = scale;
            AquaFont.invalidate();
            IconAtlas.invalidate();
            AquaFont.prewarm(4.4f, 4.8f, 5f, 5.2f, 5.4f, 5.6f, 5.8f, 6f, 6.2f, 6.4f, 6.5f, 9.5f, 11f);
            IconAtlas.prewarm(scale, 6f, 6.5f, 7f, 7.5f, 10f, 12f);
        }
    }

    private static void tick(MinecraftClient mc) {
        Aqua.modules.tick();
        Aqua.modules.get(dev.aqua.client.module.visual.Animations.class).tickInspect();
        Aqua.config.tick();
        MusicPlayer.get().tick();
        KeybindManager k = Aqua.keybinds;
        while (k.menu.wasPressed()) {
            if (mc.currentScreen == null) {
                KeyBinding.unpressAll();
                mc.setScreen(new dev.aqua.client.gui.AquaMenuScreen(null));
            }
        }
        while (k.hudEditor.wasPressed()) {
            if (mc.currentScreen == null) {
                KeyBinding.unpressAll();
                mc.setScreen(new dev.aqua.client.hud.editor.HudEditorScreen(null));
            }
        }
        while (k.musicToggle.wasPressed()) {
            MusicPlayer.get().togglePause();
        }
        while (k.musicNext.wasPressed()) {
            MusicPlayer.get().next();
        }
        Hooks.tick(mc);
    }
}
