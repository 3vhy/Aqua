package dev.aqua.client.module.cosmetic;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.music.MusicPlayer;
import dev.aqua.client.music.Track;
import dev.aqua.client.render.Anim;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.font.AquaFont;
import dev.aqua.client.render.world.Fx;
import dev.aqua.client.render.world.WorldEffect;
import dev.aqua.client.render.world.WorldText;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.setting.EnumSetting;
import dev.aqua.client.setting.NumberSetting;
import dev.aqua.client.setting.StringSetting;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.math.Vec3d;

/**
 * Glowing text in the sky (R1 0:03 "fern", R2 0:01/0:05, R1 "Lyrics Text"): shows the current lyric
 * line of the playing track, or your own text.
 */
public final class SkyLyrics extends Module implements WorldEffect {
    public enum Source implements EnumSetting.Displayable {
        LYRICS("Synced lyrics"), CUSTOM("Custom text");
        final String l;

        Source(String l) {
            this.l = l;
        }

        public String displayName() {
            return l;
        }
    }

    public enum Placement implements EnumSetting.Displayable {
        FOLLOW("Follow your view"), FIXED("Fixed in the world");
        final String l;

        Placement(String l) {
            this.l = l;
        }

        public String displayName() {
            return l;
        }
    }

    private final EnumSetting<Source> source = add(new EnumSetting<>("source", "Source", Source.LYRICS));
    private final EnumSetting<Placement> placement = add(new EnumSetting<>("placement", "Placement", Placement.FOLLOW)
            .describe("Follow your view keeps the line in front of you when you turn or move"));
    private final StringSetting text = add(new StringSetting("text", "Custom text", "aqua", 48).visibleWhen(() -> isCustom()));
    private final ColorSetting color = add(new ColorSetting("color", "Text colour", 0xFFFFFFFF, false, false));
    private final ColorSetting glow = add(new ColorSetting("glow", "Glow colour", 0xFF52E4F2, false, true));
    private final NumberSetting size = add(new NumberSetting("size", "Size", 3.0, 1.0, 8.0, 0.25).unit(" blocks"));
    private final NumberSetting distance = add(new NumberSetting("distance", "Distance", 28, 10, 80, 1).unit(" blocks"));

    private String shown = "";
    private Vec3d anchor;
    private final Anim fade = new Anim(0f, 450f);
    private String pendingText = "";

    public SkyLyrics() {
        super("sky_lyrics", "Sky Lyrics", "Glowing lyric lines or your own words floating in the sky.", Category.COSMETICS, false,
                "lyrics text", "sky text", "3d text", "song lyrics");
    }

    private boolean isCustom() {
        return source.is(Source.CUSTOM);
    }

    private String wanted() {
        if (isCustom()) {
            return text.get();
        }
        Track t = MusicPlayer.get().current();
        int idx = MusicPlayer.get().lyricIndex();
        if (t == null || idx < 0 || !MusicPlayer.get().isPlaying()) {
            return "";
        }
        return MusicPlayer.get().lyricsFor(t).get(idx).text();
    }

    /** Where the current line is drawn (null when nothing is shown); used by the in-game tests. */
    @org.jetbrains.annotations.Nullable
    public Vec3d anchor() {
        return shown.isEmpty() ? null : anchor;
    }

    /** A point ahead of the camera (horizontally), raised a little into the sky. */
    private Vec3d placeAhead() {
        net.minecraft.client.render.Camera cam = mc.gameRenderer.getCamera();
        double yaw = Math.toRadians(cam.getYaw());
        Vec3d flat = new Vec3d(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3d eye = cam.getCameraPos();
        return eye.add(flat.multiply(distance.d())).add(0, distance.d() * 0.28, 0);
    }

    private long lastFrame;

    @Override
    public void onTick() {
        // Prepare any new letters (curly quotes, accents, ...) outside of world rendering.
        if (mc.player != null) {
            String want = wanted();
            if (!want.isEmpty()) {
                AquaFont.worldAtlas(AquaFont.Weight.BOLD, 64).prepare(want);
            }
        }
    }

    @Override
    public void renderWorld(Fx fx, VertexConsumerProvider vcp) {
        if (mc.player == null) {
            return;
        }
        String want = wanted();
        if (!want.equals(pendingText)) {
            pendingText = want;
            fade.target(0f);
        }
        if (fade.get() < 0.02f && !pendingText.equals(shown)) {
            shown = pendingText;
            anchor = placeAhead();
            if (!shown.isEmpty()) {
                fade.target(1f);
            }
        } else if (!shown.isEmpty() && shown.equals(pendingText)) {
            fade.target(1f);
        }
        if (shown.isEmpty() || anchor == null) {
            return;
        }
        long now = System.currentTimeMillis();
        float dt = lastFrame == 0 ? 0f : Math.min(0.25f, (now - lastFrame) / 1000f);
        lastFrame = now;
        if (placement.is(Placement.FOLLOW)) {
            // Glide after the view so the line stays in sight when you turn or walk.
            Vec3d target = placeAhead();
            double k = 1 - Math.exp(-dt * 2.5);
            anchor = anchor.add(target.subtract(anchor).multiply(k));
        } else if (anchor.squaredDistanceTo(mc.player.getEyePos()) > Math.pow(distance.d() * 2.2, 2)) {
            anchor = placeAhead();
        }
        float a = fade.get();
        float bob = (float) Math.sin(System.currentTimeMillis() / 900.0) * 0.25f;
        float scale = 0.85f + 0.15f * Anim.easeOutBack(a);
        WorldText.draw(fx, vcp, shown, anchor.x, anchor.y + bob, anchor.z, size.f() * scale, AquaFont.Weight.BOLD,
                Colors.fade(color.color(), a), glow.color(), a);
    }
}
