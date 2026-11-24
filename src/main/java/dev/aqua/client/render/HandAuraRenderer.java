package dev.aqua.client.render;

import dev.aqua.client.cosmetic.Mesh;
import dev.aqua.client.render.world.FxLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.SpriteContents;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import org.joml.Vector3fc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hand Aura for vanilla items. Flat items (swords, tools, most items) get a glowing silhouette made
 * from the opaque pixels of their texture, slightly widened and drawn just behind the item, so only a
 * rim of light shows around it. 3D items (blocks, shields) get a soft glow shell of their faces.
 */
public final class HandAuraRenderer {
    private static int color;
    private static final Map<Identifier, boolean[]> MASKS = new HashMap<>();

    private HandAuraRenderer() {}

    /** Set around the first-person item render (0 = off). */
    public static void begin(int argb) {
        color = argb;
    }

    public static void end() {
        color = 0;
    }

    private static boolean[] mask(SpriteContents c) {
        return MASKS.computeIfAbsent(c.getId(), id -> {
            int w = c.getWidth(), h = c.getHeight();
            boolean[] m = new boolean[w * h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    m[y * w + x] = !c.isPixelTransparent(0, x, y);
                }
            }
            return m;
        });
    }

    public static void afterItem(OrderedRenderCommandQueue queue, MatrixStack matrices, List<BakedQuad> quads) {
        int c = color;
        if (c == 0 || quads == null || quads.isEmpty()) {
            return;
        }
        BakedQuad front = null;
        for (BakedQuad q : quads) {
            if ((q.face() == Direction.SOUTH || q.face() == Direction.NORTH) && spansSquare(q)) {
                front = q;
                break;
            }
        }
        if (front != null) {
            silhouette(queue, matrices, front, c);
        } else {
            shell(queue, matrices, List.copyOf(quads), c);
        }
    }

    private static boolean spansSquare(BakedQuad q) {
        float minX = 9, maxX = -9, minY = 9, maxY = -9;
        for (int i = 0; i < 4; i++) {
            Vector3fc p = q.getPosition(i);
            minX = Math.min(minX, p.x());
            maxX = Math.max(maxX, p.x());
            minY = Math.min(minY, p.y());
            maxY = Math.max(maxY, p.y());
        }
        return maxX - minX > 0.95f && maxY - minY > 0.95f;
    }

    /** Rings of transparent pixels around the item's opaque ones: distance 1 (inner) and 2 (outer). */
    private static final Map<Identifier, int[]> RINGS = new HashMap<>();

    private static int[] rings(SpriteContents sc) {
        return RINGS.computeIfAbsent(sc.getId(), id -> {
            boolean[] m = mask(sc);
            int w = sc.getWidth(), h = sc.getHeight();
            int[] r = new int[w * h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (m[y * w + x]) {
                        continue;
                    }
                    int best = 0;
                    for (int dy = -2; dy <= 2 && best != 1; dy++) {
                        for (int dx = -2; dx <= 2; dx++) {
                            int nx = x + dx, ny = y + dy;
                            if (nx < 0 || ny < 0 || nx >= w || ny >= h || !m[ny * w + nx]) {
                                continue;
                            }
                            int d = Math.max(Math.abs(dx), Math.abs(dy));
                            if (best == 0 || d < best) {
                                best = d;
                            }
                        }
                    }
                    r[y * w + x] = best;
                }
            }
            return r;
        });
    }

    /** A glowing ring around the item's silhouette, in the item's own plane (it never covers the item). */
    private static void silhouette(OrderedRenderCommandQueue queue, MatrixStack matrices, BakedQuad front, int c) {
        SpriteContents sc = front.sprite().getContents();
        int[] ring = rings(sc);
        int w = sc.getWidth(), h = sc.getHeight();
        float z = 8f / 16f;
        int inner = Colors.fade(c, 0.75f), outer = Colors.fade(c, 0.28f);
        queue.submitCustom(matrices, FxLayers.GLOW, (entry, vc) -> {
            Mesh mesh = new Mesh(entry, vc, 1f);
            float px = 1f / w, py = 1f / h;
            // Pixels at the very border of the texture also get a ring outside it.
            for (int y = -2; y < h + 2; y++) {
                for (int x = -2; x < w + 2; x++) {
                    int d;
                    if (x >= 0 && y >= 0 && x < w && y < h) {
                        d = ring[y * w + x];
                    } else {
                        d = outside(sc, x, y, w, h);
                    }
                    if (d == 0) {
                        continue;
                    }
                    int col = d == 1 ? inner : outer;
                    float x0 = x * px, x1 = (x + 1) * px;
                    float y1 = 1f - y * py, y0 = 1f - (y + 1) * py;
                    mesh.quad(x0, y0, z, x1, y0, z, x1, y1, z, x0, y1, z, col, 1f);
                    mesh.quad(x0, y0, z, x0, y1, z, x1, y1, z, x1, y0, z, col, 1f);
                }
            }
        });
    }

    private static int outside(SpriteContents sc, int x, int y, int w, int h) {
        boolean[] m = mask(sc);
        int best = 0;
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                int nx = x + dx, ny = y + dy;
                if (nx < 0 || ny < 0 || nx >= w || ny >= h || !m[ny * w + nx]) {
                    continue;
                }
                int d = Math.max(Math.abs(dx), Math.abs(dy));
                if (best == 0 || d < best) {
                    best = d;
                }
            }
        }
        return best;
    }

    /** Soft glow shell for 3D item models. */
    private static void shell(OrderedRenderCommandQueue queue, MatrixStack matrices, List<BakedQuad> quads, int c) {
        for (float[] pass : new float[][]{{1.05f, 0.35f}, {1.1f, 0.15f}}) {
            matrices.push();
            matrices.translate(0.5f, 0.5f, 0.5f);
            matrices.scale(pass[0], pass[0], pass[0]);
            matrices.translate(-0.5f, -0.5f, -0.5f);
            int col = Colors.fade(c, pass[1]);
            queue.submitCustom(matrices, FxLayers.GLOW, (entry, vc) -> {
                Mesh mesh = new Mesh(entry, vc, 1f).overrideColor(col);
                for (BakedQuad q : quads) {
                    Vector3fc a = q.position0(), b = q.position1(), d = q.position2(), e = q.position3();
                    mesh.quad(a.x(), a.y(), a.z(), b.x(), b.y(), b.z(), d.x(), d.y(), d.z(), e.x(), e.y(), e.z(), col, 1f);
                }
            });
            matrices.pop();
        }
    }
}
