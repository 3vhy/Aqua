package dev.aqua.client.render.world;

import dev.aqua.client.AquaClient;
import dev.aqua.client.render.Colors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Procedurally generated full-colour sky boxes (nebula, aurora, lava cells). Each is generated once on
 * a background thread the first time it is needed and then drawn as a textured cube around the camera.
 */
public final class SkyTextures {
    public enum Kind {
        NEBULA_VIOLET, NEBULA_EMERALD, NEBULA_AZURE, NEBULA_CRIMSON, NEBULA_SILVER, AURORA, CELLS
    }

    private static final int N = 512;
    private static final float[][] C = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
    private static final float[][] S = {{0, 0, -1}, {0, 0, 1}, {1, 0, 0}, {1, 0, 0}, {1, 0, 0}, {-1, 0, 0}};
    private static final float[][] T = {{0, -1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}, {0, -1, 0}, {0, -1, 0}};

    private static final Map<Kind, int[]> PENDING = new ConcurrentHashMap<>();
    private static final Map<Kind, Identifier> READY = new ConcurrentHashMap<>();
    private static final Map<Kind, Boolean> STARTED = new ConcurrentHashMap<>();

    private SkyTextures() {}

    /** The texture for {@code kind}, or null while it is still being generated. */
    public static Identifier get(Kind kind) {
        Identifier id = READY.get(kind);
        if (id != null) {
            return id;
        }
        int[] px = PENDING.remove(kind);
        if (px != null) {
            NativeImage img = new NativeImage(NativeImage.Format.RGBA, N * 3, N * 2, false);
            for (int y = 0; y < N * 2; y++) {
                for (int x = 0; x < N * 3; x++) {
                    img.setColorArgb(x, y, px[y * N * 3 + x]);
                }
            }
            id = Identifier.of("aqua", "dynamic/sky_" + kind.name().toLowerCase(java.util.Locale.ROOT));
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, new NativeImageBackedTexture(() -> "aqua_sky_" + kind, img));
            READY.put(kind, id);
            return id;
        }
        if (STARTED.putIfAbsent(kind, true) == null) {
            Thread t = new Thread(() -> {
                long t0 = System.currentTimeMillis();
                PENDING.put(kind, generate(kind));
                AquaClient.LOGGER.info("Aqua {} sky generated in {} ms", kind, System.currentTimeMillis() - t0);
            }, "Aqua Sky Generator");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            t.start();
        }
        return null;
    }

    /** Draws a generated sky box (if ready) tinted by {@code color}; returns false while it is still generating. */
    public static boolean render(Fx fx, net.minecraft.client.render.VertexConsumerProvider vcp, Kind kind, float radius, int color) {
        Identifier id = get(kind);
        if (id == null) {
            return false;
        }
        Matrix4f m = fx.matrices.peek().getPositionMatrix();
        VertexConsumer vc = vcp.getBuffer(FxLayers.textured(id));
        for (int f = 0; f < 6; f++) {
            float tu = (f % 3) / 3f;
            float tv = (f / 3) / 2f;
            vertex(vc, m, f, -1, -1, radius, tu, tv, color);
            vertex(vc, m, f, -1, 1, radius, tu, tv, color);
            vertex(vc, m, f, 1, 1, radius, tu, tv, color);
            vertex(vc, m, f, 1, -1, radius, tu, tv, color);
        }
        return true;
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, int f, float u, float v, float r, float tu, float tv, int color) {
        float x = C[f][0] + u * S[f][0] + v * T[f][0];
        float y = C[f][1] + u * S[f][1] + v * T[f][1];
        float z = C[f][2] + u * S[f][2] + v * T[f][2];
        // Half a texel inset so neighbouring faces do not bleed into each other.
        float inset = 0.5f / N;
        float uu = tu + (Math.max(inset, Math.min(1 - inset, u * 0.5f + 0.5f))) / 3f;
        float vv = tv + (Math.max(inset, Math.min(1 - inset, v * 0.5f + 0.5f))) / 2f;
        vc.vertex(m, x * r, y * r, z * r).texture(uu, vv).color(color);
    }

    // ------------------------------------------------------------------ generation

    private interface Shader {
        int color(float x, float y, float z);
    }

    private static int[] generate(Kind kind) {
        SkyDome.Noise noise = new SkyDome.Noise(kind.ordinal() * 7919 + 17);
        SkyDome.Noise noise2 = new SkyDome.Noise(kind.ordinal() * 104729 + 3);
        Shader sh = switch (kind) {
            case NEBULA_VIOLET -> nebula(noise, noise2, 0xFF07031A, 0xFF4B1A86, 0xFFFF6FD8, 0xFF8FD8FF);
            case NEBULA_EMERALD -> nebula(noise, noise2, 0xFF02120D, 0xFF0E6447, 0xFFD6F27A, 0xFF5FE0C0);
            case NEBULA_AZURE -> nebula(noise, noise2, 0xFF020716, 0xFF113A7C, 0xFF8BE8FF, 0xFFE6F6FF);
            case NEBULA_CRIMSON -> nebula(noise, noise2, 0xFF120205, 0xFF731022, 0xFFFFA05C, 0xFFFFD9A0);
            case NEBULA_SILVER -> nebula(noise, noise2, 0xFF0A0C10, 0xFF55606C, 0xFFF4F7FA, 0xFF9FC8E8);
            case AURORA -> aurora(noise, noise2);
            case CELLS -> cells();
        };
        int[] out = new int[N * 3 * N * 2];
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
                    out[(oy + j) * N * 3 + ox + i] = sh.color(dx / len, dy / len, dz / len);
                }
            }
        }
        return out;
    }

    private static float fbm(SkyDome.Noise n, float x, float y, float z, int octaves) {
        float sum = 0, amp = 0.55f, f = 1f, norm = 0;
        for (int i = 0; i < octaves; i++) {
            sum += n.noise(x * f, y * f, z * f) * amp;
            norm += amp;
            amp *= 0.5f;
            f *= 2.03f;
        }
        return sum / norm;
    }

    private static float smooth(float e0, float e1, float x) {
        float t = Math.max(0f, Math.min(1f, (x - e0) / (e1 - e0)));
        return t * t * (3 - 2 * t);
    }

    /** Deterministic hash in [0, 1). */
    private static float hash(int x, int y, int z) {
        int h = x * 374761393 + y * 668265263 + z * 2147483647;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xFFFFFF) / (float) 0x1000000;
    }

    /** Small points of light on a 3D grid. */
    private static float stars(float x, float y, float z, float scale, float density) {
        float sx = x * scale, sy = y * scale, sz = z * scale;
        int ix = (int) Math.floor(sx), iy = (int) Math.floor(sy), iz = (int) Math.floor(sz);
        float h = hash(ix, iy, iz);
        if (h > density) {
            return 0f;
        }
        float cx = ix + 0.25f + 0.5f * hash(iy, iz, ix), cy = iy + 0.25f + 0.5f * hash(iz, ix, iy), cz = iz + 0.25f + 0.5f * hash(ix + 7, iy, iz);
        float d = (float) Math.sqrt((sx - cx) * (sx - cx) + (sy - cy) * (sy - cy) + (sz - cz) * (sz - cz));
        return Math.max(0f, 1f - d * 9f) * (0.4f + 0.6f * hash(ix, iz, iy));
    }

    private static Shader nebula(SkyDome.Noise n, SkyDome.Noise n2, int deep, int mid, int bright, int cool) {
        return (x, y, z) -> {
            float a = fbm(n, x * 1.6f, y * 1.6f, z * 1.6f, 5) * 0.5f + 0.5f;
            float b = fbm(n2, x * 3.2f + 5f, y * 3.2f, z * 3.2f, 4) * 0.5f + 0.5f;
            // Wispy ridges: bright filaments where the noise crosses its middle.
            float ridge = 1f - Math.abs(fbm(n2, x * 2.4f, y * 2.4f + 3f, z * 2.4f, 5)) * 2.2f;
            ridge = Math.max(0f, ridge);
            ridge = ridge * ridge * ridge;
            float cloud = smooth(0.42f, 0.8f, a);
            int c = Colors.lerp(deep, mid, cloud);
            c = Colors.lerp(c, bright, Math.min(1f, ridge * 0.85f * cloud + smooth(0.72f, 0.95f, b) * 0.35f));
            c = Colors.lerp(c, cool, smooth(0.6f, 0.95f, b) * (1f - cloud) * 0.45f);
            float s = stars(x, y, z, 220f, 0.03f) + stars(x, y, z, 90f, 0.012f) * 1.3f;
            c = Colors.lerp(c, 0xFFFFFFFF, Math.min(1f, s * (1f - cloud * 0.6f)));
            return c | 0xFF000000;
        };
    }

    private static Shader aurora(SkyDome.Noise n, SkyDome.Noise n2) {
        return (x, y, z) -> {
            int night = Colors.lerp(0xFF02060C, 0xFF061A22, Math.max(0f, 1f - Math.abs(y) * 2.2f));
            if (y < -0.05f) {
                return Colors.lerp(night, 0xFF010306, Math.min(1f, -y * 3f)) | 0xFF000000;
            }
            double az = Math.atan2(z, x);
            float az1 = (float) Math.cos(az), az2 = (float) Math.sin(az);
            // Curtains: bands that wave along the horizon, rising into soft rays.
            float band = fbm(n, az1 * 1.3f, az2 * 1.3f, y * 0.4f, 4);
            float wave = (float) Math.sin(az * 3.0 + band * 6.0);
            float curtain = smooth(0.15f, 0.9f, wave * 0.5f + 0.5f);
            float rays = 0.55f + 0.45f * (fbm(n2, az1 * 14f, az2 * 14f, 0.5f, 3) * 0.5f + 0.5f);
            float height = smooth(0.0f, 0.12f, y) * (1f - smooth(0.28f, 0.75f, y + band * 0.18f));
            float k = curtain * rays * height;
            int green = 0xFF3CFFA2, teal = 0xFF22D3C5, violet = 0xFF9B5CFF;
            int col = Colors.lerp(green, teal, smooth(0.25f, 0.55f, y));
            col = Colors.lerp(col, violet, smooth(0.45f, 0.8f, y + band * 0.2f));
            int c = Colors.lerp(night, col, Math.min(1f, k * 1.25f));
            float s = stars(x, y, z, 200f, 0.035f);
            c = Colors.lerp(c, 0xFFFFFFFF, Math.min(1f, s * (1f - k)));
            return c | 0xFF000000;
        };
    }

    private static Shader cells() {
        // Glowing cell edges (Voronoi on the sphere) on black: white mask, tinted when drawn.
        java.util.Random r = new java.util.Random(4242);
        int count = 420;
        float[][] pts = new float[count][];
        for (int i = 0; i < count; i++) {
            float zz = r.nextFloat() * 2 - 1, a = (float) (r.nextFloat() * Math.PI * 2);
            float rr = (float) Math.sqrt(1 - zz * zz);
            pts[i] = new float[]{rr * (float) Math.cos(a), zz, rr * (float) Math.sin(a)};
        }
        return (x, y, z) -> {
            float d1 = 9, d2 = 9;
            for (float[] p : pts) {
                float d = (x - p[0]) * (x - p[0]) + (y - p[1]) * (y - p[1]) + (z - p[2]) * (z - p[2]);
                if (d < d1) {
                    d2 = d1;
                    d1 = d;
                } else if (d < d2) {
                    d2 = d;
                }
            }
            float edge = (float) (Math.sqrt(d2) - Math.sqrt(d1));
            float core = 1f - smooth(0.0f, 0.012f, edge);
            float halo = (1f - smooth(0.0f, 0.075f, edge)) * 0.55f;
            float v = Math.min(1f, core + halo * halo);
            int g = Math.round(v * 255);
            return 0xFF000000 | (g << 16) | (g << 8) | g;
        };
    }
}
