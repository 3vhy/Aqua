package dev.aqua.client.module.qol;

import dev.aqua.client.Aqua;
import dev.aqua.client.AquaClient;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.BoolSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

/** Screenshot notifications with a thumbnail (baseline addition). */
public final class ScreenshotHelper extends Module {
    private final BoolSetting thumbnail = add(new BoolSetting("thumbnail", "Show thumbnail", true));
    private final BoolSetting hideChat = add(new BoolSetting("hide_chat", "Hide vanilla chat message", false));
    private static final Identifier THUMB = Identifier.of("aqua", "dynamic/screenshot_thumb");

    public ScreenshotHelper() {
        super("screenshot_toast", "Screenshot Notifications", "Shows a notification with a preview when you take a screenshot.",
                Category.QOL, true, "screenshot", "f2", "capture");
        hideFromModuleList();
    }

    public boolean hideChatMessage() {
        return isEnabled() && hideChat.on();
    }

    /** Called on the IO thread once the screenshot file has been written. */
    public void onSaved(File file) {
        if (!isEnabled()) {
            return;
        }
        NativeImage thumb = null;
        if (thumbnail.on()) {
            try (InputStream in = Files.newInputStream(file.toPath())) {
                NativeImage full = NativeImage.read(in);
                int w = 256;
                int h = Math.max(1, Math.round(full.getHeight() * (w / (float) full.getWidth())));
                thumb = new NativeImage(w, h, false);
                full.resizeSubRectTo(0, 0, full.getWidth(), full.getHeight(), thumb);
                full.close();
            } catch (Exception e) {
                AquaClient.LOGGER.warn("Could not build screenshot thumbnail", e);
            }
        }
        NativeImage t = thumb;
        MinecraftClient.getInstance().execute(() -> {
            float aspect = 16f / 9f;
            Identifier id = null;
            if (t != null) {
                aspect = t.getWidth() / (float) t.getHeight();
                MinecraftClient.getInstance().getTextureManager().registerTexture(THUMB, new NativeImageBackedTexture(() -> "aqua_thumb", t));
                id = THUMB;
            }
            Aqua.notifications().image("Screenshot saved", file.getName(), id, aspect);
        });
    }
}
