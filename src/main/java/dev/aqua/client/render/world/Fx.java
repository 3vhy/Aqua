package dev.aqua.client.render.world;

import dev.aqua.client.render.Colors;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Per-frame drawing context for world effects (positions are absolute world coordinates). */
public final class Fx {
    public final MatrixStack matrices;
    public final Vec3d cam;
    public final Camera camera;
    public final float tickDelta;
    public final Vector3f right;
    public final Vector3f up;
    public VertexConsumer glow;
    public VertexConsumer color;

    public Fx(MatrixStack matrices, Camera camera, float tickDelta) {
        this.matrices = matrices;
        this.camera = camera;
        this.cam = camera.getCameraPos();
        this.tickDelta = tickDelta;
        Quaternionf rot = camera.getRotation();
        this.right = new Vector3f(1, 0, 0).rotate(rot);
        this.up = new Vector3f(0, 1, 0).rotate(rot);
    }

    private Matrix4f m() {
        return matrices.peek().getPositionMatrix();
    }

    /** Camera-facing quad (additive when using the glow consumer). */
    public void billboard(VertexConsumer vc, double x, double y, double z, float size, int argb) {
        float px = (float) (x - cam.x);
        float py = (float) (y - cam.y);
        float pz = (float) (z - cam.z);
        float rx = right.x * size, ry = right.y * size, rz = right.z * size;
        float ux = up.x * size, uy = up.y * size, uz = up.z * size;
        Matrix4f m = m();
        vc.vertex(m, px - rx - ux, py - ry - uy, pz - rz - uz).color(argb);
        vc.vertex(m, px - rx + ux, py - ry + uy, pz - rz + uz).color(argb);
        vc.vertex(m, px + rx + ux, py + ry + uy, pz + rz + uz).color(argb);
        vc.vertex(m, px + rx - ux, py + ry - uy, pz + rz - uz).color(argb);
    }

    public VertexConsumer sparks;

    /** Camera-facing textured quad (uses the round particle texture). */
    public void billboardTex(VertexConsumer vc, double x, double y, double z, float size, int argb) {
        float px = (float) (x - cam.x);
        float py = (float) (y - cam.y);
        float pz = (float) (z - cam.z);
        float rx = right.x * size, ry = right.y * size, rz = right.z * size;
        float ux = up.x * size, uy = up.y * size, uz = up.z * size;
        Matrix4f m = m();
        vc.vertex(m, px - rx - ux, py - ry - uy, pz - rz - uz).texture(0, 1).color(argb);
        vc.vertex(m, px - rx + ux, py - ry + uy, pz - rz + uz).texture(0, 0).color(argb);
        vc.vertex(m, px + rx + ux, py + ry + uy, pz + rz + uz).texture(1, 0).color(argb);
        vc.vertex(m, px + rx - ux, py + ry - uy, pz + rz - uz).texture(1, 1).color(argb);
    }

    /** Soft round glow (radial texture, additive). */
    public void spark(double x, double y, double z, float size, int argb) {
        if (sparks != null) {
            billboardTex(sparks, x, y, z, size * 2.2f, argb);
        } else {
            billboard(glow, x, y, z, size, argb);
        }
    }

    /** Flat horizontal ring on the XZ plane. */
    public void ring(VertexConsumer vc, double cx, double cy, double cz, float radius, float width, int argb, int segments) {
        Matrix4f m = m();
        float y = (float) (cy - cam.y);
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2 * i / segments;
            double a1 = Math.PI * 2 * (i + 1) / segments;
            float ox0 = (float) (cx - cam.x + Math.cos(a0) * radius), oz0 = (float) (cz - cam.z + Math.sin(a0) * radius);
            float ox1 = (float) (cx - cam.x + Math.cos(a1) * radius), oz1 = (float) (cz - cam.z + Math.sin(a1) * radius);
            float ix0 = (float) (cx - cam.x + Math.cos(a0) * (radius - width)), iz0 = (float) (cz - cam.z + Math.sin(a0) * (radius - width));
            float ix1 = (float) (cx - cam.x + Math.cos(a1) * (radius - width)), iz1 = (float) (cz - cam.z + Math.sin(a1) * (radius - width));
            vc.vertex(m, ix0, y, iz0).color(argb);
            vc.vertex(m, ox0, y, oz0).color(argb);
            vc.vertex(m, ox1, y, oz1).color(argb);
            vc.vertex(m, ix1, y, iz1).color(argb);
        }
    }

    /** Ribbon segment between two points, widened perpendicular to the view direction. */
    public void ribbon(VertexConsumer vc, Vec3d a, Vec3d b, float wa, float wb, int ca, int cb) {
        Vector3f pa = new Vector3f((float) (a.x - cam.x), (float) (a.y - cam.y), (float) (a.z - cam.z));
        Vector3f pb = new Vector3f((float) (b.x - cam.x), (float) (b.y - cam.y), (float) (b.z - cam.z));
        Vector3f dir = new Vector3f(pb).sub(pa);
        if (dir.lengthSquared() < 1.0e-8f) {
            return;
        }
        Vector3f mid = new Vector3f(pa).add(pb).mul(0.5f);
        Vector3f side = dir.cross(mid, new Vector3f());
        if (side.lengthSquared() < 1.0e-8f) {
            side.set(right);
        }
        side.normalize();
        Matrix4f m = m();
        vc.vertex(m, pa.x - side.x * wa, pa.y - side.y * wa, pa.z - side.z * wa).color(ca);
        vc.vertex(m, pa.x + side.x * wa, pa.y + side.y * wa, pa.z + side.z * wa).color(ca);
        vc.vertex(m, pb.x + side.x * wb, pb.y + side.y * wb, pb.z + side.z * wb).color(cb);
        vc.vertex(m, pb.x - side.x * wb, pb.y - side.y * wb, pb.z - side.z * wb).color(cb);
    }

    /** Axis-aligned box faces (translucent). */
    public void box(VertexConsumer vc, double x0, double y0, double z0, double x1, double y1, double z1, int argb) {
        Matrix4f m = m();
        float a = (float) (x0 - cam.x), b = (float) (y0 - cam.y), c = (float) (z0 - cam.z);
        float d = (float) (x1 - cam.x), e = (float) (y1 - cam.y), f = (float) (z1 - cam.z);
        quad(vc, m, a, b, c, d, b, c, d, b, f, a, b, f, argb);
        quad(vc, m, a, e, c, a, e, f, d, e, f, d, e, c, argb);
        quad(vc, m, a, b, c, a, e, c, d, e, c, d, b, c, argb);
        quad(vc, m, a, b, f, d, b, f, d, e, f, a, e, f, argb);
        quad(vc, m, a, b, c, a, b, f, a, e, f, a, e, c, argb);
        quad(vc, m, d, b, c, d, e, c, d, e, f, d, b, f, argb);
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3, int argb) {
        vc.vertex(m, x0, y0, z0).color(argb);
        vc.vertex(m, x1, y1, z1).color(argb);
        vc.vertex(m, x2, y2, z2).color(argb);
        vc.vertex(m, x3, y3, z3).color(argb);
    }

    /** Unit cube centred on the current matrix origin (used with push/translate/rotate). */
    public void cubeLocal(VertexConsumer vc, float half, int argb) {
        Matrix4f m = m();
        float h = half;
        quad(vc, m, -h, -h, -h, h, -h, -h, h, -h, h, -h, -h, h, argb);
        quad(vc, m, -h, h, -h, -h, h, h, h, h, h, h, h, -h, argb);
        quad(vc, m, -h, -h, -h, -h, h, -h, h, h, -h, h, -h, -h, argb);
        quad(vc, m, -h, -h, h, h, -h, h, h, h, h, -h, h, h, argb);
        quad(vc, m, -h, -h, -h, -h, -h, h, -h, h, h, -h, h, -h, argb);
        quad(vc, m, h, -h, -h, h, h, -h, h, h, h, h, -h, h, argb);
    }

    /** Thick 3D line drawn as a camera-facing ribbon. */
    public void line(VertexConsumer vc, double x0, double y0, double z0, double x1, double y1, double z1, float width, int argb) {
        ribbon(vc, new Vec3d(x0, y0, z0), new Vec3d(x1, y1, z1), width, width, argb, argb);
    }

    public static Vec3d lerp(Vec3d a, Vec3d b, float t) {
        return new Vec3d(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, a.z + (b.z - a.z) * t);
    }
}
