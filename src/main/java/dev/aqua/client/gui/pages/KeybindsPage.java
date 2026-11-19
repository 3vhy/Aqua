package dev.aqua.client.gui.pages;

import dev.aqua.client.Aqua;
import dev.aqua.client.gui.AquaMenuScreen;
import dev.aqua.client.gui.Ui;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.Icon;
import dev.aqua.client.render.R2D;
import dev.aqua.client.render.font.AquaFont;
import dev.aqua.client.theme.Theme;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

/** All Aqua bindings with capture, clearing and conflict detection. */
public final class KeybindsPage implements Page {
    @Override
    public float render(Ui ui, DrawContext ctx, float x, float y, float w, float a, AquaMenuScreen menu) {
        Theme t = Aqua.theme();
        List<KeyBinding> binds = Aqua.keybinds().aquaVanillaBinds();
        float h = 26 + binds.size() * 15 + 4;
        Page.card(ctx, t, x, y, w, h, a);
        Page.heading(ctx, t, "Actions", "Also listed under Options > Controls > Key Binds > Aqua", x + 9, y + 6, a);
        float ry = y + 26;
        for (KeyBinding kb : binds) {
            InputUtil.Key bound = KeyBindingHelper.getBoundKeyOf(kb);
            boolean isBound = !kb.isUnbound();
            List<String> conf = Aqua.keybinds().conflicts(kb);
            R2D.textMid(ctx, Text.translatable(kb.getId()).getString(), x + 9, ry + 6, 5.5f, AquaFont.Weight.MEDIUM, Colors.fade(t.text(), a));
            if (!conf.isEmpty()) {
                R2D.textRight(ctx, "Also: " + AquaFont.ellipsize(String.join(", ", conf), w * 0.35f, 4.6f, AquaFont.Weight.MEDIUM),
                        x + w - 66, ry + 6, 4.6f, AquaFont.Weight.MEDIUM, Colors.fade(0xFFFFB547, a));
            }
            ui.keybindBadge("kv" + kb.getId(), x + w - 9 - 50, ry + 1, 50, kb.getBoundKeyLocalizedText().getString(), isBound, !conf.isEmpty(),
                    c -> {
                        if (c.cancelled()) {
                            return;
                        }
                        kb.setBoundKey(c.cleared() ? InputUtil.UNKNOWN_KEY : c.type().createFromCode(c.code()));
                        KeyBinding.updateKeysByCode();
                        MinecraftClient.getInstance().options.write();
                    }, () -> {
                        kb.setBoundKey(InputUtil.UNKNOWN_KEY);
                        KeyBinding.updateKeysByCode();
                        MinecraftClient.getInstance().options.write();
                    });
            if (ui.iconButton("kvr" + kb.getId(), x + w - 9 - 50 - 15, ry, 12, Icon.RESET, t.muted(), "Reset to default")) {
                kb.setBoundKey(kb.getDefaultKey());
                KeyBinding.updateKeysByCode();
                MinecraftClient.getInstance().options.write();
            }
            ry += 15;
        }
        y += h + 4;

        List<Module> mods = Aqua.modules().all();
        float mh = 26 + mods.size() * 15 + 4;
        Page.card(ctx, t, x, y, w, mh, a);
        Page.heading(ctx, t, "Module toggles", "Toggle a module in-game with its key. Right-click a badge to clear it.", x + 9, y + 6, a);
        ry = y + 26;
        for (Module m : mods) {
            List<String> conf = Aqua.keybinds().conflicts(m.bind(), m);
            R2D.icon(ctx, m.category().icon, x + 9, ry + 2.5f, 7, Colors.fade(t.muted(), a));
            R2D.textMid(ctx, m.name(), x + 20, ry + 6, 5.5f, AquaFont.Weight.MEDIUM, Colors.fade(t.text(), a));
            if (!conf.isEmpty()) {
                R2D.textRight(ctx, "Also: " + AquaFont.ellipsize(String.join(", ", conf), w * 0.35f, 4.6f, AquaFont.Weight.MEDIUM),
                        x + w - 66, ry + 6, 4.6f, AquaFont.Weight.MEDIUM, Colors.fade(0xFFFFB547, a));
            }
            ui.keybindBadge("km" + m.id(), x + w - 9 - 50, ry + 1, 50, m.bind().display(), m.bind().isBound(), !conf.isEmpty(), c -> {
                if (c.cancelled()) {
                    return;
                }
                if (c.cleared()) {
                    m.bind().clear();
                } else {
                    m.bind().set(c.type(), c.code());
                }
                Aqua.config().markDirty();
            }, () -> {
                m.bind().clear();
                Aqua.config().markDirty();
            });
            ry += 15;
        }
        return y + mh + 4;
    }
}
