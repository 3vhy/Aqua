package dev.aqua.client.module.qol;

import dev.aqua.client.Aqua;
import dev.aqua.client.AquaClient;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.ActionSetting;
import dev.aqua.client.setting.EnumSetting;
import dev.aqua.client.setting.StringSetting;
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
        super("skin_protect", "Skin Protect", "Shows another player's skin (or a default skin) as yours, on your screen only.",
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
                status = "Looking up " + name + "…";
                CompletableFuture.supplyAsync(() -> mc.getApiServices().profileResolver().getProfileByName(name), Util.getDownloadWorkerExecutor())
                        .thenCompose(profile -> profile.isPresent()
                                ? mc.getSkinProvider().fetchSkinTextures(profile.get())
                                : CompletableFuture.completedFuture(Optional.<SkinTextures>empty()))
                        .whenComplete((skin, error) -> mc.execute(() -> {
                            loading = false;
                            if (error != null || skin == null || skin.isEmpty()) {
                                status = "Could not load the skin of " + name;
                                if (error != null) {
                                    AquaClient.LOGGER.warn("Skin Protect lookup failed for {}", name, error);
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

    /** Skin to show for yourself, or null to keep your own. */
    @Nullable
    public SkinTextures override() {
        return isEnabled() ? override : null;
    }
}
