package dev.aqua.client.module.perf;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.NumberSetting;

/** Drops a share of new vanilla particles to keep busy scenes smooth (visual only). */
public final class ParticleLimiter extends Module {
    private final NumberSetting keep = add(new NumberSetting("keep", "Particles kept", 60, 10, 100, 5).unit("%"));
    private int counter;

    public ParticleLimiter() {
        super("particle_limiter", "Particle Limiter", "Reduces how many vanilla particles are spawned.", Category.PERFORMANCE, false,
                "particles", "fps", "lag");
    }

    /** Deterministic thinning: keeps roughly keep% of spawns. */
    public boolean allow() {
        if (!isEnabled()) {
            return true;
        }
        counter = (counter + 1) % 100;
        return (counter * 37 % 100) < keep.i();
    }
}
