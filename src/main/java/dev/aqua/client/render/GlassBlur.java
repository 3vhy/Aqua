package dev.aqua.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.gl.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import dev.aqua.client.util.Problems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.texture.TextureSetup;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalInt;

/**
 * The blur behind Aqua's glass surfaces. Once per frame, only when something glassy is on screen,
 * the frame is shrunk to a quarter size and blurred a few times; glass panels then show that
 * blurred picture through their rounded shape.
 *
 * <p>There are two captures. {@link Slot#WORLD} is the plain game view, taken when the GUI starts
 * drawing (used by HUD widgets and toasts). {@link Slot#BACKDROP} is taken where a screen asks for
 * it with {@link Glass#backdrop}: everything the screen drew before that point (its background,
 * the HUD under a menu) ends up behind its glass.
 */
public final class GlassBlur {
    public enum Slot { WORLD, BACKDROP }

    private static final int DOWNSCALE = 4;

    private static final class Target {
        @Nullable GpuTexture a;
        @Nullable GpuTexture b;
        @Nullable GpuTextureView va;
        @Nullable GpuTextureView vb;
        @Nullable TextureSetup setup;
        int w;
        int h;
        boolean drawn;
        boolean everDrawn;

        void close() {
            if (va != null) {
                va.close();
            }
            if (vb != null) {
                vb.close();
            }
            if (a != null) {
                a.close();
            }
            if (b != null) {
                b.close();
            }
            va = vb = null;
            a = b = null;
            setup = null;
            everDrawn = false;
            w = h = 0;
        }
    }

    private static final Target[] TARGETS = {new Target(), new Target()};
    private static final boolean[] REQUESTED = new boolean[2];
    private static boolean backdropThisFrame;
    private static boolean replaceVanillaBlur;
    private static int passes = 3;
    private static boolean broken;
    private static long captures;
    private static long frame;

    private static boolean lightMode() {
        dev.aqua.client.module.perf.PerformanceMode pm = dev.aqua.client.Aqua.modules() != null
                ? dev.aqua.client.Aqua.modules().get(dev.aqua.client.module.perf.PerformanceMode.class) : null;
        return pm != null && pm.lightBlur();
    }

    private GlassBlur() {}

    // ------------------------------------------------------------------ while the GUI is built

    /** The capture a glass element created right now should use. */
    public static Slot currentSlot() {
        return backdropThisFrame ? Slot.BACKDROP : Slot.WORLD;
    }

    public static void request(Slot slot) {
        REQUESTED[slot.ordinal()] = true;
    }

    /** A screen drew its background; glass drawn from now on blurs what is behind it. */
    public static void markBackdrop(boolean replaceVanilla) {
        backdropThisFrame = true;
        replaceVanillaBlur = replaceVanilla;
    }

    public static boolean backdropMarked() {
        return backdropThisFrame;
    }

    /** Blur strength: number of blur passes (1 = light frost, 4 = heavy). */
    public static void setPasses(int n) {
        passes = Math.max(1, Math.min(5, n));
    }

    public static boolean broken() {
        return broken;
    }

    /** How many blur captures were made (used by the in-game tests). */
    public static long captures() {
        return captures;
    }

    // ------------------------------------------------------------------ while the GUI is drawn

    /** Called as the GUI starts drawing: sizes the textures and blurs the plain game view. */
    public static void onGuiRenderStart() {
        if (broken) {
            return;
        }
        try {
            Framebuffer fb = MinecraftClient.getInstance().getFramebuffer();
            for (Slot s : Slot.values()) {
                Target t = TARGETS[s.ordinal()];
                t.drawn = false;
                if (REQUESTED[s.ordinal()]) {
                    ensure(t, fb);
                }
            }
            frame++;
            if (REQUESTED[Slot.WORLD.ordinal()]) {
                // Performance mode reuses last frame's blur every other frame.
                Target wt = TARGETS[Slot.WORLD.ordinal()];
                if (!(lightMode() && (frame & 1) == 1 && wt.everDrawn)) {
                    blur(wt, fb);
                }
            }
        } catch (Throwable e) {
            fail(e);
        }
    }

    /**
     * Called where vanilla would blur the screen behind a menu. Returns true if the vanilla blur
     * should be skipped (the screen asked for glass instead of a fully blurred background).
     */
    public static boolean onBlurLayer() {
        if (!backdropThisFrame) {
            return false;
        }
        if (!broken && REQUESTED[Slot.BACKDROP.ordinal()]) {
            try {
                blur(TARGETS[Slot.BACKDROP.ordinal()], MinecraftClient.getInstance().getFramebuffer());
            } catch (Throwable e) {
                fail(e);
            }
        }
        return replaceVanillaBlur;
    }

    public static void onGuiRenderEnd() {
        REQUESTED[0] = false;
        REQUESTED[1] = false;
        backdropThisFrame = false;
        replaceVanillaBlur = false;
    }

    /** True when glass elements of this slot can show a blur this frame. */
    public static boolean ready(Slot slot) {
        Target t = TARGETS[slot.ordinal()];
        return !broken && t.setup != null;
    }

    @Nullable
    public static TextureSetup setup(Slot slot) {
        return TARGETS[slot.ordinal()].setup;
    }

    private static void ensure(Target t, Framebuffer fb) {
        int w = Math.max(1, fb.textureWidth / DOWNSCALE);
        int h = Math.max(1, fb.textureHeight / DOWNSCALE);
        if (t.a != null && t.w == w && t.h == h) {
            return;
        }
        t.close();
        GpuDevice device = RenderSystem.getDevice();
        int usage = GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST;
        t.a = device.createTexture(() -> "Aqua glass A", usage, TextureFormat.RGBA8, w, h, 1, 1);
        t.b = device.createTexture(() -> "Aqua glass B", usage, TextureFormat.RGBA8, w, h, 1, 1);
        t.va = device.createTextureView(t.a);
        t.vb = device.createTextureView(t.b);
        t.w = w;
        t.h = h;
        t.setup = TextureSetup.of(t.va, sampler());
        // Start dark rather than with undefined memory.
        CommandEncoder enc = device.createCommandEncoder();
        enc.clearColorTexture(t.a, 0xFF101418);
        enc.clearColorTexture(t.b, 0xFF101418);
    }

    private static GpuSampler sampler() {
        return RenderSystem.getSamplerCache().get(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, false);
    }

    private static void blur(Target t, Framebuffer fb) {
        if (t.va == null || t.vb == null || t.drawn) {
            return;
        }
        CommandEncoder enc = RenderSystem.getDevice().createCommandEncoder();
        GpuSampler s = sampler();
        pass(enc, AquaPipelines.GLASS_DOWNSAMPLE, fb.getColorAttachmentView(), t.va, s);
        int n = lightMode() ? Math.min(2, passes) : passes;
        for (int i = 0; i < n; i++) {
            pass(enc, AquaPipelines.GLASS_BLUR_H, t.va, t.vb, s);
            pass(enc, AquaPipelines.GLASS_BLUR_V, t.vb, t.va, s);
        }
        t.drawn = true;
        t.everDrawn = true;
        captures++;
    }

    private static void pass(CommandEncoder enc, RenderPipeline pipeline, GpuTextureView in, GpuTextureView out, GpuSampler sampler) {
        try (RenderPass p = enc.createRenderPass(() -> "Aqua glass blur", out, OptionalInt.empty())) {
            p.setPipeline(pipeline);
            p.bindTexture("InSampler", in, sampler);
            p.draw(0, 3);
        }
    }

    private static void fail(Throwable e) {
        // Textures stay alive: elements already prepared for this frame may still sample them.
        broken = true;
        Problems.report("Glass blur is unavailable on this system; using solid panels", e);
    }
}
