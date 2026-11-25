package dev.aqua.client.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Registry and lifecycle owner for all modules. */
public final class ModuleManager {
    private final List<Module> modules = new ArrayList<>();
    private final Map<String, Module> byId = new HashMap<>();
    private final Map<Class<?>, Module> byClass = new HashMap<>();

    public <M extends Module> M register(M m) {
        if (byId.containsKey(m.id())) {
            throw new IllegalStateException("Duplicate module id " + m.id());
        }
        modules.add(m);
        byId.put(m.id(), m);
        byClass.put(m.getClass(), m);
        return m;
    }

    public List<Module> all() {
        return Collections.unmodifiableList(modules);
    }

    public Module byId(String id) {
        return byId.get(id);
    }

    @SuppressWarnings("unchecked")
    public <M extends Module> M get(Class<M> type) {
        return (M) byClass.get(type);
    }

    public List<Module> inCategory(Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : modules) {
            if (m.category() == c) {
                out.add(m);
            }
        }
        return out;
    }

    public List<Module> search(String query) {
        List<Module> out = new ArrayList<>();
        for (Module m : modules) {
            if (m.matches(query)) {
                out.add(m);
            }
        }
        return out;
    }

    public void tick() {
        for (Module m : modules) {
            if (m.isEnabled()) {
                try {
                    m.onTick();
                } catch (Throwable t) {
                    dev.aqua.client.util.Problems.report("Module " + m.id() + " tick failed; disabling it", t);
                    m.setEnabled(false, false);
                }
            }
        }
    }

    public void resetAll() {
        for (Module m : modules) {
            m.resetToDefaults();
        }
    }
}
