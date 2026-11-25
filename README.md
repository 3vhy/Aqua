# Aqua Client

A cosmetic, HUD and quality-of-life mod for **Minecraft Java Edition 1.21.11** on **Fabric**.

Aqua is a regular Fabric mod `.jar`: it goes in your `mods` folder and runs inside a normal Fabric
installation. **It is not a standalone launcher.**

Aqua contains **no cheats or gameplay automation**: no combat or movement modules, no ESP, x-ray,
tracers, freecam, fullbright or reach. (Freelook was added on request; it is a camera-only feature
that some servers still forbid, see *Quality of life*.) `REFERENCE_FEATURES.md` lists every feature from the reference
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

`dist/index.html` is a single-file download page with the jar built in, so it works on its own
wherever it is opened or hosted. It checks the jar's SHA-256 before the download starts. After
rebuilding the jar, regenerate it with `python3 tools/make_download_page.py`.

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
| Freelook (hold) | Left Alt | same |
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

### Glass interface
Every Aqua surface is **frosted glass**: the menu window, the sidebar, popups, tooltips, toasts, the
HUD editor, the emote wheel, the title screen card and (optionally) every HUD element. Glass blurs
whatever is behind it, the world, the title background or the HUD under a menu. It also adds a
theme tint, a soft sheen across the top and a lit rim along the edges. Bright backdrops are toned
down so text stays readable in daylight or snow.

Under **Settings → Interface** you can change:
* **Interface style**: Glass (default) or Solid (the earlier opaque look).
* **Glass frost**: 1 to 5, how strongly the glass blurs.
* **Glass tint**: how dark and solid the glass is.
* **Glass HUD**: whether HUD backgrounds are glass too.
* **Blur whole background**: also blurs the area around the menu window.

The blur runs on the GPU at a quarter of the screen size and only in frames that show glass. If the
graphics driver cannot run it, Aqua switches to solid panels by itself and logs the reason.

The rest of the game follows the same style:
* **Pause menu**: Minecraft's pause menu on a glass card. It keeps the vanilla buttons (and any
  that other mods add, such as Mod Menu's), with their actions and keyboard navigation. It adds
  an **Aqua Settings** button, your **starred modules as quick toggles**, the session time and
  the song that is playing. Turn it off with *Settings → Aqua pause menu*.
* **Minecraft's own menus**: buttons, sliders and text boxes in every menu use Aqua's style, in
  the options, world list, server list and other mods' screens. Labels, layout and behaviour stay
  vanilla. Turn it off with *Settings → Aqua-style Minecraft menus*.
* **Item tooltips**: a dark card with a lit, accent-tinted rim. Tooltips that bring their own
  style from a resource or data pack keep it.
* **Chat**: chat lines sit on frosted glass that forms one seamless pane (*Chat Style → Frosted
  glass*; Chat Style is now on by default). This works with chat open and closed.

### Menu
A floating window based on the attached menu screenshot. It has a narrow sidebar with the Aqua emblem,
a module search (by name, description, keywords and setting names), and these sections: **HUD,
Visuals, Cosmetics, Quality of Life, Performance**, plus **Wardrobe, Themes, Profiles, Keybinds and
Settings**. Each module row shows its name and description on the left. On the right are a settings
chevron, a key-bind badge (with conflict warnings) and a switch. The settings open inline and include
switches, sliders, dropdowns, an HSV colour picker with alpha and theme-sync, text fields and buttons.
Every module has a **Reset module** button, and Settings has **Reset everything**. The selected
section is marked by a highlight that slides between sidebar items. Each page header has an icon
tile, and enabled modules get an accent strip and tint. Tooltips appear after a short pause above
the cursor, so they never cover the control you are pointing at.

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
* Keystrokes: W A S D, LMB and RMB with a mouse-movement pad between them (as in the Fern
  reference), a labelled SPACE bar, optional CPS
* CPS
* Armour and held-item durability
* Inventory preview
* Item counter
* Compass: shows only your own heading
* **Staff List**: players whose tab-list rank is a staff rank (Owner, Admin, Mod, Helper, …; the
  words are editable). It only reads what the tab list already shows, so hidden or vanished staff
  are never listed
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
**Custom Sky** (topographic contours, gradient, **Nebula** in violet, emerald, azure, crimson or
silver, **Aurora**, or glowing **Lava cells**, with optional **shooting stars**), Time Changer,
Weather Changer, Custom Rain, **Rain Splashes**, Custom Fog (colour, and it can only make fog
thicker), World Tint, Block Outline, **Water Colour** (colour only; underwater visibility is
unchanged), **No Block Rotation**, Animations (visual swing length and style, equip animation, hand
position and scale), Item Glow, **Hand Glow** (glowing outline around your held item; R7's "Hand aura"), Hand Shader,
**Smooth Camera** (eases mouse movement; every bit of movement is still applied), **Attack
Indicator** (ring or bar), **Low Fire & Shield**. The **inspect** key (Y) works on any held item.

### Wardrobe
Aqua > **Wardrobe** has tabs for **3D Skins, Wings, Hats, Capes, Knives, Emotes and Kill FX**. A large
preview of your player spins on the left; every card shows a live 3D preview of that item, and
hovering a card shows it in the large preview. Click a card to wear it; **Show in game** turns the
category on or off.

* **3D Skins:** nine original 3D characters (Droplet, Robo, Penguin, Sea Dragon, Ghost, Knight,
  Shark, Snowman, Blob). The player model poses them, so walking, swinging and emotes all animate.
* **Knives & swords:** karambit (held reverse-grip), butterfly knife, M9-style bayonet and stiletto,
  in Emerald, Crimson, Silver, Urban, Night, Aqua Fade, Gold or Theme finishes, plus four fantasy
  swords (Frostbite, Sunforge, Voidrend, Thornheart). They are drawn instead of your sword
  (optionally axes). Press **Y** to inspect: the karambit spins, the butterfly knife flips open.
  Only the look changes: the item, damage and reach are vanilla.
* **Wings:** angel, devil, butterfly, crystal and feather, with custom colours, size and flap.
* **Hats:** glow hat, top hat, crown, witch hat, horns and cat ears.
* **Capes:** Aqua, Theme, Topographic, Galaxy and Flame, flowing with your movement.
* **Kill FX:** lightning, soul, burst, blood, hearts or firework where something you hit within
  the last six seconds dies, with an optional sound only you hear.

### Cosmetics (visible only to you; nothing is networked)
Wings, Hats, Cape, Halo, Orbit, 3D Skins, **Glass** (your player as clear glass with glowing
edges), Knife Skins, **Pet** (a floating companion), Kill Effects, **Hit FX**, **Damage FX**,
**Swing Trails**, **Player Trail** (ice footprints, afterimages or sparkles), Jump Circles (classic,
rune or electric) and landing shockwave, Jump Effects, Dash Trail, Projectile Trails (past path
only, no prediction), Fireflies, **Ambience** (cherry petals, autumn leaves, snow or Halloween bats),
Floating Cubes, Wind, **Sky Lyrics** (glowing 3D text from lyrics or your own text; it follows your
view), Totem Pop Effect, Hit Particles, Crystal Pulse (depth tested), World Join Effect (typewriter
title), Emotes (wheel plus wardrobe preview).

* **Hit FX:** when you hit something: a hit marker on the crosshair, a slash, burst or star at the
  hit, a ring at the target's feet that fades a second later, a floating **HIT** or **CRIT** marker
  and a hit sound of your choice (click, ding, chime, bell, pling, bonk, crystal, arrow). It shows
  no damage numbers or health, only that you hit and whether the game counted it as critical.
* **Damage FX:** the screen edges pulse red when you are hurt, and glow with a heartbeat while your
  health is low.

### Quality of life
Smooth Zoom, Freelook, Toggle Sprint (holds the vanilla sprint key, so there is no omni-sprint), Music Player (with Spotify),
Screenshot notifications with a thumbnail, Chat Style (frosted glass or a colour background, and timestamps), Death Waypoints
(your own deaths only), Streamer Mode (hides coordinates and the server address on your own screen),
**Creamy Keys** (soft keyboard sounds while you type), **Media Hider** (covers your hotbar, and
optionally inventory screens, with an Aqua banner for videos).

In the menu, **star** a module to list it under **Starred**, **middle-click** a module to bind a key,
and turn on **Settings › Colour Sync** to make every module colour follow your theme.

* **Freelook**: hold **Left Alt** to look around your player in third person while you keep
  moving and facing the same way. Only the camera turns; your player's real direction, which the
  server and other players see, does not change. The camera still stops at walls. It can be set to
  toggle instead of hold, start from the front, or invert up and down. **Some servers, such as
  Hypixel, do not allow freelook mods**, so check the rules before using it.
* **Name Protect** replaces your username with an alias of your choice in everything the game draws:
  chat, tab list, nametags, scoreboard and the HUD.
* **Skin Protect** shows another skin as yours, on your screen only. Type a player's name and
  press **Apply** to load their skin from Mojang, or pick Steve or a default skin (no network).
* **Aqua Badge** puts a small Aqua mark before the names of Aqua users in the tab list and on
  nametags. Aqua clients say hello on the `aqua:hello` channel, and a server that knows the channel
  shares who uses Aqua. That works in singleplayer and on LAN worlds hosted by an Aqua user. Normal
  multiplayer servers do not pass the hello on and Aqua has no central server, so there the badge is
  only shown next to your own name. You can turn the hello off with *Let others see that I use Aqua*.

### Performance
Effect Budget, Background FPS Limit and Particle Limiter. Under Settings you can also set **Effects
quality** (Low/Medium/High), **Cosmetic intensity**, **Reduced motion**, the **Interface style**
(Glass or Solid) and **Soft shadows**.

### Title screen
The Aqua emblem and wordmark intro. Behind it are drifting coloured lights, or the Minecraft
panorama if you pick that under *Settings → Title background*. The **Singleplayer, Multiplayer,
Options, Aqua Settings and Quit** buttons sit on a glass card and open the real Minecraft screens. A
glass bar in the corner holds the language and accessibility shortcuts and a one-click switch to the
vanilla title screen. You can turn the Aqua title screen off in Settings.

---

## Music and lyrics
1. Put **Ogg Vorbis** (`.ogg`) files in `config/aqua/music/`.
   * The title and artist come from the file's tags, or from a file name like `Artist - Title.ogg`.
   * Optional synced lyrics: add `Artist - Title.lrc` next to the song, using the standard
     `[mm:ss.xx] line` format. `[offset:]` is supported.
   * Cover art: covers embedded in the song's tags are used automatically. You can also add
     `Artist - Title.png` (or `.jpg`) or `cover.png` next to the song. Songs without one get a cover
     looked up online (see below).
2. Open Aqua > **Music**, press **Rescan**, then click a song. The Music page is a full player:
   seek bar, previous/play/next, shuffle, repeat, volume, a lyrics preview and your library. You can
   also use Quality of Life > Music Player and the play/pause and next keys.

**Spotify.** Play music in the Spotify app and Aqua shows it on the music card, the island and the
sky lyrics, with cover art, the song's progress and synced lyrics. Play/pause, next, previous and
the seek bar on the Music page control Spotify. Aqua reads this from your computer's media controls,
so there is no Spotify login and nothing is sent to Spotify:

* **Windows 10/11**: the system media session (what Windows shows next to the volume flyout), read
  by a small PowerShell script that ships inside the jar. If PowerShell is blocked on your PC, Aqua
  falls back to Spotify's window title, which gives the song but no progress or controls.
* **macOS**: AppleScript, only while Spotify is open. macOS asks once whether Minecraft may control
  Spotify.
* **Linux**: MPRIS over D-Bus, using the `gdbus` command.

Turn it off with the **Spotify** switch on the Music page or *Show music from Spotify* in the Music
Player settings. Set *Apps* to *Any music app* to also show browsers and other players. While a
song plays in Aqua's own player, that song is shown instead.

**Automatic covers.** When a song (yours or Spotify's) has no cover, Aqua looks one up with
Apple's public iTunes Search by artist and title and caches it in `config/aqua/cache/covers/`.
Turn this off with *Find cover art online* in the Music Player settings.

**Automatic lyrics.** When a song has no `.lrc` file, Aqua looks up synced lyrics on
[LRCLIB](https://lrclib.net), a free public lyrics database, and saves them in
`config/aqua/lyrics/` so each song is looked up once. Only the song's **title, artist and length**
are sent. Turn this off with *Find lyrics online (LRCLIB)* in the Music Player settings.

**No lyrics at all.** Switch **Lyrics** off on the Music page (or in the Music Player settings) to
use the player on its own: the music card, island and sky text then show no lyrics. The music card
also has a **Compact** style.

Only `.ogg` files can be played. Other formats are listed as unsupported instead of failing silently.
No songs are bundled and no audio is streamed. Music plays through Minecraft's Jukebox/Note Blocks
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
| `lyrics/` | lyrics found online, cached per song |
| `cache/media/` | cover art of songs playing in Spotify, and the Windows media reader script |
| `cache/covers/` | covers embedded in your songs and covers found online |

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

## Troubleshooting

**The Aqua HUD is not showing**

1. Press **F1**: it hides the whole HUD, Aqua's included.
2. Open Aqua > **HUD** and check that the widgets you want are switched on. Use **Edit layout >
   Reset all** if they were moved off screen.
3. **Hide while F3 is open** (on by default) hides the HUD only while the F3 debug screen is
   open. Single debug options such as hitboxes (F3+B) or chunk borders (F3+G) do not hide it.
4. Another HUD mod, or a modded client with its own HUD, can replace Minecraft's HUD drawing so
   that Fabric's HUD layers never run. Aqua notices this and draws its HUD itself; `latest.log`
   then contains `Aqua is drawing its HUD directly`. If the HUD still does not appear, please share
   `logs/latest.log` and your list of mods.

## Privacy and network use
Aqua makes no network requests unless you use one of these features:

| Feature | Sends | To |
|---|---|---|
| Automatic lyrics | song title, artist and length (for your songs and Spotify's) | `lrclib.net` |
| Spotify cover art (macOS, Linux) | nothing (downloads the cover image Spotify links to) | Spotify's image server (`i.scdn.co`) |
| Automatic covers | song artist and title | Apple iTunes Search (`itunes.apple.com`, images from `mzstatic.com`) |
| Skin Protect (player name) | the player name you typed | Mojang (`api.mojang.com`, `sessionserver.mojang.com`, `textures.minecraft.net`) |
| Aqua Badge hello | the Aqua version | only the server you are connected to |

Your own account details are never sent by Aqua.

## Compatibility
See `TESTING.md` for exactly what was tested and the versions involved.

## Licence
Aqua Client is released under the MIT licence. The bundled Inter font is under the SIL OFL 1.1; see
`THIRD_PARTY_NOTICES.md`.
