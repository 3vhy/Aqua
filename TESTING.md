# Aqua Client — Test Report

All results below come from **real Minecraft 1.21.11 clients** driven by the Fabric client game test
API (`src/gametest/java/dev/aqua/gametest/AquaClientGameTest.java`). Each run launches the game
**twice**:

* **Launch 1** carries out every check below and saves changed settings.
* **Launch 2** is a fresh start that checks those settings came back, then deliberately corrupts the
  profile file and checks that Aqua recovers from it.

```bash
./tools/run_gametests.sh
```

```bash
./tools/run_gametests.sh production
```

```bash
./tools/run_gametests.sh compat
```

## Environments tested

| Mode | What runs | Result |
|---|---|---|
| `dev` | Loom development run (Yarn-named) with Aqua and Fabric API | **148 / 148 checks passed** |
| `production` | **The release jar** `aqua-client-1.0.0+mc1.21.11.jar`, remapped, in the production (intermediary) game with the Fabric API release jar | **148 / 148 checks passed** |
| `compat` | Same as production, plus the unmodified Modrinth jars of Sodium, Iris and Mod Menu | **148 / 148 checks passed** |

Exact versions:

| Component | Version |
|---|---|
| Minecraft | 1.21.11 |
| Fabric Loader | 0.20.1 |
| Fabric API | 0.141.6+1.21.11 (client gametest module 4.3.5+bfe9fcf43e, tests only) |
| Yarn / Loom / Gradle | 1.21.11+build.6 / 1.14.10 / 9.2.1 |
| Java | OpenJDK 21.0.13 |
| Sodium | 0.8.14+mc1.21.11 (`mc1.21.11-0.8.14-fabric`) |
| Iris | 1.10.8+mc1.21.11 (no shader pack loaded) |
| Mod Menu | 17.0.1 |
| Graphics | Mesa llvmpipe software OpenGL on Xvfb (no GPU) |
| Audio | OpenAL Soft null device (`ALSOFT_DRIVERS=null`): the sound engine starts and streams, with no output |

## What the suite checks (launch 1)

| Area | Checks |
|---|---|
| Startup | Game is 1.21.11; Aqua title screen replaces the vanilla one (it still extends `TitleScreen` so other code keeps working) |
| No cheats | Whole-word scan of every module id and name against a list of 60 excluded cheat and service names |
| Music | Ogg metadata, duration and `.lrc` parsed; the local Ogg file **streams through Minecraft's sound engine**; pause, then resume from the paused position |
| Event features | Zoom narrows FOV while held and returns; Toggle Sprint sprints; emote wheel opens from its key and plays an emote; F2 screenshot shows the Island notification; card notification style with thumbnail; Hit Particles on hitting an entity; Totem Pop Effect on a real totem use; Death Waypoints records a real death and respawn |
| Menu | Every page opens (HUD, Visuals, Cosmetics, QoL, Performance, Wardrobe, Themes, Profiles, Keybinds, Settings); type-to-search; Escape clears the search, then closes the menu |
| Airborne menu | While holding W and jumping: player is airborne → Right Shift opens the menu → forward, jump and sprint are released → no drift for 20 ticks → Right Shift closes it |
| Key binds | Capture records a key; Escape cancels capture without closing the menu; conflict found when binding W ("Walk Forward"); a module bind toggles its module in game |
| Modules | **All 54 modules** enabled (rendered at least 3 ticks without errors) and disabled cleanly; 79 boolean settings flipped |
| HUD editor | Real mouse drag moves a widget; Reset all restores the default layout |
| Cosmetics | First person, jump, third person (wings and halo), emote, wardrobe preview |
| Profiles | Create, duplicate, rename, switch; per-profile HUD layout restored; share-code export, import and rejection of an invalid code |
| Resolution | 854×480 at GUI scale 2 and 1920×1080 at GUI scale 4: every visible widget stays on screen; the menu fits |
| Resources | Full resource reload (F3+T path) and Aqua still renders afterwards |
| Shutdown | Leaves the world without crashing and ends on the title screen |

**Launch 2:** zoom factor, an enabled module, the theme (Mint), a moved HUD widget, a module key bind
and reduced-motion were all restored. A deliberately malformed profile is renamed to
`*.corrupt-<time>`, defaults are restored, and a warning is queued.

## Final runs

The final `production` and `compat` runs used the delivered jar, SHA-256 `71377146275335d9c5a9336f165d6aed65754dfcf470796880463bca01c102e7`.
Launch 1 recorded 145 PASS lines and launch 2 recorded 3, with 0 failures in each mode. The 145
lines in launch 1 include one PASS per module for enabling it and one for disabling it (108 lines for
54 modules). In the compat run, the log shows `Loading 58 mods` including `sodium 0.8.14+mc1.21.11`,
`iris 1.10.8+mc1.21.11` and `modmenu 17.0.1`. No Aqua errors or mixin failures were logged with them.
Iris switched off one Sodium sky mixin by its own rule; this is Iris's normal behaviour and does not
affect Aqua.

Screenshots from these runs are in `docs/screenshots/`, grouped by milestone:
`1-menu_*`, `2-hud_*`, `3-cosmetics_*` and `4-final_*` (the `4-final_compat_*` files are from the
Sodium + Iris + Mod Menu run). They were rendered with software OpenGL, so the FPS values shown are
much lower than on a real GPU.

## Not covered by automated tests

* **Real GPU hardware**: all runs used software OpenGL. Shader compile/link was exercised by Mesa
  (llvmpipe), not by NVIDIA, AMD or Intel drivers.
* **Iris with a shader pack enabled.** Iris was loaded without a shader pack. Aqua's world effects use
  Fabric world-render events, and Iris may draw them differently or not at all while a pack is active.
  This was not tested.
* **Multiplayer servers**: TPS estimation from world-time packets, server ping, and server
  scoreboards were not tested against a remote server. Singleplayer TPS and the scoreboard
  placeholder were tested.
* **macOS and Windows**: font rasterisation uses headless Java2D, which works on all desktop JVMs,
  but only Linux was run.
* **Audible output**: playback was confirmed through the sound engine with a null output device. Nobody
  listened to it.
* **Mod Menu's own screen**: Mod Menu loaded without errors and the Aqua integration entrypoint is
  declared. The "Configure" button was not clicked by the tests.

## Known limitations

* Aqua's cosmetics are not networked: other players never see your wings, halo, emotes or trails.
* Music playback supports **Ogg Vorbis only**. Other files are listed as unsupported.
* Motion blur, seen during fast turns in R1/R2, was not implemented (see `REFERENCE_FEATURES.md`).
* No official Aqua logo was attached, so a placeholder emblem ships; see the README to replace it.
