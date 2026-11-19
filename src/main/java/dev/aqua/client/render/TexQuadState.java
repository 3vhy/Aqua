package dev.aqua.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

/** Textured quad with float GUI coordinates (vanilla's equivalent only accepts integers). */
public record TexQuadState(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f pose,
                           float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1,
                           int color, @Nullable ScreenRect scissorArea, @Nullable ScreenRect bounds)
        implements SimpleGuiElementRenderState {

    public static TexQuadState of(RenderPipeline pipeline, TextureSetup setup, Matrix3x2f pose, float x0, float y0, float x1, float y1,
                                  float u0, float v0, float u1, float v1, int color, @Nullable ScreenRect scissor) {
        ScreenRect r = new ScreenRect((int) Math.floor(x0), (int) Math.floor(y0),
                Math.max(1, (int) Math.ceil(x1 - x0) + 1), Math.max(1, (int) Math.ceil(y1 - y0) + 1)).transformEachVertex(pose);
        return new TexQuadState(pipeline, setup, pose, x0, y0, x1, y1, u0, v0, u1, v1, color, scissor,
                scissor != null ? scissor.intersection(r) : r);
    }

    @Override
    public void setupVertices(VertexConsumer v) {
        v.vertex(pose, x0, y0).texture(u0, v0).color(color);
        v.vertex(pose, x0, y1).texture(u0, v1).color(color);
        v.vertex(pose, x1, y1).texture(u1, v1).color(color);
        v.vertex(pose, x1, y0).texture(u1, v0).color(color);
    }
}
