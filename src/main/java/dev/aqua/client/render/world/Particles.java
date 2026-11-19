package dev.aqua.client.render.world;

import dev.aqua.client.Aqua;
import dev.aqua.client.render.Anim;
import dev.aqua.client.render.Colors;

/**
 * Fixed-size pool of lightweight glowing particles shared by cosmetic effects. The pool never
 * grows; when full, new spawns are dropped. The budget scales with Effects quality x intensity.
 */
public final class Particles {
    public static final int CAPACITY = 1024;
    private static final double[] px = new double[CAPACITY], py = new double[CAPACITY], pz = new double[CAPACITY];
    private static final float[] vx = new float[CAPACITY], vy = new float[CAPACITY], vz = new float[CAPACITY];
    private static final float[] life = new float[CAPACITY], maxLife = new float[CAPACITY], size = new float[CAPACITY];
    private static final float[] gravity = new float[CAPACITY], drag = new float[CAPACITY];
    private static final int[] color = new int[CAPACITY];
    private static int count;

    private Particles() {}

    public static int budget() {
        return Math.max(0, Math.min(CAPACITY, Math.round(CAPACITY * 0.6f * Aqua.ui().particleBudget())));
    }

    public static int count() {
        return count;
    }

    public static void spawn(double x, double y, double z, float velX, float velY, float velZ, float lifeSec, float sz, int argb,
                             float grav, float drg) {
        if (count >= budget()) {
            return;
        }
        int i = count++;
        px[i] = x;
        py[i] = y;
        pz[i] = z;
        vx[i] = velX;
        vy[i] = velY;
        vz[i] = velZ;
        life[i] = 0;
        maxLife[i] = Math.max(0.05f, lifeSec);
        size[i] = sz;
        color[i] = argb;
        gravity[i] = grav;
        drag[i] = drg;
    }

    public static void clear() {
        count = 0;
    }

    /** Integrates and draws every particle. Called once per frame from the world pass. */
    public static void renderAll(Fx fx) {
        float dt = Anim.delta();
        int i = 0;
        while (i < count) {
            life[i] += dt;
            if (life[i] >= maxLife[i]) {
                int last = --count;
                px[i] = px[last];
                py[i] = py[last];
                pz[i] = pz[last];
                vx[i] = vx[last];
                vy[i] = vy[last];
                vz[i] = vz[last];
                life[i] = life[last];
                maxLife[i] = maxLife[last];
                size[i] = size[last];
                color[i] = color[last];
                gravity[i] = gravity[last];
                drag[i] = drag[last];
                continue;
            }
            float k = (float) Math.exp(-drag[i] * dt);
            vx[i] *= k;
            vy[i] = vy[i] * k - gravity[i] * dt;
            vz[i] *= k;
            px[i] += vx[i] * dt;
            py[i] += vy[i] * dt;
            pz[i] += vz[i] * dt;
            float t = life[i] / maxLife[i];
            float fade = t < 0.15f ? t / 0.15f : 1f - (t - 0.15f) / 0.85f;
            fx.spark(px[i], py[i], pz[i], size[i] * (1f - t * 0.4f), Colors.fade(color[i], fade));
            i++;
        }
    }
}
