package dev.aqua.client.module.visual;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.world.Fx;
import dev.aqua.client.render.world.FxLayers;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.setting.NumberSetting;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;

/** Custom outline for the block you are looking at (R3 "Block Outline"). Depth tested like vanilla. */
public final class BlockOutline extends Module {
    private final ColorSetting color = add(new ColorSetting("color", "Outline", 0xFF52E4F2, true, true));
    private final NumberSetting width = add(new NumberSetting("width", "Width", 2.0, 0.5, 5.0, 0.25).unit(" px"));
    private final BoolSetting fill = add(new BoolSetting("fill", "Fill faces", true));
    private final ColorSetting fillColor = add(new ColorSetting("fill_color", "Fill", 0x2652E4F2, true, true).visibleWhen(fill::on));
    private final BoolSetting glow = add(new BoolSetting("glow", "Glow", false));

    public BlockOutline() {
        super("block_outline", "Block Overlay", "Restyles the outline of the block you are looking at.", Category.VISUALS, false,
                "outline", "selection", "highlight block", "block outline");
    }

    public void draw(Fx fx, VertexConsumerProvider vcp, BlockPos pos, VoxelShape shape) {
        double grow = 0.002;
        float w = width.f() * 0.004f;
        VertexConsumer lines = vcp.getBuffer(glow.on() ? FxLayers.GLOW : FxLayers.COLOR);
        VertexConsumer faces = vcp.getBuffer(FxLayers.COLOR);
        for (Box b : shape.getBoundingBoxes()) {
            Box box = b.offset(pos).expand(grow);
            if (fill.on()) {
                fx.box(faces, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, fillColor.color());
            }
            int c = color.color();
            double[] xs = {box.minX, box.maxX};
            double[] ys = {box.minY, box.maxY};
            double[] zs = {box.minZ, box.maxZ};
            for (double y : ys) {
                for (double z : zs) {
                    fx.line(lines, box.minX, y, z, box.maxX, y, z, w, c);
                }
            }
            for (double x : xs) {
                for (double z : zs) {
                    fx.line(lines, x, box.minY, z, x, box.maxY, z, w, c);
                }
            }
            for (double x : xs) {
                for (double y : ys) {
                    fx.line(lines, x, y, box.minZ, x, y, box.maxZ, w, c);
                }
            }
        }
        Colors.alpha(0);
    }
}
