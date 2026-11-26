package dev.aqua.client.module.qol;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.KeySetting;
import dev.aqua.client.setting.NumberSetting;
import dev.aqua.client.setting.StringSetting;

import java.util.ArrayList;
import java.util.List;

/**
 * Chat Macro (reference video): sends a chat message or command when you press its key. One key
 * press sends one message (with a short cooldown); nothing is sent while a screen is open.
 */
public final class ChatMacro extends Module {
    public static final int SLOTS = 6;
    private final NumberSetting count = add(new NumberSetting("count", "Macros", 1, 1, SLOTS, 1).visibleWhen(() -> false));
    private final StringSetting[] texts = new StringSetting[SLOTS];
    private final KeySetting[] keys = new KeySetting[SLOTS];
    private long lastSent;
    private String lastText = "";
    private int sentCount;

    public ChatMacro() {
        super("chat_macro", "Chat Macro", "Sends chat messages or commands with a key press.", Category.QOL, false,
                "macro", "chat macro", "command", "hotkey", "message");
        for (int i = 0; i < SLOTS; i++) {
            int idx = i;
            texts[i] = add(new StringSetting("text_" + (i + 1), "Macro " + (i + 1), i == 0 ? "/home" : "", 256)
                    .describe("A message, or a command starting with /").visibleWhen(() -> idx < count.d()));
            keys[i] = add(new KeySetting("key_" + (i + 1), "Macro " + (i + 1) + " key").visibleWhen(() -> idx < count.d()));
        }
    }

    /** Shows one more macro slot (Macro panel → New macro). */
    public void addSlot() {
        count.set(Math.min(SLOTS, count.d() + 1));
    }

    /** [text, key] of each macro slot in use (for the Macro panel). */
    public List<String[]> macroRows() {
        List<String[]> l = new ArrayList<>();
        for (int i = 0; i < count.d(); i++) {
            String t = texts[i].get().isBlank() ? "Macro " + (i + 1) : texts[i].get();
            l.add(new String[]{t, keys[i].keybind().isBound() ? keys[i].display().toUpperCase(java.util.Locale.ROOT) : ""});
        }
        return l;
    }

    @Override
    public void onKeySetting(KeySetting key) {
        for (int i = 0; i < SLOTS; i++) {
            if (keys[i] == key && i < count.d()) {
                send(texts[i].get());
            }
        }
    }

    /** Sends a macro's text; returns false if nothing was sent. */
    public boolean send(String text) {
        long now = System.currentTimeMillis();
        if (text.isBlank() || mc.player == null || mc.getNetworkHandler() == null || now - lastSent < 400) {
            return false;
        }
        lastSent = now;
        String t = text.trim();
        lastText = t;
        sentCount++;
        if (t.startsWith("/")) {
            mc.getNetworkHandler().sendChatCommand(t.substring(1));
        } else {
            mc.getNetworkHandler().sendChatMessage(t);
        }
        return true;
    }

    /** The last text sent and how many were sent (used by the in-game tests). */
    public String lastText() {
        return lastText;
    }

    public int sentCount() {
        return sentCount;
    }

    /** Text of a macro slot (used by the in-game tests). */
    public StringSetting text(int i) {
        return texts[i];
    }

    public KeySetting key(int i) {
        return keys[i];
    }
}
