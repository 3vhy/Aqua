package dev.aqua.client.module.qol;

import dev.aqua.client.Aqua;
import dev.aqua.client.AquaClient;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.ActionSetting;
import dev.aqua.client.setting.EnumSetting;
import dev.aqua.client.setting.StringSetting;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.ProfileResult;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Shows a different skin for yourself on your own screen: another player's skin (looked up by name
 * through Minecraft's normal skin service) or a default skin. Other players still see your real skin.
 */
public final class SkinProtect extends Module {
    public enum Source implements EnumSetting.Displayable {
        PLAYER("Another player's skin"), STEVE("Default skin (Steve)"), DEFAULT("Default skin for my account");
        final String l;

        Source(String l) {
            this.l = l;
        }

        public String displayName() {
            return l;
        }
    }

    private final EnumSetting<Source> source = add(new EnumSetting<>("source", "Skin", Source.PLAYER));
    private final StringSetting player = add(new StringSetting("player", "Player name", "", 16).visibleWhen(this::isPlayer));

    @Nullable private volatile SkinTextures override;
    @Nullable private String loadedFor;
    private volatile boolean loading;
    private String status = "";

    public SkinProtect() {
        super("skin_protect", "Skin Change", "Shows another player's skin (or a default skin) as yours, on your screen only.",
                Category.QOL, false, "skin protect", "skin changer", "hide skin", "privacy");
        add(new ActionSetting("apply", "Load skin", "Apply", this::apply));
        source.onChange(v -> apply());
    }

    private boolean isPlayer() {
        return source.is(Source.PLAYER);
    }

    public String status() {
        return status;
    }

    public boolean loading() {
        return loading;
    }

    @Override
    protected void onEnable() {
        apply();
    }

    @Override
    protected void onDisable() {
        override = null;
        loadedFor = null;
    }

    /** Resolves the configured skin. Network lookups run off the render thread. */
    public void apply() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!isEnabled()) {
            return;
        }
        switch (source.get()) {
            case STEVE -> {
                override = DefaultSkinHelper.getSteve();
                status = "Using Steve";
            }
            case DEFAULT -> {
                override = mc.getSession() != null && mc.getSession().getUuidOrNull() != null
                        ? DefaultSkinHelper.getSkinTextures(mc.getSession().getUuidOrNull()) : DefaultSkinHelper.getSteve();
                status = "Using a default skin";
            }
            case PLAYER -> {
                String name = player.get().trim();
                if (!name.matches("[A-Za-z0-9_]{1,16}")) {
                    override = null;
                    status = name.isEmpty() ? "Enter a player name" : "Invalid player name";
                    return;
                }
                if (name.equalsIgnoreCase(loadedFor) && override != null) {
                    return;
                }
                loading = true;
                override = null;
                status = "Looking up " + name + "…";
                CompletableFuture.supplyAsync(() -> resolve(mc, name), Util.getDownloadWorkerExecutor())
                        .thenCompose(profile -> profile.isPresent()
                                ? mc.getSkinProvider().fetchSkinTextures(profile.get())
                                : CompletableFuture.completedFuture(Optional.<SkinTextures>empty()))
                        .whenComplete((skin, error) -> mc.execute(() -> {
                            loading = false;
                            if (error != null || skin == null || skin.isEmpty()) {
                                Throwable cause = error instanceof java.util.concurrent.CompletionException c && c.getCause() != null ? c.getCause() : error;
                                status = cause instanceof NoSuchPlayer ? "No player called " + name
                                        : "Could not load the skin of " + name + (cause instanceof Busy ? " (Mojang is busy, try again soon)" : "");
                                if (error != null && !(cause instanceof NoSuchPlayer)) {
                                    AquaClient.LOGGER.warn("Skin Protect lookup failed for {}: {}", name, cause.toString());
                                }
                                Aqua.notifications().warn("Skin Protect", status);
                                return;
                            }
                            override = skin.get();
                            loadedFor = name;
                            status = "Showing " + name + "'s skin";
                            Aqua.notifications().success("Skin Protect", status);
                        }));
            }
        }
    }

    private static final class NoSuchPlayer extends RuntimeException {
        NoSuchPlayer() {
            super("no such player", null, false, false);
        }
    }

    private static final class Busy extends RuntimeException {
        Busy() {
            super("skin service busy", null, false, false);
        }
    }

    /**
     * Name to UUID, then the signed profile with its textures. The session server rate-limits
     * profile requests, so a failed fetch is retried twice with a short pause (uncached path).
     */
    private static Optional<GameProfile> resolve(MinecraftClient mc, String name) {
        java.util.UUID id = lookupId(name);
        for (int attempt = 0; attempt < 3; attempt++) {
            if (attempt > 0) {
                pause(attempt * 4000L);
            }
            ProfileResult r = mc.getApiServices().sessionService().fetchProfile(id, true);
            if (r != null && r.profile() != null && r.profile().properties().containsKey("textures")) {
                return Optional.of(r.profile());
            }
        }
        throw new Busy();
    }

    private static final java.net.http.HttpClient HTTP = java.net.http.HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(8)).build();

    /**
     * Name to UUID with Mojang's public profile API. Minecraft's own lookup reports a rate-limited
     * request as "no such player", so the status code is read here: 404 means the name does not
     * exist, anything else that fails is retried and then reported as busy.
     */
    private static java.util.UUID lookupId(String name) {
        for (int attempt = 0; attempt < 3; attempt++) {
            if (attempt > 0) {
                pause(attempt * 4000L);
            }
            try {
                java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder(
                                java.net.URI.create("https://api.mojang.com/users/profiles/minecraft/" + name))
                        .timeout(java.time.Duration.ofSeconds(10)).GET().build();
                java.net.http.HttpResponse<String> res = HTTP.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() == 404 || res.statusCode() == 204) {
                    throw new NoSuchPlayer();
                }
                if (res.statusCode() == 200) {
                    String raw = com.google.gson.JsonParser.parseString(res.body()).getAsJsonObject().get("id").getAsString();
                    return java.util.UUID.fromString(raw.replaceFirst(
                            "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})", "$1-$2-$3-$4-$5"));
                }
                AquaClient.LOGGER.info("Skin Protect: name lookup returned HTTP {} (attempt {})", res.statusCode(), attempt + 1);
            } catch (NoSuchPlayer e) {
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                AquaClient.LOGGER.info("Skin Protect: name lookup failed (attempt {}): {}", attempt + 1, e.toString());
            }
        }
        throw new Busy();
    }

    private static void pause(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Skin to show for yourself, or null to keep your own. */
    @Nullable
    public SkinTextures override() {
        return isEnabled() ? override : null;
    }
}
