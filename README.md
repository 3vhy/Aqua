# Aqua Client

A cosmetic, HUD and quality-of-life mod for **Minecraft Java Edition 1.21.11** on **Fabric**.

Aqua is a regular Fabric mod `.jar`: it goes in your `mods` folder and runs inside a normal Fabric
installation. **It is not a standalone launcher.**

Aqua contains **no cheats or gameplay automation**: no combat or movement modules, no ESP, x-ray,
tracers, freecam, fullbright or reach. `REFERENCE_FEATURES.md` lists every feature from the reference
videos, including everything that was excluded and why. Server rules still apply. Check the rules of
each server before you play, because no client mod is approved everywhere.

---

## Installation

### Requirements

| Component | Version | Required? |
|---|---|---|
| Minecraft Java Edition | **1.21.11** (not 1.21.1) | required |
| Java | 21 or newer (the official launcher bundles it) | required |
| Fabric Loader | **0.20.1** or newer | required |
| Fabric API | **0.141.6+1.21.11** or newer for 1.21.11 | required |
| Mod Menu | for 1.21.11 | optional (Aqua has its own menu) |
| Sodium / Iris | for 1.21.11 | optional, see *Compatibility* |

### Steps

1. Install **Fabric Loader** for 1.21.11 using the Fabric installer from <https://fabricmc.net/use/installer/>.
   In the official launcher this creates a profile called `fabric-loader-1.21.11`.
2. Start that profile once and then close the game. This creates the `mods` folder.
3. Download **Fabric API** for 1.21.11 (from Modrinth or CurseForge) and put its `.jar` in your `mods` folder.
4. Put `aqua-client-1.0.0+mc1.21.11.jar` in the same `mods` folder:
   * Windows: `%APPDATA%\.minecraft\mods`
   * macOS: `~/Library/Application Support/minecraft/mods`
   * Linux: `~/.minecraft/mods`
5. Start the `fabric-loader-1.21.11` profile. The Aqua title screen appears.
6. Press **Right Shift**, or choose **Aqua Settings** on the title screen, to open the menu.

To check the download, compare its SHA-256 checksum with `aqua-client-1.0.0+mc1.21.11.jar.sha256`:

```bash
sha256sum aqua-client-1.0.0+mc1.21.11.jar
```

---

## Controls

| Action | Default key | Rebind in |
|---|---|---|
| Open / close the Aqua menu | Right Shift | Aqua > Keybinds, or Options > Controls |
| Zoom (hold, scroll to adjust) | C | same |
| Emote wheel | B | same |
| Inspect held item | Y | same |
| HUD editor | unbound | same |
| Music play/pause, next | unbound | same |
| Toggle any module | unbound | the **Bind** badge on its row (right-click to clear) |

Pressing Escape closes popups first, then clears the search, then closes the menu. In the HUD editor,
the arrow keys nudge the selected widget (hold Shift for bigger steps), **L** locks it, **R** resets it,
**H** hides it, and holding **Alt** while dragging turns snapping off.

---

## Features

### Menu
A floating window based on the attached menu screenshot. It has a narrow sidebar with the Aqua emblem,
a module search (by name, description, keywords and setting names), and these sections: **HUD,
Visuals, Cosmetics, Quality of Life, Performance**, plus **Wardrobe, Themes, Profiles, Keybinds and
Settings**. Each module row shows its name and description on the left. On the right are a settings
chevron, a key-bind badge (with conflict warnings) and a switch. The settings open inline and include
switches, sliders, dropdowns, an HSV colour picker with alpha and theme-sync, text fields and buttons.
Every module has a **Reset module** button, and Settings has **Reset everything**.

### HUD (Fern-style)
Dark translucent capsules with an icon, bright values and small muted units separated by dots.
The widgets are:

* Watermark
* Account
* Performance: FPS, frame time and memory
* Position: coordinates, facing, biome, server and speed
* Network: ping and TPS
* Clock: local time and session length
* Key Binds panel
* Effects panel
* Module List: enabled cosmetic modules only
* Keystrokes with CPS
* CPS
* Armour and held-item durability
* Inventory preview
* Item counter
* Compass: shows only your own heading
* **Island**: brand, now playing and notifications
* Music card with synced lyrics
* Restyled scoreboard
* Custom crosshair

Values that aren't available show **N/A**. TPS is exact in singleplayer and shown as an estimate
(`~`) on servers.

**HUD editor** lets you drag widgets and snaps them to screen edges, centre lines, a grid and other
widgets, with alignment guides. Each widget has its own scale, opacity, anchor (nine points),
background, lock and reset. **Auto layout** arranges the Fern-style cluster. Widgets that have
nothing to show display clearly marked **SAMPLE** data. Widgets stay on screen when you resize the
window or change the GUI scale.

### Visuals
Custom Sky (procedural topographic contours or a gradient), Time Changer, Weather Changer, Custom
Rain, Custom Fog (colour, and it can only make fog thicker), World Tint, Block Outline, Animations
(visual swing length and style, equip animation, hand position and scale, item **inspect**
animation), Item Glow, Hand Shader.

### Cosmetics (visible only to you; nothing is networked)
Wings, Halo, Jump Circles and landing shockwave, Jump Effects, Dash Trail, Projectile Trails (past
path only, no prediction), Fireflies, Floating Cubes, Wind, **Sky Lyrics** (glowing 3D text from
lyrics or your own text), Totem Pop Effect, Hit Particles, Crystal Pulse (depth tested), World Join
Effect (typewriter title), Emotes (wheel plus wardrobe preview).

### Quality of life
Smooth Zoom, Toggle Sprint (holds the vanilla sprint key, so there is no omni-sprint), Music Player,
Screenshot notifications with a thumbnail, Chat Style (background and timestamps), Death Waypoints
(your own deaths only), Streamer Mode (hides your name, coordinates and server on your own screen).

### Performance
Effect Budget, Background FPS Limit and Particle Limiter. Under Settings you can also set **Effects
quality** (Low/Medium/High), **Cosmetic intensity**, **Reduced motion**, **Background blur** on or
off, and **Soft shadows**.

### Title screen
The Aqua emblem and wordmark intro with a light animated background. The **Singleplayer,
Multiplayer, Options, Aqua Settings and Quit** buttons open the real Minecraft screens. There are
also language and accessibility shortcuts and a one-click switch to the vanilla title screen. You can
turn the Aqua title screen off in Settings.

---

## Music and lyrics
1. Put **Ogg Vorbis** (`.ogg`) files in `config/aqua/music/`.
   * The title and artist come from the file's tags, or from a file name like `Artist - Title.ogg`.
   * Optional synced lyrics: add `Artist - Title.lrc` next to the song, using the standard
     `[mm:ss.xx] line` format. `[offset:]` is supported.
   * Optional cover art: add `Artist - Title.png` (or `.jpg`) or `cover.png` next to the song.
2. In Aqua > Quality of Life > Music Player, use **Rescan folder**, then **Play / Pause**.

Only `.ogg` files can be played. Other formats are listed as unsupported instead of failing silently.
No songs are bundled and nothing is streamed. Music plays through Minecraft's Jukebox/Note Blocks
volume.

---

## Configuration
Everything is stored under `config/aqua/`:

| Path | Contents |
|---|---|
| `aqua.json` | client-wide preferences and the active profile (versioned) |
| `profiles/<name>.json` | modules, settings, binds, HUD layout and theme for each profile |
| `exports/` | profiles you export to a file |
| `import/` | drop exported files here and press **Import** (processed files move to `import/done`) |
| `music/` | your music, lyrics and cover art |

Saves happen one second after a change and when the game closes. Each save goes to a temporary file
first and is then moved into place. A damaged file is renamed to `*.corrupt-<time>`, defaults are
restored, and an in-game notification tells you. Profiles can be shared as a text **share code**
(`AQUA1:...`): use the copy and paste buttons on the Profiles page.

---

## Building from source

```bash
./gradlew build
```

The output is `build/libs/aqua-client-1.0.0+mc1.21.11.jar` (remapped release jar) plus a `.sha256`
file. Toolchain versions are pinned in `gradle.properties`: Minecraft 1.21.11, Yarn
1.21.11+build.6, Fabric Loader 0.20.1, Fabric API 0.141.6+1.21.11, Loom 1.14.10, Gradle 9.2.1 and
Java 21.

To run the in-game test suite you need Linux with Xvfb, Mesa and ffmpeg. Each run launches the game
twice; the second launch checks that settings were restored.

```bash
./tools/run_gametests.sh
```

```bash
./tools/run_gametests.sh production
```

```bash
./tools/run_gametests.sh compat
```

The default mode runs in the development environment. `production` runs the remapped release jar in
the real (intermediary-mapped) game. `compat` does the same with unmodified Sodium, Iris and Mod Menu
jars from Modrinth added. Each run writes its results, logs and screenshots to
`build/test-results-<mode>/`. The music test uses a sine tone that is generated at test time
(`tools/make_test_music.sh`) and is never packaged.

### Replacing the logo
No logo file was included with the build request, so Aqua ships an original placeholder emblem. To
use the official artwork, save it as `branding/aqua_logo_source.png` (transparent background), then run:

```bash
python3 tools/make_logo.py
```

```bash
./gradlew build
```

This regenerates the mod icon, the menu emblem, the small emblem and `branding/icons/aqua_<size>.png`
(16–512 px), keeping the artwork's proportions and transparent edges.

---

## Compatibility
See `TESTING.md` for exactly what was tested and the versions involved.

## Licence
Aqua Client is released under the MIT licence. The bundled Inter font is under the SIL OFL 1.1; see
`THIRD_PARTY_NOTICES.md`.
