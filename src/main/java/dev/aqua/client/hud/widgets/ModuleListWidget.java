package dev.aqua.client.hud.widgets;

import dev.aqua.client.Aqua;
import dev.aqua.client.hud.Anchor;
import dev.aqua.client.hud.HudWidget;
import dev.aqua.client.hud.Pill;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Anim;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.Icon;
import dev.aqua.client.render.R2D;
import dev.aqua.client.render.font.AquaFont;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.EnumSetting;
import dev.aqua.client.theme.Theme;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Enabled cosmetic / visual / QoL modules as right-aligned capsules sorted longest first
 * (Fern R1 0:01, R2 0:03). Only Aqua's own non-HUD modules are listed.
 */
public final class ModuleListWidget extends HudWidget {
    public enum Case implements EnumSetting.Displayable {
        NORMAL("Normal"), UPPER("Uppercase"), LOWER("Lowercase");
        final String l;

        Case(String l) {
            this.l = l;
        }

        public String displayName() {
            return l;
        }
    }

    public enum Sort implements EnumSetting.Displayable {
        LENGTH("Longest first"), ALPHABETICAL("Alphabetical");
        final String l;

        Sort(String l) {
            this.l = l;
        }

        public String displayName() {
            return l;
        }
    }

    private final EnumSetting<Sort> sort = add(new EnumSetting<>("sort", "Sort", Sort.LENGTH));
    private final EnumSetting<Case> textCase = add(new EnumSetting<>("case", "Text case", Case.NORMAL));
    private final BoolSetting accentBar = add(new BoolSetting("accent_bar", "Accent bar", false).describe("Thin accent line on the outer edge"));
    private final BoolSetting accentText = add(new BoolSetting("accent_text", "Accent text", false));
    private final BoolSetting includeQol = add(new BoolSetting("include_qol", "Include quality-of-life modules", true));

    private final Map<Module, Anim> anims = new HashMap<>();
    private static final float ROW = 11.5f;
    private static final float H = 10f;

    public ModuleListWidget() {
        super("module_list", "Module List", "Your enabled Aqua cosmetic and visual modules.", Icon.LIST, true, Anchor.TOP_RIGHT, -4, 4,
                "arraylist", "array list", "enabled modules");
    }

    private String label(Module m) {
        return switch (textCase.get()) {
            case UPPER -> m.name().toUpperCase(Locale.ROOT);
            case LOWER -> m.name().toLowerCase(Locale.ROOT);
            default -> m.name();
        };
    }

    private boolean eligible(Module m) {
        if (!m.showInModuleList() || m.category() == Category.HUD) {
            return false;
        }
        return includeQol.on() || m.category() != Category.QOL;
    }

    private List<Module> visible(boolean preview) {
        List<Module> out = new ArrayList<>();
        for (Module m : Aqua.modules().all()) {
            if (!eligible(m)) {
                continue;
            }
            Anim a = anims.computeIfAbsent(m, k -> new Anim(m.isEnabled() ? 1f : 0f, 200f));
            a.target(m.isEnabled());
            if (a.get() > 0.01f || (preview && out.size() < 4)) {
                out.add(m);
            }
        }
        if (sort.is(Sort.LENGTH)) {
            out.sort((a, b) -> Float.compare(rowWidth(b), rowWidth(a)));
        } else {
            out.sort((a, b) -> label(a).compareToIgnoreCase(label(b)));
        }
        return out;
    }

    private float rowWidth(Module m) {
        return AquaFont.width(label(m), 5.8f, AquaFont.Weight.SEMIBOLD) + 10;
    }

    @Override
    public boolean hasContent() {
        return !visible(false).isEmpty();
    }

    @Override
    public float width(boolean preview) {
        float w = 40;
        for (Module m : visible(preview)) {
            w = Math.max(w, rowWidth(m));
        }
        return w;
    }

    @Override
    public float height(boolean preview) {
        float h = 0;
        for (Module m : visible(preview)) {
            float a = preview && !m.isEnabled() ? 1f : anims.get(m).get();
            h += ROW * a;
        }
        return Math.max(H, h);
    }

    @Override
    public void draw(DrawContext ctx, boolean preview, float alpha) {
        Theme t = Aqua.theme();
        float w = width(preview);
        boolean right = anchor().fx >= 0.5f;
        float y = 0;
        for (Module m : visible(preview)) {
            float a = preview && !m.isEnabled() ? 1f : anims.get(m).get();
            float rw = rowWidth(m);
            float slide = (1f - Anim.easeOutCubic(a)) * (rw + 6);
            float x = right ? w - rw + slide : -slide;
            float ra = alpha * a;
            if (showBackground()) {
                R2D.round(ctx, x, y, rw, H, Pill.RADIUS * 0.85f, bg(ra));
            }
            if (accentBar.on()) {
                float bx = right ? x + rw - 1.4f : x;
                R2D.roundCorners(ctx, bx, y + 1.5f, 1.4f, H - 3, 0.7f, R2D.ALL, Colors.fade(t.accent(), ra));
            }
            int c = accentText.on() ? t.accent() : t.text();
            R2D.textMid(ctx, label(m), x + 5, y + H / 2f, 5.8f, AquaFont.Weight.SEMIBOLD, Colors.fade(c, ra));
            y += ROW * a;
        }
    }
}
