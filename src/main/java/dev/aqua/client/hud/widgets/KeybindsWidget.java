package dev.aqua.client.hud.widgets;

import dev.aqua.client.Aqua;
import dev.aqua.client.hud.Anchor;
import dev.aqua.client.hud.HudWidget;
import dev.aqua.client.hud.Panel;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.Icon;
import dev.aqua.client.render.R2D;
import dev.aqua.client.render.font.AquaFont;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.theme.Theme;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/** "Key Binds" panel listing Aqua modules that have a key (Fern R1 0:00). */
public final class KeybindsWidget extends HudWidget {
    private final BoolSetting highlightEnabled = add(new BoolSetting("highlight", "Highlight enabled modules", true));

    public KeybindsWidget() {
        super("keybinds", "Hotkeys", "Lists your bound Aqua modules and their keys.", Icon.COMMAND, true, Anchor.TOP_LEFT, 5, 37,
                "binds", "hotkeys", "key binds");
    }

    private List<Module> rows(boolean preview) {
        List<Module> out = new ArrayList<>();
        for (Module m : Aqua.modules().all()) {
            if (m.bind().isBound()) {
                out.add(m);
            }
        }
        out.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        return out;
    }

    @Override
    public boolean hasContent() {
        return !rows(false).isEmpty();
    }

    @Override
    public float width(boolean preview) {
        float w = AquaFont.width("Key Binds", Panel.TITLE, AquaFont.Weight.SEMIBOLD) + 24;
        for (Module m : rows(preview)) {
            w = Math.max(w, AquaFont.width(m.name(), Panel.TEXT, AquaFont.Weight.MEDIUM)
                    + AquaFont.width(m.bind().display(), Panel.TEXT, AquaFont.Weight.MEDIUM) + Panel.PAD * 2 + 14);
        }
        return Math.max(88, w);
    }

    @Override
    public float height(boolean preview) {
        List<Module> r = rows(preview);
        return Panel.height(r.isEmpty() ? 1 : r.size());
    }

    @Override
    public void draw(DrawContext ctx, boolean preview, float alpha) {
        Theme t = Aqua.theme();
        List<Module> r = rows(preview);
        float w = width(preview);
        Panel.header(ctx, w, "Key Binds", Icon.COMMAND, showBackground(), bg(alpha), alpha, r.isEmpty() ? 1 : r.size());
        if (r.isEmpty()) {
            R2D.textMid(ctx, "No binds yet", Panel.PAD, Panel.rowY(0), Panel.TEXT, AquaFont.Weight.MEDIUM, Colors.fade(t.muted(), alpha));
            return;
        }
        for (int i = 0; i < r.size(); i++) {
            Module m = r.get(i);
            float y = Panel.rowY(i);
            boolean dim = highlightEnabled.on() && !m.isEnabled();
            int c = dim ? t.muted() : t.text();
            R2D.textMid(ctx, m.name(), Panel.PAD, y, Panel.TEXT, AquaFont.Weight.MEDIUM, Colors.fade(c, alpha));
            R2D.textRight(ctx, m.bind().display(), w - Panel.PAD, y, Panel.TEXT, AquaFont.Weight.MEDIUM, Colors.fade(c, alpha * 0.9f));
        }
    }
}
