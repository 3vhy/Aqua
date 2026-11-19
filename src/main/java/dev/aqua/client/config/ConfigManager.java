package dev.aqua.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aqua.client.Aqua;
import dev.aqua.client.AquaClient;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Versioned, validated configuration under {@code config/aqua/}:
 * <ul>
 *   <li>{@code aqua.json} – client-wide preferences and the active profile name</li>
 *   <li>{@code profiles/<name>.json} – module states, settings, key binds, HUD layout and theme</li>
 *   <li>{@code exports/} – exported profiles</li>
 * </ul>
 * Files are written atomically (temp file + move) and malformed files are quarantined and replaced by
 * defaults instead of crashing the game.
 */
public final class ConfigManager {
    public static final int GLOBAL_VERSION = 1;
    public static final int PROFILE_VERSION = 1;
    public static final String SHARE_PREFIX = "AQUA1:";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Path dir;
    private final Path profilesDir;
    private final Path exportsDir;
    private final Path globalFile;
    private String activeProfile = "Default";
    private boolean dirty;
    private int dirtyTicks;
    private boolean loading;
    private final List<String> pendingWarnings = new ArrayList<>();

    public ConfigManager() {
        this.dir = FabricLoader.getInstance().getConfigDir().resolve("aqua");
        this.profilesDir = dir.resolve("profiles");
        this.exportsDir = dir.resolve("exports");
        this.globalFile = dir.resolve("aqua.json");
    }

    public Path dir() {
        return dir;
    }

    public Path exportsDir() {
        return exportsDir;
    }

    public String activeProfile() {
        return activeProfile;
    }

    public List<String> drainWarnings() {
        List<String> w = new ArrayList<>(pendingWarnings);
        pendingWarnings.clear();
        return w;
    }

    public void markDirty() {
        if (!loading) {
            dirty = true;
            dirtyTicks = 0;
        }
    }

    /** Debounced save: writes one second after the last change. */
    public void tick() {
        if (dirty && ++dirtyTicks >= 20) {
            save();
        }
    }

    // ------------------------------------------------------------------ load

    public void load() {
        loading = true;
        try {
            Files.createDirectories(profilesDir);
            Files.createDirectories(exportsDir);
            Files.createDirectories(importDir());
            Files.createDirectories(dir.resolve("music"));
        } catch (IOException e) {
            AquaClient.LOGGER.error("Cannot create Aqua config directories", e);
        }
        JsonObject global = readObject(globalFile);
        if (global != null) {
            int v = intOr(global, "version", 0);
            if (v > GLOBAL_VERSION) {
                pendingWarnings.add("aqua.json is from a newer Aqua version; unknown values were ignored");
            }
            if (global.has("activeProfile")) {
                String p = sanitizeName(global.get("activeProfile").getAsString());
                if (p != null) {
                    activeProfile = p;
                }
            }
            if (global.has("ui") && global.get("ui").isJsonObject()) {
                readSettings(Aqua.ui().all(), global.getAsJsonObject("ui"));
            }
            if (global.has("menuKey") && global.get("menuKey").isJsonObject()) {
                Aqua.keybinds().menuKeyFromJson(global.getAsJsonObject("menuKey"));
            }
        }
        if (!Files.exists(profileFile(activeProfile))) {
            if (!"Default".equals(activeProfile) || !Files.exists(profileFile("Default"))) {
                applyDefaults();
                loading = false;
                writeProfile(activeProfile);
                writeGlobal();
                loading = true;
            }
        }
        loadProfileInto(activeProfile);
        loading = false;
        dirty = false;
    }

    private void loadProfileInto(String name) {
        applyDefaults();
        JsonObject o = readObject(profileFile(name));
        if (o == null) {
            return;
        }
        applyProfileJson(o);
    }

    private void applyDefaults() {
        for (Module m : Aqua.modules().all()) {
            for (Setting<?> s : m.settings()) {
                s.reset();
            }
            m.bind().set(m.defaultBind());
            m.setEnabled(m.enabledByDefault(), false);
        }
        Aqua.hud().resetAll();
        Aqua.themes().reset();
    }

    private void applyProfileJson(JsonObject o) {
        int v = intOr(o, "version", 0);
        if (v > PROFILE_VERSION) {
            pendingWarnings.add("Profile was saved by a newer Aqua version; unknown values were ignored");
        }
        if (o.has("modules") && o.get("modules").isJsonObject()) {
            JsonObject mods = o.getAsJsonObject("modules");
            for (Module m : Aqua.modules().all()) {
                if (!mods.has(m.id()) || !mods.get(m.id()).isJsonObject()) {
                    continue;
                }
                JsonObject mo = mods.getAsJsonObject(m.id());
                if (mo.has("settings") && mo.get("settings").isJsonObject()) {
                    readSettings(m.settings(), mo.getAsJsonObject("settings"));
                }
                if (mo.has("bind")) {
                    m.bind().fromJson(mo.get("bind"));
                }
                if (mo.has("enabled") && mo.get("enabled").isJsonPrimitive()) {
                    m.setEnabled(mo.get("enabled").getAsBoolean(), false);
                }
            }
        }
        if (o.has("hud") && o.get("hud").isJsonObject()) {
            Aqua.hud().fromJson(o.getAsJsonObject("hud"));
        }
        if (o.has("theme") && o.get("theme").isJsonObject()) {
            Aqua.themes().fromJson(o.getAsJsonObject("theme"));
        }
    }

    private static void readSettings(List<Setting<?>> settings, JsonObject obj) {
        for (Setting<?> s : settings) {
            if (!s.persistent() || !obj.has(s.id())) {
                continue;
            }
            try {
                if (!s.fromJson(obj.get(s.id()))) {
                    AquaClient.LOGGER.warn("Ignoring invalid value for setting {}", s.id());
                }
            } catch (Exception e) {
                AquaClient.LOGGER.warn("Ignoring malformed setting {}", s.id());
            }
        }
    }

    @SuppressWarnings("null")
    private JsonObject readObject(Path file) {
        if (!Files.exists(file)) {
            return null;
        }
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            JsonElement e = JsonParser.parseString(text);
            if (!e.isJsonObject()) {
                throw new IllegalStateException("root is not an object");
            }
            return e.getAsJsonObject();
        } catch (Exception ex) {
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            Path quarantine = file.resolveSibling(file.getFileName() + ".corrupt-" + stamp);
            try {
                Files.move(file, quarantine, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
            }
            AquaClient.LOGGER.error("Malformed Aqua config {} moved to {}; defaults restored", file, quarantine, ex);
            pendingWarnings.add("Recovered from a damaged config file (" + file.getFileName() + "); defaults restored");
            return null;
        }
    }

    // ------------------------------------------------------------------ save

    public void save() {
        dirty = false;
        dirtyTicks = 0;
        writeProfile(activeProfile);
        writeGlobal();
    }

    private void writeGlobal() {
        JsonObject g = new JsonObject();
        g.addProperty("format", "aqua-client");
        g.addProperty("version", GLOBAL_VERSION);
        g.addProperty("activeProfile", activeProfile);
        g.add("ui", writeSettings(Aqua.ui().all()));
        g.add("menuKey", Aqua.keybinds().menuKeyToJson());
        writeAtomic(globalFile, GSON.toJson(g));
    }

    private void writeProfile(String name) {
        writeAtomic(profileFile(name), GSON.toJson(currentProfileJson(name)));
    }

    private JsonObject currentProfileJson(String name) {
        JsonObject p = new JsonObject();
        p.addProperty("format", "aqua-profile");
        p.addProperty("version", PROFILE_VERSION);
        p.addProperty("name", name);
        JsonObject mods = new JsonObject();
        for (Module m : Aqua.modules().all()) {
            JsonObject mo = new JsonObject();
            mo.addProperty("enabled", m.isEnabled());
            mo.add("bind", m.bind().toJson());
            mo.add("settings", writeSettings(m.settings()));
            mods.add(m.id(), mo);
        }
        p.add("modules", mods);
        p.add("hud", Aqua.hud().toJson());
        p.add("theme", Aqua.themes().toJson());
        return p;
    }

    private static JsonObject writeSettings(List<Setting<?>> settings) {
        JsonObject o = new JsonObject();
        for (Setting<?> s : settings) {
            if (s.persistent()) {
                o.add(s.id(), s.toJson());
            }
        }
        return o;
    }

    private static void writeAtomic(Path file, String content) {
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, content, StandardCharsets.UTF_8);
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            AquaClient.LOGGER.error("Failed to save {}", file, e);
        }
    }

    // ------------------------------------------------------------------ profiles

    public Path profileFile(String name) {
        return profilesDir.resolve(name + ".json");
    }

    public static String sanitizeName(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim().replaceAll("[^A-Za-z0-9 _\\-]", "");
        if (s.isEmpty() || s.length() > 32) {
            return null;
        }
        return s;
    }

    public List<String> profiles() {
        List<String> out = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(profilesDir, "*.json")) {
            for (Path p : ds) {
                String n = p.getFileName().toString();
                out.add(n.substring(0, n.length() - 5));
            }
        } catch (IOException ignored) {
        }
        if (!out.contains(activeProfile)) {
            out.add(activeProfile);
        }
        out.sort(String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    private boolean exists(String name) {
        for (String p : profiles()) {
            if (p.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    public String uniqueName(String base) {
        String b = sanitizeName(base);
        if (b == null) {
            b = "Profile";
        }
        if (!exists(b)) {
            return b;
        }
        for (int i = 2; i < 1000; i++) {
            String c = b.length() > 28 ? b.substring(0, 28) : b;
            String n = c + " " + i;
            if (!exists(n)) {
                return n;
            }
        }
        return b + System.currentTimeMillis() % 1000;
    }

    /** Creates a profile with default settings. Returns its name or null. */
    public String create(String rawName) {
        String name = sanitizeName(rawName);
        if (name == null || exists(name)) {
            return null;
        }
        save();
        loading = true;
        applyDefaults();
        loading = false;
        writeAtomic(profileFile(name), GSON.toJson(currentProfileJson(name)));
        loading = true;
        loadProfileInto(activeProfile);
        loading = false;
        return name;
    }

    public String duplicate(String source, String rawName) {
        String name = sanitizeName(rawName);
        if (name == null || exists(name)) {
            return null;
        }
        save();
        try {
            Files.copy(profileFile(source), profileFile(name), StandardCopyOption.REPLACE_EXISTING);
            return name;
        } catch (IOException e) {
            AquaClient.LOGGER.error("Duplicate failed", e);
            return null;
        }
    }

    public boolean rename(String from, String rawTo) {
        String to = sanitizeName(rawTo);
        if (to == null || (exists(to) && !to.equalsIgnoreCase(from))) {
            return false;
        }
        save();
        try {
            Files.move(profileFile(from), profileFile(to), StandardCopyOption.REPLACE_EXISTING);
            if (from.equals(activeProfile)) {
                activeProfile = to;
                save();
            }
            return true;
        } catch (IOException e) {
            AquaClient.LOGGER.error("Rename failed", e);
            return false;
        }
    }

    public boolean delete(String name) {
        if (name.equals(activeProfile) || profiles().size() <= 1) {
            return false;
        }
        try {
            Files.deleteIfExists(profileFile(name));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void switchTo(String name) {
        if (name.equals(activeProfile)) {
            return;
        }
        save();
        activeProfile = name;
        loading = true;
        loadProfileInto(name);
        loading = false;
        save();
    }

    /** Resets the active profile to defaults. */
    public void resetActive() {
        loading = true;
        applyDefaults();
        loading = false;
        save();
    }

    // ------------------------------------------------------------------ share codes / files

    public String exportCode(String name) {
        save();
        try {
            String json = Files.readString(profileFile(name), StandardCharsets.UTF_8);
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            byte[] raw = new Gson().toJson(o).getBytes(StandardCharsets.UTF_8);
            Deflater d = new Deflater(Deflater.BEST_COMPRESSION);
            d.setInput(raw);
            d.finish();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            while (!d.finished()) {
                int n = d.deflate(buf);
                bos.write(buf, 0, n);
            }
            d.end();
            return SHARE_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bos.toByteArray());
        } catch (Exception e) {
            AquaClient.LOGGER.error("Export failed", e);
            return null;
        }
    }

    /** Imports a share code as a new profile. Returns the new profile name or null. */
    public String importCode(String code, String preferredName) {
        if (code == null) {
            return null;
        }
        code = code.trim();
        if (!code.startsWith(SHARE_PREFIX) || code.length() > 200_000) {
            return null;
        }
        try {
            byte[] data = Base64.getUrlDecoder().decode(code.substring(SHARE_PREFIX.length()));
            Inflater inf = new Inflater();
            inf.setInput(data);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int total = 0;
            while (!inf.finished()) {
                int n = inf.inflate(buf);
                if (n == 0 && (inf.needsInput() || inf.needsDictionary())) {
                    break;
                }
                total += n;
                if (total > 2_000_000) {
                    throw new DataFormatException("too large");
                }
                bos.write(buf, 0, n);
            }
            inf.end();
            return importJson(bos.toString(StandardCharsets.UTF_8), preferredName);
        } catch (Exception e) {
            AquaClient.LOGGER.warn("Invalid share code", e);
            return null;
        }
    }

    public String importJson(String json, String preferredName) {
        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            if (!o.has("format") || !"aqua-profile".equals(o.get("format").getAsString())) {
                return null;
            }
            String base = preferredName != null ? preferredName : (o.has("name") ? o.get("name").getAsString() : "Imported");
            String name = uniqueName(base);
            o.addProperty("name", name);
            writeAtomic(profileFile(name), GSON.toJson(o));
            return name;
        } catch (Exception e) {
            return null;
        }
    }

    public Path exportFile(String name) {
        save();
        try {
            Files.createDirectories(exportsDir);
            Path target = exportsDir.resolve(name.toLowerCase(Locale.ROOT).replace(' ', '_') + ".aqua.json");
            Files.copy(profileFile(name), target, StandardCopyOption.REPLACE_EXISTING);
            return target;
        } catch (IOException e) {
            AquaClient.LOGGER.error("Export to file failed", e);
            return null;
        }
    }

    public Path importDir() {
        return dir.resolve("import");
    }

    /**
     * Imports every *.json placed in config/aqua/import as a new profile, then moves the file into
     * import/done so it is not imported twice. Returns the imported profile names.
     */
    public List<String> importFromFolder() {
        List<String> out = new ArrayList<>();
        Path in = importDir();
        try {
            Files.createDirectories(in.resolve("done"));
        } catch (IOException ignored) {
        }
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(in, "*.json")) {
            for (Path p : ds) {
                if (Files.size(p) > 2_000_000) {
                    continue;
                }
                String n = importJson(Files.readString(p, StandardCharsets.UTF_8), null);
                if (n != null) {
                    out.add(n);
                    Files.move(p, in.resolve("done").resolve(p.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException ignored) {
        }
        return out;
    }

    private static int intOr(JsonObject o, String k, int def) {
        try {
            return o.has(k) ? o.get(k).getAsInt() : def;
        } catch (Exception e) {
            return def;
        }
    }
}
