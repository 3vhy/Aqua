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
| `dev` | Loom development run (Yarn-named) with Aqua and Fabric API | **202 / 202 checks passed** |
| `production` | **The release jar** `aqua-client-1.0.0+mc1.21.11.jar`, remapped, in the production (intermediary) game with the Fabric API release jar | **202 / 202 checks passed** |
| `compat` | Same as production, plus the unmodified Modrinth jars of Sodium, Iris and Mod Menu | **202 / 202 checks passed** |

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
| Menu | Every page opens (HUD, Visuals, Cosmetics, QoL, Performance, Wardrobe, Music, Themes, Profiles, Keybinds, Settings); type-to-search; Escape clears the search, then closes the menu |
| Airborne menu | While holding W and jumping: player is airborne → Right Shift opens the menu → forward, jump and sprint are released → no drift for 20 ticks → Right Shift closes it |
| Key binds | Capture records a key; Escape cancels capture without closing the menu; conflict found when binding W ("Walk Forward"); a module bind toggles its module in game |
| Modules | **All 64 modules** enabled (rendered at least 3 ticks without errors) and disabled cleanly; 92 boolean settings flipped |
| HUD editor | Real mouse drag moves a widget; Reset all restores the default layout |
| Cosmetics | First person, jump, third person (wings and halo), emote, wardrobe preview |
| 3D skins | A character replaces your player model in third person (character draw calls counted, so the render hook is proven to run in the remapped jar); Robo, Shark and an emote on a character; the held item stays in the character's hand |
| Wings, hats, capes | Devil wings with the glow hat and orbit, angel wings with crown and cape from behind |
| Knife skins | Karambit, katana, dagger and butterfly knife each replace the held sword in first person (knife draw calls counted); third person; knife inspect |
| Kill effects | A chicken hit with a sword dies and the lightning effect starts at that spot; soul and firework effects rendered |
| Wardrobe | All seven tabs (3D Skins, Wings, Hats, Capes, Knives, Emotes, Kill FX) render with live previews; choosing a 3D skin makes it your look |
| Keystrokes | Real mouse movement moves the dot in the pad between LMB and RMB |
| Spotify | A stand-in for Spotify (`tools/mock_mpris_player.py`) publishes songs on a private D-Bus session exactly as Spotify does on Linux, including Spotify's always-zero position. Checked: the song appears on the card, island and Music page; its time runs in step with real time; synced lyrics are found; the JPEG cover loads; play/pause and next reach the player; a title with an apostrophe is read correctly; the setting turns it off; closing the player clears it |
| Freelook | Holding Left Alt with real mouse movement turns the camera 84° while the player's direction stays at 0°; W still walks the player's way; releasing returns to first person |
| Covers and Sky Lyrics | A song whose tags carry a 1.2 MB embedded cover keeps its title and artist and shows the cover; a song with no cover gets one online; Sky Lyrics with curly quotes, dashes, accents and symbols, and the line follows your view after you turn around |
| Knives and swords | Karambit, butterfly knife, bayonet, stiletto and the four fantasy swords each replace the held sword (draw calls counted); inspect works without the Animations module |
| Second video set (R5–R8) | Staff list finds a `[Mod]` team prefix in the tab list; Hit FX and Swing Trails on a real hit; Damage FX on real damage; Hand Glow; Glass; Blob skin; footprints, afterimages, sparkles; pet follows you; rune jump circle; nebula, aurora and lava-cell skies generated and drawn; Ambience; Rain Splashes in real rain; Smooth Camera passes on all mouse movement (measured); Media Hider; Low Shield; Water Colour and No Block Rotation rebuild the world; Creamy Keys while typing; Colour Sync; Starred modules |
| HUD and debug options | With hitboxes on (F3+B) the HUD stays visible; it hides only while the F3 screen is open |
| HUD with other HUD mods | A simulated HUD-replacing mod stops Fabric's HUD layers: Aqua notices and draws its HUD directly (no false trigger during world loading); when the layer runs again Aqua goes back to it |
| Aqua badge | The `aqua:hello` handshake makes a round trip through the integrated server; your tab-list name carries the badge (with a scoreboard list objective so singleplayer draws the tab list); own nametag with the badge in third person |
| Name Protect | Drawn text shows the alias instead of your username (chat and tab list); switching it off shows the real name again |
| Skin Protect | Steve skin applied; the skin of the public player `jeb_` is looked up from Mojang and shown as yours |
| Lyrics | A song with no `.lrc` file gets synced lyrics from LRCLIB automatically (the test song is the public-domain hymn "Amazing Grace"; the audio is a generated tone); the Music page; the **Lyrics** switch turns lyrics off while the song keeps playing |
| Profiles | Create, duplicate, rename, switch; per-profile HUD layout restored; share-code export, import and rejection of an invalid code |
| Resolution | 854×480 at GUI scale 2 and 1920×1080 at GUI scale 4: every visible widget stays on screen; the menu fits |
| Resources | Full resource reload (F3+T path) and Aqua still renders afterwards |
| Shutdown | Leaves the world without crashing and ends on the title screen |

**Launch 2:** zoom factor, an enabled module, the theme (Mint), a moved HUD widget, a module key bind
and reduced-motion were all restored. A deliberately malformed profile is renamed to
`*.corrupt-<time>`, defaults are restored, and a warning is queued.

## Final runs

The final `production` and `compat` runs used the delivered jar, SHA-256 `152d3477c4cd114fbf7d68ec267d86922dc8ac4033ad7257dc0fe21d0cccb340`.
Launch 1 recorded 199 PASS lines and launch 2 recorded 3, with 0 failures and 0 skips in each mode.
The 199 lines in launch 1 include one PASS per module for enabling it and one for disabling it (128
lines for 64 modules). In the compat run, the log shows `Loading 58 mods` including `sodium 0.8.14+mc1.21.11`,
`iris 1.10.8+mc1.21.11` and `modmenu 17.0.1`. No Aqua errors or mixin failures were logged with them.
The only errors in that log come from the test environment: the offline test account (user
properties, Realms), an anisotropic-filtering option value, and the virtual display's missing cursor
shapes. Iris switched off one Sodium sky mixin by its own rule; this is Iris's normal behaviour and does not
affect Aqua.

Screenshots from these runs are in `docs/screenshots/`, grouped by milestone:
`1-menu_*`, `2-hud_*`, `3-cosmetics_*`, `4-final_*` (the `4-final_compat_*` files are from the
Sodium + Iris + Mod Menu run) and `5-update_*` (3D skins, knives, wings and hats, kill effects, Spotify, Freelook, the HUD with hitboxes on,
wardrobe tabs, badge, Name and Skin Protect, Music page and online lyrics, from the release-jar run). They were rendered with software OpenGL, so the FPS values shown are
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
* **Aqua badge between two players**: the handshake was tested with one Aqua client on its own
  integrated server. Two Aqua players on one LAN world were not tested, and nothing was tested on a
  public server (where the badge only appears next to your own name; see *Known limitations*).
* **Smooth Camera and Freelook on a real mouse**: the virtual display delivers mouse movement to
  Aqua but the test player does not turn from it, so Smooth Camera is checked by measuring the
  movement it passes on (all of it, spread over a few frames) rather than by the camera angle.
* **Windows cover art from Spotify**: the WinRT DataReader path was written and reviewed but not
  run (the tests run on Linux).
* **Spotify on Windows and macOS**: only the Linux reader was run, against a stand-in player. The
  Windows PowerShell reader and the macOS AppleScript reader were written against the documented
  Windows and Spotify interfaces and reviewed by hand, but **not executed**, because the tests run
  on Linux. The real Spotify app was not used on any system.
* **Online lookups depend on outside services**: the LRCLIB and Mojang lookups ran against the live
  services. If Mojang rate-limits the address a test runs from, the Skin Protect player lookup is
  recorded as `SKIP`, not as a pass. No run recorded a `SKIP`.

## Known limitations

* Aqua's cosmetics are not networked: other players never see your wings, halo, hats, cape, 3D
  skin, knife skins, kill effects, emotes or trails. Skin Protect and Name Protect change only your
  own screen.
* The Aqua badge needs the server to pass on Aqua's `aqua:hello` channel. That happens in
  singleplayer and on LAN worlds hosted by an Aqua user. Ordinary multiplayer servers do not, and
  Aqua has no central server, so there only your own name gets the badge.
* Automatic lyrics only work for songs that LRCLIB has synced lyrics for, and need an internet
  connection the first time (results are cached in `config/aqua/lyrics/`).
* Music playback supports **Ogg Vorbis only**. Other files are listed as unsupported.
* Motion blur, seen during fast turns in R1/R2, was not implemented (see `REFERENCE_FEATURES.md`).
* No official Aqua logo was attached, so a placeholder emblem ships; see the README to replace it.
