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

    private final EnumSetting<Source> source = add(new EnumSetting<>("source", "Source", Source.LYRICS));
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

    private Vec3d placeAhead() {
        Vec3d look = mc.player.getRotationVec(1f);
        Vec3d flat = new Vec3d(look.x, 0, look.z);
        if (flat.lengthSquared() < 1.0e-4) {
            flat = new Vec3d(0, 0, 1);
        }
        flat = flat.normalize();
        return mc.player.getEyePos().add(flat.multiply(distance.d())).add(0, distance.d() * 0.28, 0);
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
        if (isCustom() && anchor.squaredDistanceTo(mc.player.getEyePos()) > Math.pow(distance.d() * 2.2, 2)) {
            anchor = placeAhead();
        }
        float a = fade.get();
        float bob = (float) Math.sin(System.currentTimeMillis() / 900.0) * 0.25f;
        float scale = 0.85f + 0.15f * Anim.easeOutBack(a);
        WorldText.draw(fx, vcp, shown, anchor.x, anchor.y + bob, anchor.z, size.f() * scale, AquaFont.Weight.BOLD,
                Colors.fade(color.color(), a), glow.color(), a);
    }
}
