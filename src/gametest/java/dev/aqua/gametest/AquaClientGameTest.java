package dev.aqua.gametest;

import dev.aqua.client.Aqua;
import dev.aqua.client.cosmetic.Emotes;
import dev.aqua.client.gui.AquaMenuScreen;
import dev.aqua.client.gui.AquaTitleScreen;
import dev.aqua.client.gui.Ui;
import dev.aqua.client.hud.HudWidget;
import dev.aqua.client.hud.editor.HudEditorScreen;
import dev.aqua.client.module.Keybind;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.NumberSetting;
import dev.aqua.client.setting.Setting;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

/**
 * End-to-end tests run inside a real Minecraft 1.21.11 client (Fabric client game test API).
 * Phase 1 changes settings and saves; phase 2 (a second launch) verifies they were restored.
 */
@SuppressWarnings("UnstableApiUsage")
public final class AquaClientGameTest implements FabricClientGameTest {
    private final List<String> results = new ArrayList<>();
    private Path resultFile;

    private void pass(String what) {
        results.add("PASS  " + what);
        System.out.println("[AquaTest] PASS " + what);
        flush();
    }

    /** Records a check that could not be carried out for a reason outside the mod (reported, not passed). */
    private void skip(String what) {
        results.add("SKIP  " + what);
        System.out.println("[AquaTest] SKIP " + what);
        flush();
    }

    private void check(boolean ok, String what) {
        if (!ok) {
            results.add("FAIL  " + what);
            flush();
            throw new AssertionError("Aqua test failed: " + what);
        }
        pass(what);
    }

    private void flush() {
        try {
            Files.writeString(resultFile, String.join("\n", results) + "\n", StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        Path gameDir = FabricLoader.getInstance().getGameDir();
        String state = System.getProperty("aqua.test.stateDir");
        Path stateDir = state != null ? Path.of(state) : gameDir;
        try {
            Files.createDirectories(stateDir);
            copyMusicFixture(gameDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Path marker = stateDir.resolve("aqua-test-phase2");
        if (System.getenv("AQUA_SHOWCASE") != null) {
            resultFile = stateDir.resolve("aqua-showcase.txt");
            Showcase.run(context, this::pass);
            return;
        }
        boolean phase2 = Files.exists(marker);
        resultFile = stateDir.resolve(phase2 ? "aqua-test-results-phase2.txt" : "aqua-test-results-phase1.txt");
        if (phase2) {
            runPhase2(context, marker);
        } else {
            runPhase1(context, marker);
        }
    }

    /** Copies the generated test-only music fixture into the game's music folder (if provided). */
    private static void copyMusicFixture(Path gameDir) throws IOException {
        String fixture = System.getProperty("aqua.test.musicFixture");
        if (fixture == null || !Files.isDirectory(Path.of(fixture))) {
            return;
        }
        Path music = gameDir.resolve("config").resolve("aqua").resolve("music");
        Files.createDirectories(music);
        try (var files = Files.list(Path.of(fixture))) {
            for (Path f : (Iterable<Path>) files::iterator) {
                Files.copy(f, music.resolve(f.getFileName().toString()), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    // ------------------------------------------------------------------ phase 1

    private void runPhase1(ClientGameTestContext context, Path marker) {
        // Software rendering (llvmpipe) shares the CPU with the integrated server: cap the frame rate.
        context.runOnClient(mc -> mc.options.getMaxFps().setValue(30));
        context.getInput().resizeWindow(1600, 900);
        context.waitTicks(30);
        String version = context.computeOnClient(mc -> net.minecraft.SharedConstants.getGameVersion().name());
        check("1.21.11".equals(version), "Running on Minecraft " + version);
        boolean aquaTitle = context.computeOnClient(mc -> mc.currentScreen instanceof AquaTitleScreen);
        check(aquaTitle, "Aqua title screen replaces the vanilla title screen");
        context.waitTicks(40);
        context.takeScreenshot("01_title_screen");

        // Static scan: no cheat modules exist anywhere.
        String[] banned = {"kill aura", "killaura", "aura", "triggerbot", "trigger bot", "aim assist", "aimassist", "aimbot", "reach",
                "backtrack", "esp", "tracers", "tracer", "xray", "x ray", "freecam", "free cam", "freelook", "fullbright", "scaffold",
                "autoclicker", "auto clicker", "auto totem", "autototem", "inv totem", "velocity", "no fall", "nofall", "fly", "flight",
                "speed", "blink", "fake lag", "nametags", "radar", "staff list", "staff panel", "hitbox", "criticals", "chams", "auto tool",
                "auto riptide", "pearl catch", "pearl throw", "anchor", "crystal aura", "auto crystal", "shield breaker", "sprint reset",
                "piercing", "camera clip", "no water fog", "elytra glide", "autopilot", "base finder", "chunk finder", "anti vanish",
                "fake player", "bot"};
        List<String> ids = context.computeOnClient(mc -> {
            List<String> l = new ArrayList<>();
            for (Module m : Aqua.modules().all()) {
                l.add(m.id().toLowerCase(Locale.ROOT).replace('_', ' '));
                l.add(m.name().toLowerCase(Locale.ROOT).replace('-', ' '));
            }
            return l;
        });
        for (String id : ids) {
            String padded = " " + id + " ";
            for (String b : banned) {
                if (padded.contains(" " + b + " ")) {
                    check(false, "No excluded/cheat module present (found '" + id + "' matching '" + b + "')");
                }
            }
        }
        pass("No excluded/cheat module names or ids among " + (ids.size() / 2) + " modules (whole-word scan)");

        // Menu from the title screen.
        context.setScreen(() -> new AquaMenuScreen(new AquaTitleScreen()));
        context.waitTicks(15);
        context.takeScreenshot("02_menu_title_hud_page");

        try (TestSingleplayerContext sp = createWorld(context)) {
            sp.getClientWorld().waitForChunksRender();
            context.runOnClient(mc -> ((dev.aqua.client.module.cosmetic.WorldJoinEffect) Aqua.modules().byId("world_join_effect")).trigger());
            context.waitTicks(36);
            context.takeScreenshot("03a_world_join_effect");
            context.waitTicks(80);
            context.takeScreenshot("03_hud_default_layout");

            music(context);
            eventFeatures(context, sp);
            menuPages(context);
            airborneMenu(context);
            keybindCapture(context);
            toggleEveryModule(context);
            hudEditor(context);
            cosmetics(context);
            skinsAndKnives(context, sp);
            identity(context, sp);
            hudFallback(context);
            profilesAndLayouts(context);
            resizeAndScale(context);
            reloadResources(context);
            prepareRestart(context, marker);
        }
        context.waitTicks(10);
        context.takeScreenshot("19_after_leaving_world");
        boolean back = context.computeOnClient(mc -> mc.world == null);
        check(back, "Left the world without crashing");
    }

    private void music(ClientGameTestContext context) {
        context.runOnClient(mc -> dev.aqua.client.music.MusicPlayer.get().rescan());
        context.waitFor(mc -> !dev.aqua.client.music.MusicPlayer.get().scanning(), 200);
        String info = context.computeOnClient(mc -> {
            var lib = dev.aqua.client.music.MusicPlayer.get().library();
            if (lib.isEmpty()) {
                return "empty";
            }
            var t = lib.get(0);
            return t.title() + "|" + t.artist() + "|" + (t.durationMs() / 1000) + "|" + t.lyrics().size() + "|" + t.supported();
        });
        if ("empty".equals(info)) {
            pass("Music: no test fixture present (run tools/make_test_music.sh) - playback not exercised");
            return;
        }
        check(info.equals("Generated Tone|Aqua Test|40|6|true"), "Music: Ogg metadata, duration and .lrc lyrics parsed (" + info + ")");
        context.runOnClient(mc -> {
            Aqua.modules().byId("music_card").setEnabled(true, false);
            Aqua.modules().byId("sky_lyrics").setEnabled(true, false);
            dev.aqua.client.music.MusicPlayer.get().play(0);
        });
        context.waitTicks(80);
        String state = context.computeOnClient(mc -> dev.aqua.client.music.MusicPlayer.get().state() + "|"
                + dev.aqua.client.music.MusicPlayer.get().lyricIndex() + "|" + mc.getSoundManager().isPlaying(
                        (net.minecraft.client.sound.SoundInstance) field(dev.aqua.client.music.MusicPlayer.get(), "sound")));
        context.takeScreenshot("03b_music_island_card_sky_lyrics");
        check(state.startsWith("PLAYING|") && state.endsWith("|true"), "Music: local Ogg file streams through the sound engine (" + state + ")");
        context.runOnClient(mc -> dev.aqua.client.music.MusicPlayer.get().pause());
        context.waitTicks(5);
        boolean paused = context.computeOnClient(mc -> dev.aqua.client.music.MusicPlayer.get().state() == dev.aqua.client.music.MusicPlayer.State.PAUSED);
        context.runOnClient(mc -> dev.aqua.client.music.MusicPlayer.get().resume());
        context.waitTicks(30);
        String resumed = context.computeOnClient(mc -> dev.aqua.client.music.MusicPlayer.get().state() + "|" + dev.aqua.client.music.MusicPlayer.get().positionMs() / 1000);
        check(paused && resumed.startsWith("PLAYING"), "Music: pause and resume from position (" + resumed + "s)");
        onlineLyrics(context);
        context.runOnClient(mc -> {
            dev.aqua.client.music.MusicPlayer.get().stop();
            Aqua.modules().byId("music_card").setEnabled(false, false);
            Aqua.modules().byId("sky_lyrics").setEnabled(false, false);
        });
    }

    /** A track with no .lrc file gets synced lyrics from LRCLIB; turning lyrics off makes the player standalone. */
    private void onlineLyrics(ClientGameTestContext context) {
        int idx = context.computeOnClient(mc -> {
            var lib = dev.aqua.client.music.MusicPlayer.get().library();
            for (int i = 0; i < lib.size(); i++) {
                if (lib.get(i).title().equals("Amazing Grace") && lib.get(i).lyrics().isEmpty()) {
                    return i;
                }
            }
            return -1;
        });
        if (idx < 0) {
            pass("Music: online lyrics fixture not present - LRCLIB lookup not exercised");
            return;
        }
        context.runOnClient(mc -> dev.aqua.client.music.MusicPlayer.get().play(idx));
        context.waitFor(mc -> !dev.aqua.client.music.MusicPlayer.get().lyricStatus().startsWith("Searching"), 1200);
        String found = context.computeOnClient(mc -> {
            var p = dev.aqua.client.music.MusicPlayer.get();
            return p.lyricStatus() + "|" + p.lyricsFor(p.current()).size();
        });
        check(found.startsWith("Lyrics from LRCLIB|") && !found.endsWith("|0"),
                "Music: synced lyrics fetched automatically from LRCLIB for a song without a .lrc file (" + found + " lines)");
        context.runOnClient(mc -> dev.aqua.client.music.MusicPlayer.get().seek(15000));
        context.waitTicks(60);
        context.takeScreenshot("03c_music_online_lyrics");
        context.setScreen(() -> new AquaMenuScreen(null));
        context.runOnClient(mc -> setNav((AquaMenuScreen) mc.currentScreen, "MUSIC"));
        context.waitTicks(20);
        context.takeScreenshot("03d_music_page");
        context.setScreen(() -> null);
        context.runOnClient(mc -> Aqua.modules().get(dev.aqua.client.music.MusicModule.class).setLyrics(false));
        context.waitTicks(30);
        String off = context.computeOnClient(mc -> {
            var p = dev.aqua.client.music.MusicPlayer.get();
            return p.lyricStatus() + "|" + p.lyricsFor(p.current()).size() + "|" + p.lyricIndex() + "|" + p.state();
        });
        context.takeScreenshot("03e_music_lyrics_off_standalone");
        check(off.equals("Lyrics off|0|-1|PLAYING"), "Music: lyrics switch off leaves a standalone player that keeps playing (" + off + ")");
        context.runOnClient(mc -> Aqua.modules().get(dev.aqua.client.music.MusicModule.class).setLyrics(true));
    }

    /** Triggers event-driven features: zoom, toggle sprint, emote wheel, notifications, hits, totems, deaths. */
    private void eventFeatures(ClientGameTestContext context, TestSingleplayerContext sp) {
        // Zoom while holding the zoom key
        context.getInput().holdKey(o -> Aqua.keybinds().zoom);
        context.waitTicks(20);
        float zoomed = context.computeOnClient(mc -> Aqua.modules().get(dev.aqua.client.module.qol.Zoom.class).fovFactor());
        context.takeScreenshot("05a_zoom_held");
        context.getInput().releaseKey(o -> Aqua.keybinds().zoom);
        context.waitTicks(20);
        float normal = context.computeOnClient(mc -> Aqua.modules().get(dev.aqua.client.module.qol.Zoom.class).fovFactor());
        check(zoomed < 0.4f && normal > 0.95f, "Zoom narrows the view while held and returns smoothly (" + zoomed + " -> " + normal + ")");

        // Toggle sprint keeps the vanilla sprint key held
        context.runOnClient(mc -> Aqua.modules().byId("toggle_sprint").setEnabled(true, false));
        context.getInput().holdKey(o -> o.forwardKey);
        context.waitTicks(15);
        boolean sprinting = context.computeOnClient(mc -> mc.player.isSprinting());
        context.getInput().releaseKey(o -> o.forwardKey);
        context.runOnClient(mc -> Aqua.modules().byId("toggle_sprint").setEnabled(false, false));
        check(sprinting, "Toggle Sprint sprints while moving forward");

        // Emote wheel from its key
        context.getInput().pressKey(o -> Aqua.keybinds().emoteWheel);
        context.waitTicks(5);
        boolean wheel = context.computeOnClient(mc -> mc.currentScreen instanceof dev.aqua.client.gui.EmoteWheelScreen);
        context.takeScreenshot("05b_emote_wheel");
        context.getInput().pressKey(GLFW.GLFW_KEY_1);
        context.waitTicks(5);
        boolean waved = context.computeOnClient(mc -> Emotes.current() == Emotes.Emote.WAVE && mc.currentScreen == null);
        check(wheel && waved, "Emote wheel opens from its key and plays the chosen emote");
        context.runOnClient(mc -> Emotes.stop());
        context.waitTicks(10);

        // Screenshot notification with thumbnail (F2)
        context.getInput().pressKey(o -> o.screenshotKey);
        context.waitFor(mc -> Aqua.notifications().current() != null && "Screenshot saved".equals(Aqua.notifications().current().title), 200);
        context.waitTicks(5);
        context.takeScreenshot("05c_screenshot_notification_island");
        pass("Screenshot notification appears after F2");

        // Card notification style (R3 reference) with thumbnail and timer bar
        context.runOnClient(mc -> {
            Aqua.ui().notificationStyle.set(dev.aqua.client.config.UiSettings.NotificationStyle.CARD);
            mc.options.screenshotKey.setPressed(false);
        });
        context.getInput().pressKey(o -> o.screenshotKey);
        context.waitFor(mc -> Aqua.notifications().current() != null && "Screenshot saved".equals(Aqua.notifications().current().title), 200);
        context.runOnClient(mc -> Aqua.notifications().info("Card style", "Notification with a remaining-time bar"));
        context.waitTicks(10);
        context.takeScreenshot("05d_card_notifications");
        context.runOnClient(mc -> Aqua.ui().notificationStyle.set(dev.aqua.client.config.UiSettings.NotificationStyle.ISLAND));
        pass("Card notification style renders");

        // Hit particles: hit an armour stand in front of the player
        context.runOnClient(mc -> {
            Aqua.modules().byId("hit_particles").setEnabled(true, false);
            Aqua.modules().byId("pop_effect").setEnabled(true, false);
            mc.player.setYaw(0f);
            mc.player.setPitch(20f);
            dev.aqua.client.render.world.Particles.clear();
        });
        sp.getServer().runCommand("gamemode survival @a");
        String pos = context.computeOnClient(mc -> String.format(Locale.ROOT, "%.2f %.2f %.2f", mc.player.getX(), mc.player.getY(), mc.player.getZ() + 1.8));
        sp.getServer().runCommand("summon minecraft:armor_stand " + pos);
        context.waitTicks(10);
        context.getInput().pressKey(o -> o.attackKey);
        context.waitTicks(2);
        int hitParticles = context.computeOnClient(mc -> dev.aqua.client.render.world.Particles.count());
        context.takeScreenshot("05e_hit_particles");
        check(hitParticles > 0, "Hit Particles spawn when you hit an entity (" + hitParticles + " particles)");

        // Totem pop effect
        context.runOnClient(mc -> dev.aqua.client.render.world.Particles.clear());
        sp.getServer().runCommand("item replace entity @a weapon.offhand with minecraft:totem_of_undying");
        context.waitTicks(5);
        sp.getServer().runCommand("damage @p 1000 minecraft:generic");
        context.waitTicks(4);
        int popParticles = context.computeOnClient(mc -> dev.aqua.client.render.world.Particles.count());
        context.takeScreenshot("05f_totem_pop_effect");
        check(popParticles > 0, "Totem Pop Effect bursts when a totem is used (" + popParticles + " particles)");
        context.waitTicks(40);

        // Death waypoint: die, respawn, marker recorded
        sp.getServer().runCommand("kill @a");
        context.waitForScreen(net.minecraft.client.gui.screen.DeathScreen.class);
        context.waitTicks(10);
        context.runOnClient(mc -> {
            mc.player.requestRespawn();
            mc.setScreen(null);
        });
        context.waitTicks(40);
        int deaths = context.computeOnClient(mc -> ((dev.aqua.client.module.qol.DeathWaypoints) Aqua.modules().byId("death_waypoints")).count());
        context.runOnClient(mc -> mc.player.setPitch(-5f));
        context.waitTicks(5);
        context.takeScreenshot("05g_death_waypoint");
        check(deaths == 1, "Death Waypoints records your death position (" + deaths + ")");
        sp.getServer().runCommand("kill @e[type=minecraft:armor_stand]");
        sp.getServer().runCommand("gamemode creative @a");
        context.runOnClient(mc -> {
            Aqua.modules().byId("hit_particles").setEnabled(false, false);
            Aqua.modules().byId("pop_effect").setEnabled(false, false);
        });
        context.waitTicks(10);
    }

    private void menuPages(ClientGameTestContext context) {
        context.setScreen(() -> new AquaMenuScreen(null));
        context.waitTicks(15);
        context.takeScreenshot("04_menu_hud");
        String[] navs = {"VISUALS", "COSMETICS", "QOL", "PERFORMANCE", "WARDROBE", "MUSIC", "THEMES", "PROFILES", "KEYBINDS", "SETTINGS"};
        int n = 5;
        for (String nav : navs) {
            context.runOnClient(mc -> setNav((AquaMenuScreen) mc.currentScreen, nav));
            context.waitTicks(12);
            context.takeScreenshot(String.format("%02d_menu_%s", n++, nav.toLowerCase(Locale.ROOT)));
        }
        // Search by keyword
        context.runOnClient(mc -> setNav((AquaMenuScreen) mc.currentScreen, "HUD"));
        context.getInput().typeChars("lyrics");
        context.waitTicks(10);
        context.takeScreenshot("14_menu_search_lyrics");
        context.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
        context.waitTicks(3);
        context.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
        context.waitTicks(15);
        boolean closed = context.computeOnClient(mc -> mc.currentScreen == null);
        check(closed, "Escape clears search, then closes the menu");
    }

    private static void setNav(AquaMenuScreen menu, String nav) {
        try {
            var m = AquaMenuScreen.class.getDeclaredMethod("setNav", AquaMenuScreen.Nav.class);
            m.setAccessible(true);
            m.invoke(menu, AquaMenuScreen.Nav.valueOf(nav));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private void airborneMenu(ClientGameTestContext context) {
        context.getInput().holdKey(o -> o.forwardKey);
        context.waitTicks(10);
        context.getInput().holdKey(o -> o.jumpKey);
        context.waitTicks(3);
        boolean airborne = context.computeOnClient(mc -> !mc.player.isOnGround());
        check(airborne, "Player is airborne while moving and jumping");
        context.getInput().pressKey(mc -> Aqua.keybinds().menu);
        context.waitTicks(2);
        boolean opened = context.computeOnClient(mc -> mc.currentScreen instanceof AquaMenuScreen);
        check(opened, "Right Shift opens the Aqua menu while airborne");
        boolean released = context.computeOnClient(mc -> !mc.options.forwardKey.isPressed() && !mc.options.jumpKey.isPressed()
                && !mc.options.sprintKey.isPressed());
        check(released, "Gameplay inputs are released when the menu opens");
        context.waitTicks(20);
        boolean stopped = context.computeOnClient(mc -> {
            var v = mc.player.getVelocity();
            return Math.abs(v.x) < 0.05 && Math.abs(v.z) < 0.05;
        });
        check(stopped, "No stuck movement while the menu is open");
        context.getInput().releaseKey(o -> o.forwardKey);
        context.getInput().releaseKey(o -> o.jumpKey);
        context.takeScreenshot("15_menu_opened_airborne");
        context.getInput().pressKey(mc -> Aqua.keybinds().menu);
        context.waitTicks(15);
        boolean closed = context.computeOnClient(mc -> mc.currentScreen == null);
        check(closed, "Menu key closes the menu again");
    }

    private void keybindCapture(ClientGameTestContext context) {
        context.setScreen(() -> new AquaMenuScreen(null));
        context.waitTicks(5);
        AtomicReference<Ui.KeyCapture> got = new AtomicReference<>();
        context.runOnClient(mc -> ((AquaMenuScreen) mc.currentScreen).ui().startCapture("test", got::set));
        context.getInput().pressKey(GLFW.GLFW_KEY_K);
        context.waitTicks(2);
        check(got.get() != null && got.get().code() == GLFW.GLFW_KEY_K && got.get().type() == InputUtil.Type.KEYSYM,
                "Keybind capture records the pressed key");
        got.set(null);
        context.runOnClient(mc -> ((AquaMenuScreen) mc.currentScreen).ui().startCapture("test2", got::set));
        context.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
        context.waitTicks(2);
        boolean stillOpen = context.computeOnClient(mc -> mc.currentScreen instanceof AquaMenuScreen);
        check(got.get() != null && got.get().cancelled() && stillOpen, "Escape cancels capture without closing the menu");
        List<String> conflicts = context.computeOnClient(mc -> {
            Module zoom = Aqua.modules().byId("fireflies");
            zoom.bind().set(InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_W);
            List<String> c = Aqua.keybinds().conflicts(zoom.bind(), zoom);
            return c;
        });
        check(!conflicts.isEmpty(), "Conflict detected when binding W (" + String.join(", ", conflicts) + ")");
        context.runOnClient(mc -> Aqua.modules().byId("fireflies").bind().set(InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G));
        context.setScreen(() -> null);
        context.waitTicks(3);
        boolean before = context.computeOnClient(mc -> Aqua.modules().byId("fireflies").isEnabled());
        context.getInput().pressKey(GLFW.GLFW_KEY_G);
        context.waitTicks(3);
        boolean after = context.computeOnClient(mc -> Aqua.modules().byId("fireflies").isEnabled());
        check(before != after, "Module key bind toggles the module in game");
        context.waitTicks(10);
        context.takeScreenshot("16_toggle_notification");
        context.runOnClient(mc -> Aqua.modules().byId("fireflies").setEnabled(false, false));
    }

    private void toggleEveryModule(ClientGameTestContext context) {
        List<String> ids = context.computeOnClient(mc -> {
            List<String> l = new ArrayList<>();
            for (Module m : Aqua.modules().all()) {
                l.add(m.id());
            }
            return l;
        });
        int count = 0;
        for (String id : ids) {
            boolean original = context.computeOnClient(mc -> Aqua.modules().byId(id).isEnabled());
            context.runOnClient(mc -> Aqua.modules().byId(id).setEnabled(true, false));
            context.waitTicks(3);
            boolean survived = context.computeOnClient(mc -> Aqua.modules().byId(id).isEnabled());
            check(survived, "Module '" + id + "' enabled and rendered without errors");
            context.runOnClient(mc -> Aqua.modules().byId(id).setEnabled(false, false));
            context.waitTicks(1);
            boolean off = context.computeOnClient(mc -> !Aqua.modules().byId(id).isEnabled());
            check(off, "Module '" + id + "' disabled cleanly");
            context.runOnClient(mc -> Aqua.modules().byId(id).setEnabled(original, false));
            count++;
        }
        pass("Enabled and disabled all " + count + " modules");
        // Exercise every boolean setting of every module once (render a frame with it flipped).
        int flipped = context.computeOnClient(mc -> {
            int n = 0;
            for (Module m : Aqua.modules().all()) {
                for (Setting<?> s : m.settings()) {
                    if (s instanceof BoolSetting b) {
                        b.toggle();
                        b.toggle();
                        n++;
                    }
                }
            }
            return n;
        });
        pass("Toggled " + flipped + " boolean settings back and forth");
    }

    private void hudEditor(ClientGameTestContext context) {
        context.setScreen(() -> new HudEditorScreen(null));
        context.waitTicks(15);
        context.takeScreenshot("17_hud_editor");
        // Drag the watermark with the mouse and verify it moved and stayed on screen.
        float[] before = context.computeOnClient(mc -> {
            HudWidget w = (HudWidget) Aqua.modules().byId("watermark");
            return new float[]{w.sx, w.sy, w.sw, w.sh};
        });
        double scale = context.computeOnClient(mc -> (double) mc.getWindow().getScaleFactor());
        context.getInput().setCursorPos((before[0] + before[2] / 2) * scale, (before[1] + before[3] / 2) * scale);
        context.getInput().holdMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
        context.waitTicks(2);
        for (int i = 0; i < 10; i++) {
            context.getInput().moveCursor(25 * scale / 3, 12 * scale / 3);
            context.waitTicks(1);
        }
        context.waitTicks(2);
        context.takeScreenshot("18_hud_editor_dragging");
        context.getInput().releaseMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
        context.waitTicks(3);
        float[] after = context.computeOnClient(mc -> {
            HudWidget w = (HudWidget) Aqua.modules().byId("watermark");
            return new float[]{w.sx, w.sy, w.sw, w.sh};
        });
        check(Math.abs(after[0] - before[0]) > 10 || Math.abs(after[1] - before[1]) > 5, "HUD editor drag moves a widget");
        context.runOnClient(mc -> {
            for (HudWidget w : Aqua.hud().widgets()) {
                w.resetLayout();
            }
            HudEditorScreen.autoArrange();
        });
        boolean restored = context.computeOnClient(mc -> {
            HudWidget w = (HudWidget) Aqua.modules().byId("watermark");
            return w.offX() == 5f && w.offY() == 5f;
        });
        check(restored, "HUD editor 'Reset all' restores the default layout");
        context.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
        context.waitTicks(3);
        context.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
        context.waitTicks(5);
    }

    private void cosmetics(ClientGameTestContext context) {
        context.runOnClient(mc -> {
            for (String id : new String[]{"custom_sky", "fireflies", "jump_circles", "jump_effects", "wings", "halo", "floating_cubes",
                    "sky_lyrics", "dash_trail", "hand_shader", "item_glow", "block_outline", "crosshair", "wind", "music_card"}) {
                Aqua.modules().byId(id).setEnabled(true, false);
            }
            mc.player.getInventory().setSelectedSlot(0);
            ((dev.aqua.client.setting.EnumSetting<dev.aqua.client.module.cosmetic.SkyLyrics.Source>) find("sky_lyrics", "source"))
                    .set(dev.aqua.client.module.cosmetic.SkyLyrics.Source.CUSTOM);
        });
        context.waitTicks(60);
        context.takeScreenshot("20_cosmetics_first_person");
        context.getInput().holdKeyFor(o -> o.jumpKey, 3);
        context.waitTicks(4);
        context.takeScreenshot("21_cosmetics_jump");
        context.runOnClient(mc -> mc.options.setPerspective(Perspective.THIRD_PERSON_FRONT));
        context.waitTicks(20);
        context.takeScreenshot("22_cosmetics_third_person_wings_halo");
        context.runOnClient(mc -> Emotes.play(Emotes.Emote.WAVE));
        context.waitTicks(15);
        context.takeScreenshot("23_emote_wave");
        context.runOnClient(mc -> {
            mc.options.setPerspective(Perspective.FIRST_PERSON);
            Emotes.stop();
        });
        context.setScreen(() -> new AquaMenuScreen(null));
        context.runOnClient(mc -> setNav((AquaMenuScreen) mc.currentScreen, "WARDROBE"));
        context.waitTicks(20);
        context.takeScreenshot("24_wardrobe_preview");
        context.setScreen(() -> null);
        context.runOnClient(mc -> {
            for (String id : new String[]{"custom_sky", "fireflies", "jump_circles", "jump_effects", "wings", "halo", "floating_cubes",
                    "sky_lyrics", "dash_trail", "hand_shader", "item_glow", "block_outline", "crosshair", "wind", "music_card"}) {
                Aqua.modules().byId(id).setEnabled(false, false);
            }
        });
        pass("Cosmetics rendered in first person, third person, emote and wardrobe preview");
    }

    /** 3D character skins, wing/hat/cape styles, knife skins, kill effects, the wardrobe tabs and the keystrokes mouse pad. */
    private void skinsAndKnives(ClientGameTestContext context, TestSingleplayerContext sp) {
        sp.getServer().runCommand("time set noon");
        sp.getServer().runCommand("item replace entity @a weapon.mainhand with minecraft:diamond_sword");
        context.runOnClient(mc -> {
            mc.player.getInventory().setSelectedSlot(0);
            var cs = Aqua.modules().get(dev.aqua.client.module.cosmetic.CharacterSkin.class);
            cs.select(dev.aqua.client.cosmetic.character.Characters.ROBO);
            cs.setEnabled(true, false);
            mc.options.setPerspective(Perspective.THIRD_PERSON_FRONT);
            mc.player.setPitch(0f);
        });
        long before = context.computeOnClient(mc -> dev.aqua.client.cosmetic.character.CharacterRenderer.drawn());
        context.waitTicks(20);
        long after = context.computeOnClient(mc -> dev.aqua.client.cosmetic.character.CharacterRenderer.drawn());
        context.takeScreenshot("31_3d_skin_robo");
        check(after > before, "3D skin replaces the player model in third person (" + (after - before) + " character frames)");
        context.runOnClient(mc -> Emotes.play(Emotes.Emote.WAVE));
        context.waitTicks(12);
        context.takeScreenshot("32_3d_skin_emote");
        context.runOnClient(mc -> {
            Emotes.stop();
            Aqua.modules().get(dev.aqua.client.module.cosmetic.CharacterSkin.class)
                    .select(dev.aqua.client.cosmetic.character.Characters.SHARK);
        });
        context.waitTicks(8);
        context.takeScreenshot("33_3d_skin_shark");
        context.runOnClient(mc -> Aqua.modules().get(dev.aqua.client.module.cosmetic.CharacterSkin.class).setEnabled(false, false));

        // Wings, hats and capes in each style (third person, back view for capes)
        context.runOnClient(mc -> {
            for (String id : new String[]{"wings", "hats", "orbit"}) {
                Aqua.modules().byId(id).setEnabled(true, false);
            }
            ((dev.aqua.client.setting.EnumSetting<dev.aqua.client.module.cosmetic.Wings.Style>) find("wings", "style"))
                    .set(dev.aqua.client.module.cosmetic.Wings.Style.DEVIL);
            ((dev.aqua.client.setting.EnumSetting<dev.aqua.client.module.cosmetic.Hats.Style>) find("hats", "style"))
                    .set(dev.aqua.client.module.cosmetic.Hats.Style.GLOW_CONE);
        });
        context.waitTicks(20);
        context.takeScreenshot("34_devil_wings_glow_hat_orbit");
        context.runOnClient(mc -> {
            ((dev.aqua.client.setting.EnumSetting<dev.aqua.client.module.cosmetic.Wings.Style>) find("wings", "style"))
                    .set(dev.aqua.client.module.cosmetic.Wings.Style.ANGEL);
            ((dev.aqua.client.setting.EnumSetting<dev.aqua.client.module.cosmetic.Hats.Style>) find("hats", "style"))
                    .set(dev.aqua.client.module.cosmetic.Hats.Style.CROWN);
            Aqua.modules().byId("orbit").setEnabled(false, false);
            Aqua.modules().byId("cape").setEnabled(true, false);
            mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        });
        context.waitTicks(20);
        context.takeScreenshot("35_angel_wings_crown_cape_back");
        context.runOnClient(mc -> {
            for (String id : new String[]{"wings", "hats", "cape"}) {
                Aqua.modules().byId(id).setEnabled(false, false);
            }
            mc.options.setPerspective(Perspective.FIRST_PERSON);
        });
        pass("Wing, hat, cape and orbit styles rendered (devil/angel wings, glow hat, crown)");

        // Knife skins on the held sword, first and third person
        for (var type : dev.aqua.client.module.cosmetic.Knives.Type.values()) {
            context.runOnClient(mc -> {
                var k = Aqua.modules().get(dev.aqua.client.module.cosmetic.Knives.class);
                k.select(type);
                k.setEnabled(true, false);
            });
            long k0 = context.computeOnClient(mc -> dev.aqua.client.module.cosmetic.Knives.drawn());
            context.waitTicks(10);
            long k1 = context.computeOnClient(mc -> dev.aqua.client.module.cosmetic.Knives.drawn());
            context.takeScreenshot("36_knife_" + type.name().toLowerCase(Locale.ROOT));
            check(k1 > k0, "Knife skin '" + type.displayName() + "' replaces the held sword in first person (" + (k1 - k0) + " frames)");
        }
        context.runOnClient(mc -> mc.options.setPerspective(Perspective.THIRD_PERSON_FRONT));
        context.waitTicks(10);
        context.takeScreenshot("37_knife_third_person");
        context.runOnClient(mc -> mc.options.setPerspective(Perspective.FIRST_PERSON));
        context.getInput().pressKey(o -> Aqua.keybinds().inspect);
        context.waitTicks(16);
        context.takeScreenshot("38_knife_inspect");
        context.waitTicks(50);

        // Kill effect: hit a chicken with the sword so it dies; the effect plays where it fell
        context.runOnClient(mc -> {
            var ke = Aqua.modules().get(dev.aqua.client.module.cosmetic.KillEffects.class);
            ((dev.aqua.client.setting.EnumSetting<dev.aqua.client.module.cosmetic.KillEffects.Effect>) find("kill_effects", "effect"))
                    .set(dev.aqua.client.module.cosmetic.KillEffects.Effect.LIGHTNING);
            ke.setEnabled(true, false);
            mc.player.setYaw(0f);
            mc.player.setPitch(15f);
        });
        sp.getServer().runCommand("gamemode survival @a");
        String pos = context.computeOnClient(mc -> String.format(Locale.ROOT, "%.2f %.2f %.2f", mc.player.getX(), mc.player.getY(), mc.player.getZ() + 2.2));
        sp.getServer().runCommand("summon minecraft:chicken " + pos + " {NoAI:1b}");
        context.waitTicks(30);
        int k0 = context.computeOnClient(mc -> Aqua.modules().get(dev.aqua.client.module.cosmetic.KillEffects.class).triggered());
        boolean attacked = context.computeOnClient(mc -> {
            var box = mc.player.getBoundingBox().expand(5);
            var list = mc.world.getEntitiesByClass(net.minecraft.entity.passive.ChickenEntity.class, box, e -> true);
            if (list.isEmpty()) {
                return false;
            }
            mc.interactionManager.attackEntity(mc.player, list.get(0));
            mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
            return true;
        });
        context.waitFor(mc -> Aqua.modules().get(dev.aqua.client.module.cosmetic.KillEffects.class).triggered() > k0, 100);
        context.waitTicks(3);
        context.takeScreenshot("39_kill_effect_lightning");
        int k1 = context.computeOnClient(mc -> Aqua.modules().get(dev.aqua.client.module.cosmetic.KillEffects.class).triggered());
        check(attacked && k1 > k0, "Kill effect plays when an entity you hit dies");
        context.waitTicks(30);
        for (var fx : new dev.aqua.client.module.cosmetic.KillEffects.Effect[]{
                dev.aqua.client.module.cosmetic.KillEffects.Effect.SOUL, dev.aqua.client.module.cosmetic.KillEffects.Effect.FIREWORK}) {
            context.runOnClient(mc -> Aqua.modules().get(dev.aqua.client.module.cosmetic.KillEffects.class)
                    .trigger(fx, mc.player.getEntityPos().add(0, 0, 3), 1f));
            context.waitTicks(10);
            context.takeScreenshot("40_kill_effect_" + fx.name().toLowerCase(Locale.ROOT));
        }
        sp.getServer().runCommand("gamemode creative @a");
        context.runOnClient(mc -> {
            Aqua.modules().get(dev.aqua.client.module.cosmetic.KillEffects.class).setEnabled(false, false);
            mc.player.setPitch(0f);
        });
        pass("Kill effect styles rendered (lightning, soul, firework)");

        // Wardrobe: every tab, with live 3D previews in the cards
        context.setScreen(() -> new AquaMenuScreen(null));
        context.runOnClient(mc -> setNav((AquaMenuScreen) mc.currentScreen, "WARDROBE"));
        String[] tabs = {"SKINS", "WINGS", "HATS", "CAPES", "KNIVES", "EMOTES", "KILL"};
        for (String tab : tabs) {
            context.runOnClient(mc -> setWardrobeTab((AquaMenuScreen) mc.currentScreen, tab));
            context.waitTicks(10);
            context.takeScreenshot("41_wardrobe_" + tab.toLowerCase(Locale.ROOT));
        }
        // Selecting a 3D skin card enables it
        context.runOnClient(mc -> setWardrobeTab((AquaMenuScreen) mc.currentScreen, "SKINS"));
        context.waitTicks(5);
        boolean picked = context.computeOnClient(mc -> {
            var cs = Aqua.modules().get(dev.aqua.client.module.cosmetic.CharacterSkin.class);
            cs.select(dev.aqua.client.cosmetic.character.Characters.PENGUIN);
            cs.setEnabled(true, false);
            return dev.aqua.client.cosmetic.Look.current().character() == dev.aqua.client.cosmetic.character.Characters.PENGUIN;
        });
        context.waitTicks(10);
        context.takeScreenshot("42_wardrobe_penguin_selected");
        check(picked, "Wardrobe: all 7 tabs render and a selected 3D skin becomes your look");
        context.setScreen(() -> null);
        context.runOnClient(mc -> {
            Aqua.modules().get(dev.aqua.client.module.cosmetic.CharacterSkin.class).setEnabled(false, false);
            Aqua.modules().get(dev.aqua.client.module.cosmetic.Knives.class).setEnabled(false, false);
        });

        // Keystrokes: mouse movement pad between LMB and RMB
        boolean locked = context.computeOnClient(mc -> mc.mouse.isCursorLocked());
        float peak = 0;
        for (int i = 0; i < 6; i++) {
            context.getInput().moveCursor(40, 12);
            context.waitTicks(1);
            peak = Math.max(peak, context.computeOnClient(mc -> Math.abs(dev.aqua.client.util.MouseTracker.x())));
        }
        context.takeScreenshot("43_keystrokes_mouse_pad");
        if (locked) {
            check(peak > 0.05f, "Keystrokes mouse pad follows real mouse movement (peak " + peak + ")");
        } else {
            pass("Keystrokes mouse pad drawn (cursor not captured by the virtual display, so movement was not measured)");
        }
    }

    /** Aqua badge (presence handshake, tab list, own nametag), Name Protect and Skin Protect. */
    private void identity(ClientGameTestContext context, TestSingleplayerContext sp) {
        context.waitFor(mc -> dev.aqua.client.net.Presence.knownCount() >= 1, 200);
        String presence = context.computeOnClient(mc -> dev.aqua.client.net.Presence.knownCount() + "|"
                + dev.aqua.client.net.Presence.isAquaUser(mc.player.getUuid()));
        check(presence.equals("1|true"), "Aqua presence handshake round trip through the integrated server (" + presence + ")");

        // Tab list badge: singleplayer only draws the tab list when a list objective exists
        sp.getServer().runCommand("scoreboard objectives add aqua_test dummy");
        sp.getServer().runCommand("scoreboard objectives setdisplay list aqua_test");
        String tabName = context.computeOnClient(mc -> {
            var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            net.minecraft.text.Text n = mc.inGameHud.getPlayerListHud().getPlayerName(entry);
            return (n.getContent() instanceof net.minecraft.text.ObjectTextContent ? "badge" : "no badge") + "|"
                    + n.getString().contains(mc.player.getGameProfile().name());
        });
        check(tabName.equals("badge|true"), "Aqua badge is prefixed to your name in the tab list (" + tabName + ")");
        context.getInput().holdKey(o -> o.playerListKey);
        context.waitTicks(6);
        context.takeScreenshot("44_tab_list_badge");

        // Name Protect: replaces your name in everything drawn
        String name = context.computeOnClient(mc -> mc.getSession().getUsername());
        context.runOnClient(mc -> {
            ((dev.aqua.client.setting.StringSetting) find("name_protect", "alias")).set("AquaUser");
            Aqua.modules().byId("name_protect").setEnabled(true, false);
        });
        context.waitTicks(4);
        context.takeScreenshot("45_tab_list_badge_name_protect");
        context.getInput().releaseKey(o -> o.playerListKey);
        String drawn = context.computeOnClient(mc -> {
            StringBuilder sb = new StringBuilder();
            net.minecraft.text.Text.literal("<" + name + "> hello").asOrderedText().accept((i, style, cp) -> {
                sb.appendCodePoint(cp);
                return true;
            });
            mc.inGameHud.getChatHud().addMessage(net.minecraft.text.Text.literal("<" + name + "> Name Protect hides this name"));
            return sb.toString();
        });
        context.waitTicks(4);
        context.takeScreenshot("46_name_protect_chat");
        check(drawn.equals("<AquaUser> hello"), "Name Protect replaces your username in drawn text (" + drawn + ")");
        context.runOnClient(mc -> Aqua.modules().byId("name_protect").setEnabled(false, false));
        String restored = context.computeOnClient(mc -> dev.aqua.client.module.qol.NameProtect.filter("<" + name + ">"));
        check(restored.equals("<" + name + ">"), "Name Protect off shows your real name again");

        // Own nametag with badge in third person
        context.runOnClient(mc -> {
            ((BoolSetting) find("aqua_badge", "own_nametag")).set(true);
            mc.options.setPerspective(Perspective.THIRD_PERSON_FRONT);
        });
        context.waitTicks(10);
        context.takeScreenshot("47_own_nametag_badge");

        // Skin Protect: Steve, then another player's skin looked up by name
        context.runOnClient(mc -> {
            ((dev.aqua.client.setting.EnumSetting<dev.aqua.client.module.qol.SkinProtect.Source>) find("skin_protect", "source"))
                    .set(dev.aqua.client.module.qol.SkinProtect.Source.STEVE);
            Aqua.modules().byId("skin_protect").setEnabled(true, false);
        });
        context.waitTicks(5);
        boolean steve = context.computeOnClient(mc -> mc.player.getSkin() == net.minecraft.client.util.DefaultSkinHelper.getSteve());
        context.takeScreenshot("48_skin_protect_steve");
        check(steve, "Skin Protect shows the Steve skin as yours");
        context.runOnClient(mc -> {
            ((dev.aqua.client.setting.StringSetting) find("skin_protect", "player")).set("jeb_");
            ((dev.aqua.client.setting.EnumSetting<dev.aqua.client.module.qol.SkinProtect.Source>) find("skin_protect", "source"))
                    .set(dev.aqua.client.module.qol.SkinProtect.Source.PLAYER);
        });
        context.waitFor(mc -> !Aqua.modules().get(dev.aqua.client.module.qol.SkinProtect.class).loading(), 1200);
        context.waitTicks(20);
        String skin = context.computeOnClient(mc -> {
            var spm = Aqua.modules().get(dev.aqua.client.module.qol.SkinProtect.class);
            return spm.status() + "|" + (spm.override() != null && mc.player.getSkin() == spm.override());
        });
        context.takeScreenshot("49_skin_protect_player");
        if (skin.contains("Mojang is busy")) {
            // Mojang's session server rate-limits shared addresses (HTTP 429); this is outside Aqua's control.
            skip("Skin Protect player lookup: Mojang's session server refused the request after 3 attempts (rate limited) - " + skin);
        } else {
            check(skin.equals("Showing jeb_'s skin|true"), "Skin Protect looks up a player by name and shows their skin as yours (" + skin + ")");
        }
        context.runOnClient(mc -> {
            Aqua.modules().byId("skin_protect").setEnabled(false, false);
            ((BoolSetting) find("aqua_badge", "own_nametag")).set(false);
            mc.options.setPerspective(Perspective.FIRST_PERSON);
        });
        sp.getServer().runCommand("scoreboard objectives remove aqua_test");
        context.waitTicks(5);
    }

    private static volatile boolean blockAquaLayer;
    private static boolean blockerInstalled;

    /**
     * Some HUD mods and modded clients replace Minecraft's HUD drawing so Fabric's HUD layers never
     * run. Simulate that by suppressing Aqua's layer, and check that Aqua draws its HUD directly.
     */
    private void hudFallback(ClientGameTestContext context) {
        context.runOnClient(mc -> {
            if (!blockerInstalled) {
                blockerInstalled = true;
                net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.replaceElement(
                        net.minecraft.util.Identifier.of("aqua", "hud"), original -> (ctx, tick) -> {
                            if (!blockAquaLayer) {
                                original.render(ctx, tick);
                            }
                        });
            }
            blockAquaLayer = true;
        });
        context.waitTicks(5);
        long f0 = context.computeOnClient(mc -> Aqua.hud().frames());
        context.waitTicks(15);
        String blocked = context.computeOnClient(mc -> dev.aqua.client.hud.HudFallback.active() + "|" + (Aqua.hud().frames() - f0));
        context.takeScreenshot("50_hud_drawn_by_fallback");
        check(blocked.startsWith("true|") && !blocked.endsWith("|0"),
                "HUD still draws when another mod stops Fabric's HUD layers (fallback active, frames drawn: " + blocked + ")");
        context.runOnClient(mc -> blockAquaLayer = false);
        context.waitTicks(10);
        boolean normal = context.computeOnClient(mc -> !dev.aqua.client.hud.HudFallback.active());
        check(normal, "HUD goes back to the normal Fabric layer when it runs again");
    }

    private static void setWardrobeTab(AquaMenuScreen menu, String tab) {
        try {
            Field pf = AquaMenuScreen.class.getDeclaredField("wardrobe");
            pf.setAccessible(true);
            Object page = pf.get(menu);
            Field tf = page.getClass().getDeclaredField("tab");
            tf.setAccessible(true);
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object v = Enum.valueOf((Class) tf.getType(), tab);
            tf.set(page, v);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private void profilesAndLayouts(ClientGameTestContext context) {
        String result = context.computeOnClient(mc -> {
            var cfg = Aqua.config();
            String base = cfg.activeProfile();
            HudWidget wm = (HudWidget) Aqua.modules().byId("watermark");
            float bx = wm.offX();
            String created = cfg.create("Test Layout");
            if (created == null) {
                return "create failed";
            }
            String dup = cfg.duplicate(created, "Test Copy");
            if (dup == null) {
                return "duplicate failed";
            }
            if (!cfg.rename(dup, "Test Renamed")) {
                return "rename failed";
            }
            cfg.switchTo(created);
            wm.setLayout(wm.anchor(), 77, wm.offY());
            cfg.save();
            cfg.switchTo(base);
            if (Math.abs(wm.offX() - bx) > 0.01f) {
                return "base layout not restored (" + wm.offX() + " vs " + bx + ")";
            }
            cfg.switchTo(created);
            if (Math.abs(wm.offX() - 77) > 0.01f) {
                return "profile layout not restored";
            }
            String code = cfg.exportCode(created);
            String imported = cfg.importCode(code, "Imported Test");
            if (imported == null) {
                return "share code import failed";
            }
            if (cfg.importCode("AQUA1:not-valid", null) != null) {
                return "invalid code accepted";
            }
            cfg.switchTo(base);
            cfg.delete("Test Renamed");
            cfg.delete(imported);
            return "ok";
        });
        check("ok".equals(result), "Profiles: create, duplicate, rename, switch, per-profile HUD layout, share code import/export (" + result + ")");
    }

    private String offscreen(ClientGameTestContext context) {
        return context.computeOnClient(mc -> {
            float sw = mc.getWindow().getScaledWidth();
            float sh = mc.getWindow().getScaledHeight();
            for (HudWidget w : Aqua.hud().widgets()) {
                if (w.isEnabled() && w.hasContent()) {
                    w.layout(sw, sh, false);
                    if (w.sx < -0.5f || w.sy < -0.5f || w.sx + w.sw > sw + 0.5f || w.sy + w.sh > sh + 0.5f) {
                        return w.id();
                    }
                }
            }
            return "";
        });
    }

    private void resizeAndScale(ClientGameTestContext context) {
        // Tight: 854x480 at GUI scale 2 (427x240 GUI units).
        context.getInput().resizeWindow(854, 480);
        context.waitTicks(10);
        context.runOnClient(mc -> {
            mc.options.getGuiScale().setValue(2);
            mc.onResolutionChanged();
        });
        context.waitTicks(10);
        int scale = context.computeOnClient(mc -> mc.getWindow().getScaleFactor());
        context.takeScreenshot("25_hud_854x480_gui" + scale);
        String off = offscreen(context);
        check(off.isEmpty(), "Widgets stay on screen at 854x480, GUI scale " + scale + (off.isEmpty() ? "" : " (" + off + ")"));
        context.setScreen(() -> new AquaMenuScreen(null));
        context.waitTicks(15);
        context.takeScreenshot("26_menu_854x480_gui" + scale);
        context.setScreen(() -> null);
        // Large GUI scale: 1920x1080 at GUI scale 4 (480x270 GUI units).
        context.getInput().resizeWindow(1920, 1080);
        context.waitTicks(10);
        context.runOnClient(mc -> {
            mc.options.getGuiScale().setValue(4);
            mc.onResolutionChanged();
        });
        context.waitTicks(10);
        scale = context.computeOnClient(mc -> mc.getWindow().getScaleFactor());
        check(scale == 4, "GUI scale 4 applied at 1920x1080");
        off = offscreen(context);
        check(off.isEmpty(), "Widgets stay on screen at 1920x1080, GUI scale 4" + (off.isEmpty() ? "" : " (" + off + ")"));
        context.takeScreenshot("27_hud_1920x1080_gui4");
        context.setScreen(() -> new AquaMenuScreen(null));
        context.waitTicks(15);
        context.takeScreenshot("27b_menu_1920x1080_gui4");
        context.setScreen(() -> null);
        context.runOnClient(mc -> {
            mc.options.getGuiScale().setValue(0);
            mc.onResolutionChanged();
        });
        context.getInput().resizeWindow(1600, 900);
        context.waitTicks(10);
        pass("Window resize and GUI scale changes handled");
    }

    private void reloadResources(ClientGameTestContext context) {
        context.runOnClient(MinecraftClient::reloadResources);
        context.waitFor(mc -> mc.getOverlay() == null, 1200);
        context.waitTicks(20);
        context.setScreen(() -> new AquaMenuScreen(null));
        context.waitTicks(15);
        context.takeScreenshot("28_menu_after_resource_reload");
        context.setScreen(() -> null);
        pass("Resource reload completed and Aqua still renders");
    }

    private void prepareRestart(ClientGameTestContext context, Path marker) {
        context.runOnClient(mc -> {
            ((NumberSetting) find("zoom", "factor")).set(7.5);
            Aqua.modules().byId("chat_style").setEnabled(true, false);
            Aqua.themes().select("Mint");
            HudWidget wm = (HudWidget) Aqua.modules().byId("network");
            wm.setLayout(wm.anchor(), 123, 45);
            Aqua.modules().byId("chat_style").bind().set(InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J);
            Aqua.ui().reducedMotion.set(true);
            Aqua.config().save();
        });
        try {
            Files.writeString(marker, "phase2", StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        pass("Settings changed and saved for the restart check");
    }

    /**
     * The harness ticks client and server in lockstep; under software GL the vanilla loading-screen
     * blur makes every frame slow, so blur is disabled while the world is created and restored after.
     */
    static TestSingleplayerContext createWorldFor(ClientGameTestContext context) {
        return createWorld(context);
    }

    private static TestSingleplayerContext createWorld(ClientGameTestContext context) {
        int blur = context.computeOnClient(mc -> mc.options.getMenuBackgroundBlurriness().getValue());
        context.runOnClient(mc -> mc.options.getMenuBackgroundBlurriness().setValue(0));
        TestSingleplayerContext sp = context.worldBuilder().adjustSettings(AquaClientGameTest::flat).create();
        context.runOnClient(mc -> mc.options.getMenuBackgroundBlurriness().setValue(blur));
        return sp;
    }

    /** Superflat keeps world creation well inside the harness's 60 second timeout under software GL. */
    private static void flat(net.minecraft.client.gui.screen.world.WorldCreator creator) {
        for (var type : creator.getNormalWorldTypes()) {
            if (type.preset() != null && type.preset().matchesKey(net.minecraft.world.gen.WorldPresets.FLAT)) {
                creator.setWorldType(type);
                return;
            }
        }
    }

    private static Setting<?> find(String module, String setting) {
        for (Setting<?> s : Aqua.modules().byId(module).settings()) {
            if (s.id().equals(setting)) {
                return s;
            }
        }
        throw new IllegalStateException(module + "." + setting);
    }

    // ------------------------------------------------------------------ phase 2 (after restart)

    private void runPhase2(ClientGameTestContext context, Path marker) {
        context.runOnClient(mc -> mc.options.getMaxFps().setValue(30));
        context.getInput().resizeWindow(1600, 900);
        context.waitTicks(30);
        String state = context.computeOnClient(mc -> {
            double zoom = ((NumberSetting) find("zoom", "factor")).d();
            boolean chat = Aqua.modules().byId("chat_style").isEnabled();
            String theme = Aqua.themes().presetName();
            HudWidget nw = (HudWidget) Aqua.modules().byId("network");
            Keybind kb = Aqua.modules().byId("chat_style").bind();
            boolean rm = Aqua.ui().reducedMotion.on();
            return zoom + "|" + chat + "|" + theme + "|" + nw.offX() + "," + nw.offY() + "|" + kb.code() + "|" + rm;
        });
        check(state.equals("7.5|true|Mint|123.0,45.0|" + GLFW.GLFW_KEY_J + "|true"), "Settings, theme, HUD layout and binds restored after restart (" + state + ")");
        context.takeScreenshot("29_title_after_restart_mint");
        try (TestSingleplayerContext sp = createWorld(context)) {
            // Fixed wait: this world only provides a backdrop for the screenshot.
            context.waitTicks(120);
            context.takeScreenshot("30_hud_after_restart_mint");
        }
        // Malformed config recovery
        context.runOnClient(mc -> {
            try {
                Files.writeString(Aqua.config().profileFile(Aqua.config().activeProfile()), "{ this is not json", StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Aqua.config().load();
        });
        String recovered = context.computeOnClient(mc -> Aqua.themes().presetName() + "|" + Aqua.config().drainWarnings().size());
        check(recovered.startsWith("Aqua|") && !recovered.endsWith("|0"), "Malformed profile is quarantined and defaults restored (" + recovered + ")");
        // Restore a clean state for future runs.
        context.runOnClient(mc -> {
            Aqua.ui().reducedMotion.set(false);
            Aqua.config().save();
        });
        try {
            Files.deleteIfExists(marker);
        } catch (IOException ignored) {
        }
        pass("Phase 2 complete");
    }

    @SuppressWarnings("unused")
    private static Object field(Object o, String name) {
        try {
            Field f = o.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(o);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
