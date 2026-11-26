package dev.aqua.client;

import dev.aqua.client.hud.widgets.*;
import dev.aqua.client.module.Module;
import dev.aqua.client.module.cosmetic.*;
import dev.aqua.client.module.perf.EffectBudget;
import dev.aqua.client.module.perf.ParticleLimiter;
import dev.aqua.client.module.perf.UnfocusedFps;
import dev.aqua.client.module.qol.*;
import dev.aqua.client.module.visual.*;
import dev.aqua.client.music.MusicModule;

import java.util.ArrayList;
import java.util.List;

/** Every Aqua module, in menu order. Contains no gameplay automation of any kind. */
final class Modules {
    private Modules() {}

    static List<Module> create() {
        List<Module> m = new ArrayList<>();
        // HUD (Fern-style widgets)
        m.add(new WatermarkWidget());
        m.add(new AccountWidget());
        m.add(new PerformanceWidget());
        m.add(new PositionWidget());
        m.add(new NetworkWidget());
        m.add(new ClockWidget());
        m.add(new KeybindsWidget());
        m.add(new dev.aqua.client.hud.widgets.StaffListWidget());
        m.add(new EffectsWidget());
        m.add(new ModuleListWidget());
        m.add(new KeystrokesWidget());
        m.add(new CpsWidget());
        m.add(new ArmorWidget());
        m.add(new InventoryWidget());
        m.add(new ItemCounterWidget());
        m.add(new CompassWidget());
        m.add(new IslandWidget());
        m.add(new MusicWidget());
        m.add(new ScoreboardWidget());
        m.add(new CustomCrosshair());
        // Visuals
        m.add(new CustomSky());
        m.add(new TimeChanger());
        m.add(new WeatherChanger());
        m.add(new CustomRain());
        m.add(new CustomFog());
        m.add(new WorldTint());
        m.add(new BlockOutline());
        m.add(new Animations());
        m.add(new ItemGlow());
        m.add(new HandShader());
        // Cosmetics
        m.add(new CharacterSkin());
        m.add(new Wings());
        m.add(new Hats());
        m.add(new Cape());
        m.add(new Halo());
        m.add(new Orbit());
        m.add(new Knives());
        m.add(new KillEffects());
        m.add(new dev.aqua.client.module.cosmetic.HitFx());
        m.add(new dev.aqua.client.module.cosmetic.DamageFx());
        m.add(new dev.aqua.client.module.cosmetic.SwingTrails());
        m.add(new dev.aqua.client.module.cosmetic.PlayerTrail());
        m.add(new dev.aqua.client.module.cosmetic.Ambience());
        m.add(new dev.aqua.client.module.cosmetic.Pet());
        m.add(new dev.aqua.client.module.cosmetic.GlassSkin());
        m.add(new JumpCircles());
        m.add(new JumpEffects());
        m.add(new DashTrail());
        m.add(new ProjectileTrails());
        m.add(new Fireflies());
        m.add(new FloatingCubes());
        m.add(new Wind());
        m.add(new SkyLyrics());
        m.add(new PopEffect());
        m.add(new HitParticles());
        m.add(new CrystalPulse());
        m.add(new WorldJoinEffect());
        m.add(new EmotesModule());
        // Quality of life
        m.add(new Zoom());
        m.add(new dev.aqua.client.module.qol.Freelook());
        m.add(new dev.aqua.client.module.qol.MediaHider());
        m.add(new dev.aqua.client.module.qol.CreamyKeys());
        m.add(new dev.aqua.client.module.visual.SmoothCamera());
        m.add(new dev.aqua.client.module.visual.RainSplashes());
        m.add(new dev.aqua.client.module.visual.LowOverlays());
        m.add(new dev.aqua.client.module.visual.AttackIndicator());
        m.add(new dev.aqua.client.module.visual.HandAura());
        m.add(new dev.aqua.client.module.visual.NoBlockRotation());
        m.add(new dev.aqua.client.module.visual.WaterColour());
        m.add(new ToggleSprint());
        m.add(new dev.aqua.client.module.qol.ChatMacro());
        // From the reference video (panel menu update)
        m.add(new dev.aqua.client.module.visual.LowHealthWarning());
        m.add(new dev.aqua.client.module.qol.Waypoints());
        m.add(new dev.aqua.client.module.qol.Nametags());
        m.add(new dev.aqua.client.module.qol.AutoReconnect());
        m.add(new dev.aqua.client.module.qol.CoordSnapper());
        m.add(new dev.aqua.client.module.qol.SlowSwing());
        m.add(new dev.aqua.client.module.qol.WeatherNotifier());
        m.add(new dev.aqua.client.module.qol.ToggleSneak());
        m.add(new dev.aqua.client.module.qol.AutoJump());
        m.add(new dev.aqua.client.module.qol.DiscordPresence());
        m.add(new dev.aqua.client.hud.HudModule());
        m.add(new dev.aqua.client.module.perf.PerformanceMode());
        m.add(new dev.aqua.client.module.visual.ColorGrading());
        m.add(new dev.aqua.client.module.visual.MotionBlur());
        m.add(new dev.aqua.client.module.visual.CustomFov());
        m.add(new dev.aqua.client.module.visual.CustomGlint());
        m.add(new dev.aqua.client.module.visual.HandPose());
        m.add(new dev.aqua.client.module.visual.ThreeDHand());
        m.add(new dev.aqua.client.module.visual.TargetMarker());
        m.add(new dev.aqua.client.module.cosmetic.FloatingRelics());
        m.add(new dev.aqua.client.module.cosmetic.AuraParticles());
        m.add(new dev.aqua.client.module.cosmetic.PharaohHeaddress());
        m.add(new dev.aqua.client.module.cosmetic.TotemSpirit());
        m.add(new MusicModule());
        m.add(new ScreenshotHelper());
        m.add(new ChatStyle());
        m.add(new DeathWaypoints());
        m.add(new NameProtect());
        m.add(new SkinProtect());
        m.add(new AquaBadge());
        m.add(new StreamerMode());
        // Performance
        m.add(new EffectBudget());
        m.add(new UnfocusedFps());
        m.add(new ParticleLimiter());
        return m;
    }
}
