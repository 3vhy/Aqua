package dev.aqua.client.gui;

import dev.aqua.client.Aqua;
import dev.aqua.client.gui.pages.KeybindsPage;
import dev.aqua.client.gui.pages.Page;
import dev.aqua.client.gui.pages.ProfilesPage;
import dev.aqua.client.gui.pages.SettingsPage;
import dev.aqua.client.gui.pages.ThemesPage;
import dev.aqua.client.gui.pages.WardrobePage;
import dev.aqua.client.hud.HudWidget;
import dev.aqua.client.hud.editor.HudEditorScreen;
import dev.aqua.client.hud.widgets.WatermarkWidget;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Anim;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.Icon;
import dev.aqua.client.render.R2D;
import dev.aqua.client.render.font.AquaFont;
import dev.aqua.client.setting.ActionSetting;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.setting.EnumSetting;
import dev.aqua.client.setting.NumberSetting;
import dev.aqua.client.setting.Setting;
import dev.aqua.client.setting.StringSetting;
import dev.aqua.client.theme.Theme;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Aqua's main menu. Layout follows the attached menu screenshot: one floating window, a narrow
 * sidebar with emblem + name, module search, grouped navigation and a large content area with a
 * category title above a scrollable list of wide rounded module rows (name + description left;
 * settings chevron, bind badge and switch right).
 */
public final class AquaMenuScreen extends Screen {
    public enum Nav {
        HUD(Category.HUD), VISUALS(Category.VISUALS), COSMETICS(Category.COSMETICS), QOL(Category.QOL), PERFORMANCE(Category.PERFORMANCE),
        STARRED(null), WARDROBE(null), MUSIC(null), THEMES(null), PROFILES(null), KEYBINDS(null), SETTINGS(null);

        @Nullable final Category category;

        Nav(@Nullable Category c) {
            this.category = c;
        }
    }

    private static Nav lastNav = Nav.HUD;
    private static final Set<String> expanded = new HashSet<>();

    @Nullable private final Screen parent;
    private final Ui ui = new Ui();
    private Nav nav = lastNav;
    private String query = "";
    private final Anim open = new Anim(0f, 180f);
    private final Anim scroll = new Anim(0f, 140f);
    private float scrollTarget;
    private float contentHeight;
    private boolean closing;
    private final Anim navIndicator = new Anim(0f, 160f);
    private final Anim pageFade = new Anim(1f, 160f);

    private final Page wardrobe = new WardrobePage();
    private final Page music = new dev.aqua.client.gui.pages.MusicPage();
    private final Page themes = new ThemesPage();
    private final Page profiles = new ProfilesPage();
    private final Page keybinds = new KeybindsPage();
    private final Page settings = new SettingsPage();

    // Window geometry in menu space
    private float wx, wy, ww, wh;
    private float scale = 1f;

    public AquaMenuScreen(@Nullable Screen parent) {
        super(Text.literal("Aqua"));
        this.parent = parent;
        open.target(1f);
        navIndicator.snap(nav.ordinal());
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        if (closing) {
            return;
        }
        closing = true;
        open.target(0f);
        Aqua.config().save();
        if (Aqua.ui().animationScale() <= 0.01f) {
            finishClose();
        }
    }

    private void finishClose() {
        client.setScreen(parent);
    }

    private void setNav(Nav n) {
        if (n == nav && query.isEmpty()) {
            return;
        }
        nav = n;
        lastNav = n;
        query = "";
        ui.setText("search", "");
        scrollTarget = 0;
        scroll.snap(0);
        navIndicator.target(n.ordinal());
        pageFade.snap(0f).target(1f);
        UiSounds.click(1.05f);
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (client.world == null) {
            super.renderBackground(ctx, mouseX, mouseY, delta);
            return;
        }
        float a = open.get();
        if (Aqua.ui().blur.on() && client.options.getMenuBackgroundBlurrinessValue() > 0) {
            ctx.applyBlur();
        }
        if (Aqua.ui().menuDim.on()) {
            ctx.fill(0, 0, width, height, Colors.withAlpha(0x05080B, Math.round(110 * a)));
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        float a = open.get();
        if (closing && a < 0.02f) {
            finishClose();
            return;
        }
        Theme t = Aqua.theme();
        scale = (float) Aqua.ui().menuScale.d();
        float sw = width / scale;
        float sh = height / scale;
        ww = Math.min(sw - 20, 440);
        wh = Math.min(sh - 20, 290);
        wx = (sw - ww) / 2f;
        wy = (sh - wh) / 2f;

        float appear = Anim.easeOutCubic(a);
        float s = scale * (0.965f + 0.035f * appear);
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(width / 2f, height / 2f);
        ctx.getMatrices().scale(s, s);
        ctx.getMatrices().translate(-sw / 2f, -sh / 2f + (1f - appear) * 6f);
        float lmx = (mouseX - width / 2f) / s + sw / 2f;
        float lmy = (mouseY - height / 2f) / s + sh / 2f - (1f - appear) * 6f;
        ui.begin(ctx, lmx, lmy);

        // Window
        R2D.shadow(ctx, wx, wy, ww, wh, 10, 22, Colors.withAlpha(0x000000, Math.round(150 * a)));
        R2D.round(ctx, wx, wy, ww, wh, 10, Colors.fade(t.windowBg(0.9f), a));
        R2D.outline(ctx, wx, wy, ww, wh, 10, 0.7f, Colors.fade(t.border(), a));

        float sideW = 96;
        sidebar(ctx, t, wx, wy, sideW, wh, a);
        float cx = wx + sideW;
        content(ctx, t, cx, wy, ww - sideW, wh, a);

        ui.drawTooltip(sw, sh);
        ui.end();
        ctx.getMatrices().popMatrix();
    }

    // ------------------------------------------------------------------ sidebar

    private void sidebar(DrawContext ctx, Theme t, float x, float y, float w, float h, float a) {
        R2D.roundCorners(ctx, x, y, w, h, 10, R2D.LEFT, Colors.fade(Colors.withAlpha(t.panel(), 0.55f), a));
        R2D.rect(ctx, x + w - 0.5f, y + 8, 0.5f, h - 16, Colors.fade(t.divider(), a));
        // Brand
        R2D.texture(ctx, WatermarkWidget.EMBLEM, x + 10, y + 10, 13, 13, Colors.withAlpha(0xFFFFFF, Math.round(255 * a)));
        R2D.textMid(ctx, Aqua.NAME, x + 27, y + 16.5f, 9.5f, AquaFont.Weight.SEMIBOLD, Colors.fade(t.text(), a));
        // Search
        String before = query;
        query = ui.textField("search", x + 8, y + 30, w - 16, 13, query, "Search modules...", 40, Icon.SEARCH,
                q -> {
                }, null);
        if (!query.equals(before)) {
            scrollTarget = 0;
        }
        float iy = y + 50;
        // Fit the navigation to the window height (small windows / large GUI scales).
        float footer = 22;
        float avail = y + h - iy - 6;
        boolean showFooter = avail - footer >= 12 * 13f + 22;
        if (showFooter) {
            avail -= footer;
        }
        itemH = Math.max(10f, Math.min(14f, (avail - 22) / 12f - 1.5f));
        iy = sectionLabel(ctx, t, "MODULES", x + 10, iy, a);
        for (Nav n : new Nav[]{Nav.HUD, Nav.VISUALS, Nav.COSMETICS, Nav.QOL, Nav.PERFORMANCE}) {
            iy = navItem(ctx, t, n, n.category.title, n.category.icon, x, iy, w, a);
        }
        iy = navItem(ctx, t, Nav.STARRED, "Starred", Icon.STAR, x, iy, w, a);
        iy += 3;
        iy = sectionLabel(ctx, t, "CLIENT", x + 10, iy, a);
        iy = navItem(ctx, t, Nav.WARDROBE, "Wardrobe", Icon.HANGER, x, iy, w, a);
        iy = navItem(ctx, t, Nav.MUSIC, "Music", Icon.NOTE, x, iy, w, a);
        iy = navItem(ctx, t, Nav.THEMES, "Themes", Icon.PALETTE, x, iy, w, a);
        iy = navItem(ctx, t, Nav.PROFILES, "Profiles", Icon.FOLDER, x, iy, w, a);
        iy = navItem(ctx, t, Nav.KEYBINDS, "Keybinds", Icon.KEYBOARD, x, iy, w, a);
        navItem(ctx, t, Nav.SETTINGS, "Settings", Icon.GEAR, x, iy, w, a);
        if (showFooter) {
            R2D.textMid(ctx, AquaFont.ellipsize(Aqua.config().activeProfile(), w - 20, 4.8f, AquaFont.Weight.MEDIUM), x + 10, y + h - 15,
                    4.8f, AquaFont.Weight.MEDIUM, Colors.fade(t.muted(), a));
            R2D.textMid(ctx, "v" + Aqua.VERSION + " • 1.21.11", x + 10, y + h - 8.5f, 4.4f, AquaFont.Weight.MEDIUM,
                    Colors.fade(Colors.withAlpha(t.muted(), 0.7f), a));
        }
    }

    private float sectionLabel(DrawContext ctx, Theme t, String label, float x, float y, float a) {
        R2D.textMid(ctx, label, x, y + 4, 4.4f, AquaFont.Weight.BOLD, Colors.fade(Colors.withAlpha(t.muted(), 0.75f), a));
        return y + 9;
    }

    private float itemH = 14;

    private float navItem(DrawContext ctx, Theme t, Nav n, String label, Icon icon, float x, float y, float w, float a) {
        float ih = itemH;
        float ix = x + 6;
        float iw = w - 12;
        boolean sel = n == nav && query.isEmpty();
        boolean hov = ui.hover(ix, y, iw, ih);
        float ha = ui.anim("nav" + n, 120, 0).target(hov).get();
        float sa = ui.anim("navsel" + n, 160, sel ? 1 : 0).target(sel).get();
        if (sa > 0.01f || ha > 0.01f) {
            int bg = Colors.lerp(Colors.withAlpha(t.elevated(), 0.55f * ha), Colors.withAlpha(Colors.lerp(t.elevated(), t.accent(), 0.18f), 0.95f), sa);
            R2D.round(ctx, ix, y, iw, ih, 4.5f, Colors.fade(bg, a));
            if (sa > 0.01f) {
                R2D.outline(ctx, ix, y, iw, ih, 4.5f, 0.6f, Colors.fade(Colors.withAlpha(t.accent(), 0.45f * sa), a));
            }
        }
        int fg = Colors.lerp(t.muted(), t.text(), Math.max(sa, ha * 0.6f));
        R2D.icon(ctx, icon, ix + 5, y + ih / 2f - 3.5f, 7, Colors.fade(sel ? t.accent() : fg, a));
        R2D.textMid(ctx, label, ix + 16, y + ih / 2f, 5.6f, sel ? AquaFont.Weight.SEMIBOLD : AquaFont.Weight.MEDIUM, Colors.fade(fg, a));
        if (ui.clickedIn(ix, y, iw, ih)) {
            setNav(n);
        }
        return y + ih + 1.5f;
    }

    // ------------------------------------------------------------------ content

    private void content(DrawContext ctx, Theme t, float x, float y, float w, float h, float a) {
        float pad = 12;
        float pa = a * Anim.easeOutCubic(pageFade.get());
        String title;
        String subtitle;
        boolean searching = !query.isBlank();
        if (searching) {
            title = "Search";
            subtitle = "Results for “" + query.trim() + "”";
        } else if (nav.category != null) {
            title = nav.category.title;
            subtitle = nav.category.subtitle;
        } else {
            title = switch (nav) {
                case STARRED -> "Starred";
                case WARDROBE -> "Wardrobe";
                case MUSIC -> "Music";
                case THEMES -> "Themes";
                case PROFILES -> "Profiles";
                case KEYBINDS -> "Keybinds";
                default -> "Settings";
            };
            subtitle = switch (nav) {
                case STARRED -> "Your favourite modules (click the star on any module)";
                case WARDROBE -> "3D skins, knives and wearables, previewed live";
                case MUSIC -> "Your music and Spotify, with synced lyrics";
                case THEMES -> "Colours used across Aqua";
                case PROFILES -> "Separate setups with their own HUD layout";
                case KEYBINDS -> "Every Aqua binding in one place";
                default -> "Interface, motion and effect quality";
            };
        }
        R2D.text(ctx, title, x + pad, y + 10, 11f, AquaFont.Weight.SEMIBOLD, Colors.fade(t.text(), pa));
        R2D.text(ctx, subtitle, x + pad, y + 24, 5.2f, AquaFont.Weight.MEDIUM, Colors.fade(t.muted(), pa));
        if (ui.iconButton("close", x + w - 20, y + 8, 12, Icon.CLOSE, t.muted(), "Close (Esc)")) {
            close();
        }

        float listY = y + 36;
        float listH = h - 36 - 8 - (mods(searching) ? 12 : 0);
        List<Module> mods = null;
        if (searching) {
            mods = Aqua.modules().search(query);
        } else if (nav.category != null) {
            mods = Aqua.modules().inCategory(nav.category);
        } else if (nav == Nav.STARRED) {
            mods = new java.util.ArrayList<>();
            for (Module m : Aqua.modules().all()) {
                if (Aqua.ui().isStarred(m.id())) {
                    mods.add(m);
                }
            }
        }
        if (mods != null) {
            String count = mods.size() + (mods.size() == 1 ? " module" : " modules");
            float rx = x + w - 26;
            rx -= R2D.width(count, 5f, AquaFont.Weight.MEDIUM);
            R2D.text(ctx, count, rx, y + 13, 5f, AquaFont.Weight.MEDIUM, Colors.fade(t.muted(), pa));
            if (nav == Nav.HUD && !searching) {
                if (ui.button("edit_layout", x + w - 26 - 62 - R2D.width(count, 5f, AquaFont.Weight.MEDIUM) - 6, y + 9, 60, 12,
                        "Edit layout", Icon.MOVE, true)) {
                    client.setScreen(new HudEditorScreen(this));
                    return;
                }
                hudGlobalBar(ctx, t, x + pad, listY, w - pad * 2 - 4, pa);
                listY += 20;
                listH -= 20;
            }
        }

        float scrolled = scroll.target(scrollTarget).get();
        ui.pushClip(x + 4, listY - 2, w - 8, listH + 4);
        float cy = listY - scrolled;
        if (mods != null) {
            if (mods.isEmpty()) {
                R2D.textCentered(ctx, nav == Nav.STARRED && !searching ? "No starred modules yet: click the star on a module"
                        : "No modules match your search", x + w / 2f, listY + 30, 6f, AquaFont.Weight.MEDIUM, t.muted());
            }
            for (Module m : mods) {
                cy = moduleRow(ctx, t, m, x + pad, cy, w - pad * 2 - 4, pa, listY, listY + listH) + 4;
            }
        } else {
            Page page = switch (nav) {
                case WARDROBE -> wardrobe;
                case MUSIC -> music;
                case THEMES -> themes;
                case PROFILES -> profiles;
                case KEYBINDS -> keybinds;
                default -> settings;
            };
            cy = page.render(ui, ctx, x + pad, cy, w - pad * 2 - 4, pa, this);
        }
        contentHeight = cy + scrolled - listY;
        ui.popClip();
        if (mods != null) {
            legend(ctx, t, x + pad, y + h - 14, pa);
        }

        // Scroll handling + scrollbar
        float max = Math.max(0, contentHeight - listH);
        if (ui.scroll != 0 && ui.hover(x, listY, w, listH)) {
            scrollTarget -= ui.scroll * 22;
            ui.scroll = 0;
        }
        scrollTarget = Math.max(0, Math.min(max, scrollTarget));
        if (max > 0) {
            float trackH = listH;
            float thumbH = Math.max(14, trackH * listH / contentHeight);
            float ty = listY + (trackH - thumbH) * (scrolled / max);
            R2D.round(ctx, x + w - 6, ty, 2, thumbH, 1, Colors.fade(Colors.withAlpha(t.text(), 0.25f), pa));
        }
    }

    private boolean mods(boolean searching) {
        return searching || nav.category != null || nav == Nav.STARRED;
    }

    /** Key legend under module lists (R7): how to toggle, open settings, bind and search. */
    private void legend(DrawContext ctx, Theme t, float x, float y, float a) {
        String[][] items = {{"Click", "Settings"}, {"Right-click", "Toggle"}, {"Middle-click", "Bind key"}, {"Type", "Search"}};
        float cx = x;
        for (String[] it : items) {
            float kw = R2D.width(it[0], 4.4f, AquaFont.Weight.SEMIBOLD) + 6;
            R2D.round(ctx, cx, y, kw, 8, 2.5f, Colors.fade(Colors.withAlpha(t.panel(), 0.9f), a));
            R2D.outline(ctx, cx, y, kw, 8, 2.5f, 0.5f, Colors.fade(Colors.withAlpha(t.border(), 0.6f), a));
            R2D.textMid(ctx, it[0], cx + 3, y + 4, 4.4f, AquaFont.Weight.SEMIBOLD, Colors.fade(t.text(), a));
            cx += kw + 3;
            R2D.textMid(ctx, it[1], cx, y + 4, 4.4f, AquaFont.Weight.MEDIUM, Colors.fade(t.muted(), a));
            cx += R2D.width(it[1], 4.4f, AquaFont.Weight.MEDIUM) + 9;
        }
    }

    private void hudGlobalBar(DrawContext ctx, Theme t, float x, float y, float w, float a) {
        R2D.round(ctx, x, y, w, 16, 6, Colors.fade(Colors.withAlpha(t.panel(), 0.85f), a));
        R2D.textMid(ctx, "HUD scale", x + 8, y + 8, 5.4f, AquaFont.Weight.MEDIUM, Colors.fade(t.text(), a));
        ui.slider("hud_scale", x + 50, y + 3.5f, w * 0.35f, Aqua.hud().scale);
        R2D.textMid(ctx, Aqua.hud().scale.format(), x + 56 + w * 0.35f, y + 8, 5f, AquaFont.Weight.MEDIUM, Colors.fade(t.muted(), a));
        float bx = x + w - 26;
        R2D.textRight(ctx, "Element backgrounds", bx - 4, y + 8, 5.4f, AquaFont.Weight.MEDIUM, Colors.fade(t.text(), a));
        boolean nb = ui.toggle("hud_bg", bx, y + 3.25f, Aqua.hud().backgrounds.on());
        if (nb != Aqua.hud().backgrounds.on()) {
            Aqua.hud().backgrounds.set(nb);
        }
    }

    // ------------------------------------------------------------------ module rows

    private float moduleRow(DrawContext ctx, Theme t, Module m, float x, float y, float w, float a, float clipTop, float clipBottom) {
        float rowH = 26;
        boolean isOpen = expanded.contains(m.id());
        float ex = ui.anim("exp" + m.id(), 180, isOpen ? 1 : 0).target(isOpen).get();
        float settingsH = settingsHeight(m);
        float totalH = rowH + settingsH * Anim.easeInOutCubic(ex);
        if (y + totalH < clipTop - 4 || y > clipBottom + 4) {
            return y + totalH;
        }
        boolean hov = ui.hover(x, y, w, rowH);
        float ha = ui.anim("row" + m.id(), 120, 0).target(hov).get();
        int bg = Colors.lerp(Colors.withAlpha(t.panel(), 0.92f), Colors.withAlpha(Colors.brighten(t.panel(), 0.07f), 0.96f), ha);
        R2D.round(ctx, x, y, w, totalH, 6.5f, Colors.fade(bg, a));
        R2D.outline(ctx, x, y, w, totalH, 6.5f, 0.6f, Colors.fade(Colors.withAlpha(t.border(), 0.35f + ha * 0.25f), a));

        // Text
        R2D.text(ctx, m.name(), x + 9, y + 5.5f, 6.4f, AquaFont.Weight.SEMIBOLD, Colors.fade(t.text(), a));
        String desc = AquaFont.ellipsize(m.description(), w - 100, 4.9f, AquaFont.Weight.REGULAR);
        R2D.text(ctx, desc, x + 9, y + 15, 4.9f, AquaFont.Weight.REGULAR, Colors.fade(t.muted(), a));

        // Right controls: chevron, bind badge, toggle
        float tx = x + w - 26;
        boolean on = ui.toggle("tg" + m.id(), tx, y + rowH / 2f - 4.75f, m.isEnabled());
        if (on != m.isEnabled()) {
            m.setEnabled(on, false);
        }
        float bw = Math.max(20, Math.min(46, R2D.width(m.bind().display(), 4.8f, AquaFont.Weight.SEMIBOLD) + 8));
        float bx = tx - bw - 6;
        boolean conflict = !Aqua.keybinds().conflicts(m.bind(), m).isEmpty();
        ui.keybindBadge("kb" + m.id(), bx, y + rowH / 2f - 5, bw, m.bind().display(), m.bind().isBound(), conflict, c -> {
            if (c.cancelled()) {
                return;
            }
            if (c.cleared()) {
                m.bind().clear();
            } else {
                m.bind().set(c.type(), c.code());
                List<String> conf = Aqua.keybinds().conflicts(m.bind(), m);
                if (!conf.isEmpty()) {
                    Aqua.notifications().warn("Key conflict", m.bind().display() + " is also used by " + String.join(", ", conf));
                }
            }
            Aqua.config().markDirty();
        }, () -> {
            m.bind().clear();
            Aqua.config().markDirty();
        });
        float chx = bx - 13;
        boolean starred = Aqua.ui().isStarred(m.id());
        float sx = chx - 12;
        boolean starHover = ui.hover(sx - 1, y + rowH / 2f - 4.5f, 9, 9);
        if (starred || ha > 0.05f) {
            R2D.icon(ctx, Icon.STAR, sx, y + rowH / 2f - 3.5f, 7,
                    Colors.fade(starred ? 0xFFFFC94D : Colors.lerp(t.muted(), t.text(), starHover ? 1f : 0f), a * (starred ? 1f : ha)));
        }
        if (ui.clickedIn(sx - 1, y + rowH / 2f - 4.5f, 9, 9)) {
            Aqua.ui().toggleStar(m.id());
            UiSounds.click(starred ? 0.9f : 1.2f);
        }
        if (ui.middleClickedIn(x, y, w, rowH)) {
            ui.startCapture("kb" + m.id(), c -> {
                if (c.cancelled()) {
                    return;
                }
                if (c.cleared()) {
                    m.bind().clear();
                } else {
                    m.bind().set(c.type(), c.code());
                }
                Aqua.config().markDirty();
            });
        }
        boolean hasSettings = settingsH > 0;
        if (hasSettings) {
            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().translate(chx + 4, y + rowH / 2f);
            ctx.getMatrices().rotate((float) (Math.PI / 2 * Anim.easeInOutCubic(ex)));
            R2D.icon(ctx, Icon.CHEVRON_RIGHT, -3.5f, -3.5f, 7, Colors.fade(Colors.lerp(t.muted(), t.text(), ha), a));
            ctx.getMatrices().popMatrix();
        }
        // Clicking the row (outside controls) expands settings
        if (hasSettings && ui.clickedIn(x, y, sx - 2 - x, rowH)) {
            if (isOpen) {
                expanded.remove(m.id());
            } else {
                expanded.add(m.id());
            }
            UiSounds.click(isOpen ? 0.9f : 1.1f);
        }
        if (ui.rightClickedIn(x, y, w, rowH)) {
            m.toggle();
        }

        // Settings
        if (ex > 0.01f && hasSettings) {
            float sy = y + rowH;
            float visibleH = settingsH * Anim.easeInOutCubic(ex);
            ui.pushClip(x, sy, w, visibleH);
            R2D.rect(ctx, x + 9, sy, w - 18, 0.5f, Colors.fade(t.divider(), a));
            float ry = sy + 4;
            for (Setting<?> s : m.settings()) {
                if (!s.isVisible()) {
                    continue;
                }
                settingRow(ctx, t, m.id(), s, x + 9, ry, w - 18, a * ex);
                ry += 14;
            }
            // Footer: per-module reset (+ HUD editor shortcut for widgets)
            if (ui.button("rst" + m.id(), x + w - 9 - 58, ry + 1, 58, 11, "Reset module", Icon.RESET, false)) {
                m.resetToDefaults();
                Aqua.notifications().info(m.name(), "Settings reset to defaults");
            }
            if (m instanceof HudWidget) {
                if (ui.button("edit" + m.id(), x + w - 9 - 58 - 62, ry + 1, 58, 11, "Position", Icon.MOVE, false)) {
                    client.setScreen(new HudEditorScreen(this));
                }
            }
            ui.popClip();
        }
        return y + totalH;
    }

    private static float settingsHeight(Module m) {
        int n = 0;
        for (Setting<?> s : m.settings()) {
            if (s.isVisible()) {
                n++;
            }
        }
        return n == 0 && !(m instanceof HudWidget) ? 0 : 4 + n * 14 + 16;
    }

    /** Draws one setting with the matching control. Public for pages that reuse it. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void settingRow(DrawContext ctx, Theme t, String owner, Setting<?> s, float x, float y, float w, float a) {
        String id = owner + "." + s.id();
        boolean hovLabel = ui.hover(x, y, w * 0.5f, 12);
        R2D.textMid(ctx, s.name(), x, y + 6, 5.5f, AquaFont.Weight.MEDIUM, Colors.fade(t.text(), a));
        if (hovLabel && !s.description().isEmpty()) {
            ui.tooltip(s.description());
        }
        float right = x + w;
        if (s instanceof BoolSetting b) {
            boolean nv = ui.toggle(id, right - 17, y + 1.25f, b.on());
            if (nv != b.on()) {
                b.set(nv);
            }
        } else if (s instanceof NumberSetting n) {
            float sw = Math.min(90, w * 0.38f);
            R2D.textRight(ctx, n.format(), right, y + 6, 5f, AquaFont.Weight.MEDIUM, Colors.fade(t.muted(), a));
            ui.slider(id, right - sw - 30, y + 1.5f, sw, n);
        } else if (s instanceof EnumSetting e) {
            ui.dropdown(id, right - 86, y + 0.5f, 86, e);
        } else if (s instanceof ColorSetting c) {
            ui.colorButton(id, right - 22, y + 0.5f, c);
        } else if (s instanceof StringSetting str) {
            float fw = Math.min(150, w * 0.55f);
            ui.textField(id, right - fw, y, fw, 12, str.get(), "", str.maxLength(), null, str::set, str::set);
        } else if (s instanceof ActionSetting act) {
            float bw = Math.max(44, R2D.width(act.label(), 5.6f, AquaFont.Weight.SEMIBOLD) + 14);
            if (ui.button(id, right - bw, y + 0.5f, bw, 11, act.label(), null, false)) {
                act.run();
            }
        }
    }

    public Ui ui() {
        return ui;
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        ui.onClick(click.button());
        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        ui.onRelease(click.button());
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        ui.onScroll(vertical);
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        // Escape order: popup -> key capture -> search (clear) -> other text field -> close menu.
        if (input.key() == GLFW.GLFW_KEY_ESCAPE && ui.popup == null && ui.capturing == null
                && ("search".equals(ui.focus) || (ui.focus == null && !query.isEmpty()))) {
            ui.focus = null;
            if (!query.isEmpty()) {
                query = "";
                ui.setText("search", "");
                scrollTarget = 0;
                return true;
            }
            close();
            return true;
        }
        if (ui.onKey(input)) {
            return true;
        }
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (!query.isEmpty()) {
                query = "";
                ui.setText("search", "");
                return true;
            }
            close();
            return true;
        }
        KeyBinding menu = Aqua.keybinds().menu;
        if (ui.focus == null && menu.matchesKey(input)) {
            close();
            return true;
        }
        // Typing anywhere focuses search (when no other field is focused).
        return true;
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (ui.focus == null && ui.popup == null && ui.capturing == null && input.isValidChar() && input.codepoint() != ' ') {
            ui.focus = "search";
        }
        ui.onChar(input);
        return true;
    }

    public static boolean isMouseBind(InputUtil.Type t) {
        return t == InputUtil.Type.MOUSE;
    }
}
