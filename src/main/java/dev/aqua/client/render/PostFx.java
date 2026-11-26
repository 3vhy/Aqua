package dev.aqua.client.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import dev.aqua.client.Aqua;
import dev.aqua.client.module.visual.ColorGrading;
import dev.aqua.client.module.visual.MotionBlur;
import dev.aqua.client.util.Problems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.OptionalInt;

/**
 * Color Grading and Motion Blur: full-screen passes over the game image, made just before the
 * interface is drawn (so the HUD and menus are never graded or smeared).
 */
public final class PostFx {
    private PostFx() {}

    @Nullable private static GpuTexture temp;
    @Nullable private static GpuTexture history;
    @Nullable private static GpuTextureView tempView;
    @Nullable private static GpuTextureView historyView;
    @Nullable private static GpuBuffer gradeBuf;
    @Nullable private static GpuBuffer mixBuf;
    private static int w;
    private static int h;
    private static boolean historyFresh;
    private static boolean broken;
    private static long frames;

    /** Frames processed (used by the in-game tests). */
    public static long frames() {
        return frames;
    }

    public static void apply() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (broken || mc.world == null || Aqua.modules() == null) {
            return;
        }
        ColorGrading grade = Aqua.modules().get(ColorGrading.class);
        MotionBlur blur = Aqua.modules().get(MotionBlur.class);
        boolean g = grade != null && grade.isEnabled();
        boolean b = blur != null && blur.isEnabled();
        if (!g && !b) {
            historyFresh = true;
            return;
        }
        try {
            Framebuffer fb = mc.getFramebuffer();
            ensure(fb.textureWidth, fb.textureHeight);
            CommandEncoder enc = RenderSystem.getDevice().createCommandEncoder();
            GpuSampler s = RenderSystem.getSamplerCache().get(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, false);
            GpuTextureView main = fb.getColorAttachmentView();
            if (b) {
                float amount = blur.frameAmount();
                if (historyFresh) {
                    pass(enc, AquaPipelines.POST_COPY, main, null, historyView, s, null, null);
                    historyFresh = false;
                }
                pass(enc, AquaPipelines.POST_COPY, main, null, tempView, s, null, null);
                writeMix(enc, amount);
                pass(enc, AquaPipelines.POST_MIX, tempView, historyView, main, s, "AquaMix", mixBuf);
                pass(enc, AquaPipelines.POST_COPY, main, null, historyView, s, null, null);
            } else {
                historyFresh = true;
            }
            if (g) {
                pass(enc, AquaPipelines.POST_COPY, main, null, tempView, s, null, null);
                writeGrade(enc, grade.params());
                pass(enc, AquaPipelines.POST_GRADE, tempView, null, main, s, "AquaGrade", gradeBuf);
            }
            frames++;
        } catch (Throwable e) {
            broken = true;
            Problems.report("Color Grading / Motion Blur are unavailable on this system", e);
        }
    }

    private static void ensure(int fw, int fh) {
        if (temp != null && w == fw && h == fh) {
            return;
        }
        close();
        GpuDevice d = RenderSystem.getDevice();
        int usage = GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST;
        temp = d.createTexture(() -> "Aqua post temp", usage, TextureFormat.RGBA8, fw, fh, 1, 1);
        history = d.createTexture(() -> "Aqua post history", usage, TextureFormat.RGBA8, fw, fh, 1, 1);
        tempView = d.createTextureView(temp);
        historyView = d.createTextureView(history);
        if (gradeBuf == null) {
            gradeBuf = d.createBuffer(() -> "Aqua grade UBO", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 32);
            mixBuf = d.createBuffer(() -> "Aqua mix UBO", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 16);
        }
        w = fw;
        h = fh;
        historyFresh = true;
    }

    private static void close() {
        if (tempView != null) {
            tempView.close();
            historyView.close();
            temp.close();
            history.close();
        }
        tempView = historyView = null;
        temp = history = null;
    }

    private static void writeGrade(CommandEncoder enc, float[] p) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer bb = Std140Builder.onStack(stack, 32).putVec4(p[0], p[1], p[2], p[3]).putVec4(p[4], p[5], p[6], p[7]).get();
            enc.writeToBuffer(gradeBuf.slice(), bb);
        }
    }

    private static void writeMix(CommandEncoder enc, float amount) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer bb = Std140Builder.onStack(stack, 16).putVec4(amount, 0, 0, 0).get();
            enc.writeToBuffer(mixBuf.slice(), bb);
        }
    }

    private static void pass(CommandEncoder enc, RenderPipeline pipeline, GpuTextureView in, @Nullable GpuTextureView prev, GpuTextureView out,
                             GpuSampler sampler, @Nullable String uniform, @Nullable GpuBuffer buf) {
        try (RenderPass p = enc.createRenderPass(() -> "Aqua post", out, OptionalInt.empty())) {
            p.setPipeline(pipeline);
            p.bindTexture("InSampler", in, sampler);
            if (prev != null) {
                p.bindTexture("PrevSampler", prev, sampler);
            }
            if (uniform != null) {
                p.setUniform(uniform, buf);
            }
            p.draw(0, 3);
        }
    }
}
