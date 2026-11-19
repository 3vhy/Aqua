package dev.aqua.client.render.world;

import dev.aqua.client.AquaClient;
import dev.aqua.client.render.Colors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.Random;

/**
 * Original procedural "topographic" sky: contour lines of 3D noise evaluated on the sphere, so the
 * six cube faces join without seams. Generated once on a background thread, uploaded on the render
 * thread. Rendered as a large camera-centred cube behind terrain (depth tested).
 */
public final class SkyDome {
    public static final Identifier TEXTURE = Identifier.of("aqua", "dynamic/sky_topographic");
    private static final int N = 768;
    private static volatile int[] pending;
    private static boolean uploaded;
    private static boolean started;

    private static final float[][] C = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
    private static final float[][] S = {{0, 0, -1}, {0, 0, 1}, {1, 0, 0}, {1, 0, 0}, {1, 0, 0}, {-1, 0, 0}};
    private static final float[][] T = {{0, -1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}, {0, -1, 0}, {0, -1, 0}};

    private SkyDome() {}

    public static void generateAsync() {
        if (started) {
            return;
        }
        started = true;
        Thread t = new Thread(() -> {
            long t0 = System.currentTimeMillis();
            pending = generate();
            AquaClient.LOGGER.info("Aqua topographic sky generated in {} ms", System.currentTimeMillis() - t0);
        }, "Aqua Sky Generator");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    public static boolean ready() {
        if (!uploaded && pending != null) {
            int[] px = pending;
            pending = null;
            NativeImage img = new NativeImage(NativeImage.Format.RGBA, N * 3, N * 2, false);
            for (int y = 0; y < N * 2; y++) {
                for (int x = 0; x < N * 3; x++) {
                    img.setColorArgb(x, y, px[y * N * 3 + x]);
                }
            }
            MinecraftClient.getInstance().getTextureManager().registerTexture(TEXTURE, new NativeImageBackedTexture(() -> "aqua_sky", img));
            uploaded = true;
        }
        return uploaded;
    }

    private static int[] generate() {
        Noise noise = new Noise(1337);
        int[] out = new int[N * 3 * N * 2];
        float levels = 12f;
        for (int f = 0; f < 6; f++) {
            int ox = (f % 3) * N;
            int oy = (f / 3) * N;
            for (int j = 0; j < N; j++) {
                for (int i = 0; i < N; i++) {
                    float u = (i + 0.5f) / N * 2f - 1f;
                    float v = (j + 0.5f) / N * 2f - 1f;
                    float dx = C[f][0] + u * S[f][0] + v * T[f][0];
                    float dy = C[f][1] + u * S[f][1] + v * T[f][1];
                    float dz = C[f][2] + u * S[f][2] + v * T[f][2];
                    float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                    dx /= len;
                    dy /= len;
                    dz /= len;
                    float val = fbm(noise, dx * 2.6f, dy * 2.6f, dz * 2.6f) * levels;
                    // Gradient magnitude (finite differences) keeps line width uniform in screen space.
                    float e = 2.6f / N;
                    float gx = fbm(noise, dx * 2.6f + e, dy * 2.6f, dz * 2.6f) * levels - val;
                    float gy = fbm(noise, dx * 2.6f, dy * 2.6f + e, dz * 2.6f) * levels - val;
                    float gz = fbm(noise, dx * 2.6f, dy * 2.6f, dz * 2.6f + e) * levels - val;
                    float grad = (float) Math.sqrt(gx * gx + gy * gy + gz * gz) + 1e-5f;
                    float fr = val - (float) Math.floor(val);
                    float dist = Math.min(fr, 1f - fr) / grad;
                    float width = 0.85f;
                    float a = Math.max(0f, Math.min(1f, (width - dist) / 1.0f + 0.5f));
                    out[(oy + j) * N * 3 + ox + i] = ((int) (a * 255) << 24) | 0xFFFFFF;
                }
            }
        }
        return out;
    }

    private static float fbm(Noise n, float x, float y, float z) {
        return n.noise(x, y, z) * 0.62f + n.noise(x * 2.03f, y * 2.03f, z * 2.03f) * 0.26f + n.noise(x * 4.1f, y * 4.1f, z * 4.1f) * 0.12f;
    }

    /** Draws the cube: a tinted base pass plus the contour line overlay (or a vertical gradient). */
    public static void render(Fx fx, net.minecraft.client.render.VertexConsumerProvider vcp, float radius, int top, int horizon,
                              int lineColor, boolean topographic, float drift) {
        Matrix4f m = fx.matrices.peek().getPositionMatrix();
        VertexConsumer base = vcp.getBuffer(FxLayers.COLOR);
        int sub = 6;
        for (int f = 0; f < 6; f++) {
            for (int a = 0; a < sub; a++) {
                for (int b = 0; b < sub; b++) {
                    float u0 = a / (float) sub * 2 - 1, u1 = (a + 1) / (float) sub * 2 - 1;
                    float v0 = b / (float) sub * 2 - 1, v1 = (b + 1) / (float) sub * 2 - 1;
                    gradVertex(base, m, f, u0, v0, radius, top, horizon);
                    gradVertex(base, m, f, u0, v1, radius, top, horizon);
                    gradVertex(base, m, f, u1, v1, radius, top, horizon);
                    gradVertex(base, m, f, u1, v0, radius, top, horizon);
                }
            }
        }
        if (topographic && ready()) {
            VertexConsumer lines = vcp.getBuffer(FxLayers.textured(TEXTURE));
            for (int f = 0; f < 6; f++) {
                float tu = (f % 3) / 3f;
                float tv = (f / 3) / 2f;
                texVertex(lines, m, f, -1, -1, radius * 0.995f, tu, tv, lineColor);
                texVertex(lines, m, f, -1, 1, radius * 0.995f, tu, tv, lineColor);
                texVertex(lines, m, f, 1, 1, radius * 0.995f, tu, tv, lineColor);
                texVertex(lines, m, f, 1, -1, radius * 0.995f, tu, tv, lineColor);
            }
        }
    }

    private static void gradVertex(VertexConsumer vc, Matrix4f m, int f, float u, float v, float r, int top, int horizon) {
        float x = C[f][0] + u * S[f][0] + v * T[f][0];
        float y = C[f][1] + u * S[f][1] + v * T[f][1];
        float z = C[f][2] + u * S[f][2] + v * T[f][2];
        float len = (float) Math.sqrt(x * x + y * y + z * z);
        float h = y / len;
        float t = h >= 0 ? (float) Math.pow(h, 0.6) : 0f;
        int c = Colors.lerp(horizon, top, t);
        if (h < 0) {
            c = Colors.lerp(horizon, Colors.darken(horizon, 0.5f), Math.min(1f, -h * 2f));
        }
        vc.vertex(m, x * r, y * r, z * r).color(c);
    }

    private static void texVertex(VertexConsumer vc, Matrix4f m, int f, float u, float v, float r, float tu, float tv, int color) {
        float x = C[f][0] + u * S[f][0] + v * T[f][0];
        float y = C[f][1] + u * S[f][1] + v * T[f][1];
        float z = C[f][2] + u * S[f][2] + v * T[f][2];
        float uu = tu + (u * 0.5f + 0.5f) / 3f;
        float vv = tv + (v * 0.5f + 0.5f) / 2f;
        vc.vertex(m, x * r, y * r, z * r).texture(uu, vv).color(color);
    }

    /** Small Perlin-style gradient noise. */
    static final class Noise {
        private final int[] p = new int[512];

        Noise(long seed) {
            int[] perm = new int[256];
            for (int i = 0; i < 256; i++) {
                perm[i] = i;
            }
            Random r = new Random(seed);
            for (int i = 255; i > 0; i--) {
                int j = r.nextInt(i + 1);
                int tmp = perm[i];
                perm[i] = perm[j];
                perm[j] = tmp;
            }
            for (int i = 0; i < 512; i++) {
                p[i] = perm[i & 255];
            }
        }

        private static float fade(float t) {
            return t * t * t * (t * (t * 6 - 15) + 10);
        }

        private static float grad(int h, float x, float y, float z) {
            int k = h & 15;
            float u = k < 8 ? x : y;
            float v = k < 4 ? y : (k == 12 || k == 14 ? x : z);
            return ((k & 1) == 0 ? u : -u) + ((k & 2) == 0 ? v : -v);
        }

        float noise(float x, float y, float z) {
            int X = (int) Math.floor(x) & 255, Y = (int) Math.floor(y) & 255, Z = (int) Math.floor(z) & 255;
            x -= (float) Math.floor(x);
            y -= (float) Math.floor(y);
            z -= (float) Math.floor(z);
            float u = fade(x), v = fade(y), w = fade(z);
            int A = p[X] + Y, AA = p[A] + Z, AB = p[A + 1] + Z, B = p[X + 1] + Y, BA = p[B] + Z, BB = p[B + 1] + Z;
            return lerp(w, lerp(v, lerp(u, grad(p[AA], x, y, z), grad(p[BA], x - 1, y, z)),
                            lerp(u, grad(p[AB], x, y - 1, z), grad(p[BB], x - 1, y - 1, z))),
                    lerp(v, lerp(u, grad(p[AA + 1], x, y, z - 1), grad(p[BA + 1], x - 1, y, z - 1)),
                            lerp(u, grad(p[AB + 1], x, y - 1, z - 1), grad(p[BB + 1], x - 1, y - 1, z - 1))));
        }

        private static float lerp(float t, float a, float b) {
            return a + t * (b - a);
        }
    }

    public static Vec3d unused() {
        return Vec3d.ZERO;
    }
}
