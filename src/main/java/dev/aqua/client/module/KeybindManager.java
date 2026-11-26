package dev.aqua.client.module;

import com.google.gson.JsonObject;
import dev.aqua.client.Aqua;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Vanilla-visible key bindings for Aqua actions plus dispatch of module toggle binds.
 * Module binds fire only in-game (no screen open) on key press.
 */
public final class KeybindManager {
    public static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of("aqua", "aqua"));

    public final KeyBinding menu = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.aqua.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, CATEGORY));
    public final KeyBinding zoom = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.aqua.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY));
    public final KeyBinding emoteWheel = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.aqua.emote_wheel", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY));
    public final KeyBinding inspect = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.aqua.inspect", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Y, CATEGORY));
    public final KeyBinding freelook = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.aqua.freelook", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, CATEGORY));
    public final KeyBinding hudEditor = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.aqua.hud_editor", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY));
    public final KeyBinding musicToggle = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.aqua.music_toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY));
    public final KeyBinding musicNext = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.aqua.music_next", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY));

    public List<KeyBinding> aquaVanillaBinds() {
        return List.of(menu, zoom, freelook, emoteWheel, inspect, hudEditor, musicToggle, musicNext);
    }

    /** Called from the keyboard/mouse mixins. Returns true when an Aqua module consumed the input. */
    public boolean onInput(InputUtil.Type type, int code, boolean press) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!press || mc.currentScreen != null || mc.player == null || code == Keybind.NONE) {
            return false;
        }
        boolean any = false;
        for (Module m : Aqua.modules().all()) {
            if (m.bind().matches(type, code)) {
                m.toggle();
                any = true;
            }
            if (m.isEnabled()) {
                for (dev.aqua.client.setting.Setting<?> s : m.settings()) {
                    if (s instanceof dev.aqua.client.setting.KeySetting k && k.matches(type, code)) {
                        m.onKeySetting(k);
                        any = true;
                    }
                }
            }
        }
        return any;
    }

    /** Describes every other binding using the same key (Aqua modules and vanilla/mod key bindings). */
    public List<String> conflicts(Keybind kb, Object self) {
        List<String> out = new ArrayList<>();
        if (kb == null || !kb.isBound()) {
            return out;
        }
        for (Module m : Aqua.modules().all()) {
            if (m != self && m.bind().sameAs(kb)) {
                out.add(m.name());
            }
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options != null) {
            for (KeyBinding k : mc.options.allKeys) {
                if (k == self) {
                    continue;
                }
                InputUtil.Key bound = KeyBindingHelper.getBoundKeyOf(k);
                if (bound != null && bound.getCategory() == kb.type() && bound.getCode() == kb.code()) {
                    out.add(Text.translatable(k.getId()).getString());
                }
            }
        }
        return out;
    }

    /** Conflicts for a vanilla key binding owned by Aqua. */
    public List<String> conflicts(KeyBinding binding) {
        InputUtil.Key bound = KeyBindingHelper.getBoundKeyOf(binding);
        if (bound == null || bound.getCode() == GLFW.GLFW_KEY_UNKNOWN) {
            return new ArrayList<>();
        }
        return conflicts(new Keybind(bound.getCategory(), bound.getCode()), binding);
    }

    public JsonObject menuKeyToJson() {
        JsonObject o = new JsonObject();
        InputUtil.Key k = KeyBindingHelper.getBoundKeyOf(menu);
        o.addProperty("info", "Aqua key bindings are stored in options.txt; this value is informational");
        o.addProperty("menu", k.getTranslationKey());
        return o;
    }

    public void menuKeyFromJson(JsonObject o) {
        // Vanilla options.txt is the source of truth for vanilla-visible bindings.
    }
}
