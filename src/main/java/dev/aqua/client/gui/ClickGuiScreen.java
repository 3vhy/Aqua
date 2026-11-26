package dev.aqua.client.gui;

import dev.aqua.client.Aqua;
import dev.aqua.client.gui.click.ClickStyle;
import dev.aqua.client.gui.click.MenuPanel;
import dev.aqua.client.gui.pages.FriendsPage;
import dev.aqua.client.gui.pages.KeybindsPage;
import dev.aqua.client.gui.pages.Page;
import dev.aqua.client.gui.pages.ProfilesPage;
import dev.aqua.client.gui.pages.SettingsPage;
import dev.aqua.client.gui.pages.ThemesPage;
import dev.aqua.client.hud.HudWidget;
import dev.aqua.client.hud.editor.HudEditorScreen;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Anim;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.Icon;
import dev.aqua.client.render.R2D;
import dev.aqua.client.render.font.AquaFont;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Aqua's panel menu, laid out like the reference video: one floating panel per category (dark,
 * with a faint grid, modules in alphabetical order, enabled ones highlighted in blue and their key
 * on the right), a Search panel and a Macro panel at the top and a tab bar at the bottom (Modules,
 * HUD, Friends, Theme, Config). Left-click switches a module, right-click opens its settings card,
 * middle-click binds a key. Panels can be dragged by their header and folded with the arrow.
 *
 * <p>Everything is laid out in a 640x360 space that is scaled to the window.
 */
public final class ClickGuiScreen extends Screen implements PageHost {
    public enum Tab {
        MODULES("Modules"), HUD("HUD"), FRIENDS("Friends"), THEME("Theme"), CONFIG("Config");
        public final String label;

        Tab(String l) {
            label = l;
        }
    }

    public static final float CW = 640;
    public static final float CH = 360;
    public static final float PW = 69;
    public static final float HEADER = 11.5f;
    public static final float ROW = 10.65f;
    private static final float TAB_Y = 336;

    /** A panel's place on screen; also used for the Search and Macro panels. */
    static final class PanelState {
        final String id;
        @Nullable final MenuPanel panel;
        float x;
        float y;
        boolean open = true;
        float scrollTarget;
        final Anim scroll = new Anim(0f, 110f);
        final Anim fold = new Anim(1f, 150f);
        // bounds from the last frame (for hit testing)
        float h;

        PanelState(String id, @Nullable MenuPanel panel, float x, float y) {
            this.id = id;
            this.panel = panel;
            this.x = x;
            this.y = y;
        }
    }

    @Nullable private final Screen parent;
    private final Ui ui = new Ui();
    private final Anim open = new Anim(0f, 180f);
    private boolean closing;
    private Tab tab = Tab.MODULES;
    private final List<PanelState> panels = new ArrayList<>();
    private final PanelState search;
    private final PanelState macro;
    @Nullable private PanelState dragging;
    private float dragDx;
    private float dragDy;
    private String query = "";
    @Nullable private SettingsCard card;
    private float scale = 1f;
    private float ox;
    private float oy;
    // tooltip
    @Nullable private Module tipModule;
    private long tipSince;
    // tab pages
    private final Page friendsPage = new FriendsPage();
    private final Page themesPage = new ThemesPage();
    private final Page settingsPage = new SettingsPage();
    private final Page profilesPage = new ProfilesPage();
    private final Page keybindsPage = new KeybindsPage();
    private final Anim pageScroll = new Anim(0f, 120f);
    private float pageScrollTarget;
    private float pageContentH;
    private final Anim tabSlide = new Anim(0f, 170f);

    public ClickGuiScreen(@Nullable Screen parent) {
        super(Text.literal("Aqua"));
        this.parent = parent;
        open.target(1f);
        for (MenuPanel p : MenuPanel.values()) {
            panels.add(new PanelState(p.name(), p, p.defaultX, p.defaultY));
        }
        search = new PanelState("SEARCH", null, 179, 12.5f);
        macro = new PanelState("MACRO", null, 327.5f, 12.5f);
        macro.open = false;
        macro.fold.snap(0f);
        panels.add(search);
        panels.add(macro);
        loadLayout();
    }

    @Override
    public Ui ui() {
        return ui;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    /** The current tab (used by the in-game tests). */
    public Tab tab() {
        return tab;
    }

    public void setTab(Tab t) {
        if (t == Tab.HUD) {
            saveLayout();
            client.setScreen(new HudEditorScreen(this));
            return;
        }
        if (t != tab) {
            tab = t;
            pageScrollTarget = 0;
            pageScroll.snap(0);
            card = null;
            UiSounds.click(1.05f);
        }
    }

    /** Opens the settings card of a module (as right-click does). */
    public void openSettings(Module m) {
        tab = Tab.MODULES;
        card = new SettingsCard(m, extrasFor(m));
    }

    /** The module whose settings card is open, or null. */
    @Nullable
    public Module openCard() {
        return card != null ? card.module : null;
    }

    /** Modules shown in a panel, in the order they are drawn (used by the in-game tests). */
    public static List<Module> modulesOf(MenuPanel p) {
        List<Module> l = new ArrayList<>();
        for (Module m : Aqua.modules().all()) {
            if (MenuPanel.of(m.id()) == p) {
                l.add(m);
            }
        }
        l.sort(Comparator.comparing(m -> m.name().toLowerCase(Locale.ROOT)));
        return l;
    }

    private List<SettingsCard.Extra> extrasFor(Module m) {
        List<SettingsCard.Extra> l = new ArrayList<>();
        String id = m.id();
        if (List.of("character_skin", "wings", "hats", "cape", "knives", "emotes", "kill_effects", "three_d_hand", "pharaoh_headdress").contains(id)) {
            l.add(new SettingsCard.Extra("Open Wardrobe", Icon.HANGER, () -> openClassic(AquaMenuScreen.Nav.WARDROBE)));
        }
        if (id.equals("music") || id.equals("music_card")) {
            l.add(new SettingsCard.Extra("Open Music page", Icon.NOTE, () -> openClassic(AquaMenuScreen.Nav.MUSIC)));
        }
        if (m instanceof HudWidget || id.equals("hud")) {
            l.add(new SettingsCard.Extra("Edit HUD layout", Icon.MOVE, () -> client.setScreen(new HudEditorScreen(this))));
        }
        return l;
    }

    private void openClassic(AquaMenuScreen.Nav nav) {
        AquaMenuScreen s = new AquaMenuScreen(this);
        s.showPage(nav);
        client.setScreen(s);
    }

    // ------------------------------------------------------------------ layout persistence

    private void loadLayout() {
        String saved = Aqua.ui().panelLayout.get();
        if (saved.isBlank()) {
            return;
        }
        for (String part : saved.split(";")) {
            String[] kv = part.split(":");
            if (kv.length != 2) {
                continue;
            }
            String[] v = kv[1].split(",");
            for (PanelState p : panels) {
                if (p.id.equals(kv[0]) && v.length >= 3) {
                    try {
                        p.x = clampX(Float.parseFloat(v[0]));
                        p.y = clampY(Float.parseFloat(v[1]));
                        p.open = v[2].equals("1");
                        p.fold.snap(p.open ? 1f : 0f);
                    } catch (NumberFormatException ignored) {
                        // keep the default place
                    }
                }
            }
        }
    }

    private void saveLayout() {
        StringBuilder b = new StringBuilder();
        for (PanelState p : panels) {
            if (!b.isEmpty()) {
                b.append(';');
            }
            b.append(p.id).append(':').append(Math.round(p.x * 10) / 10f).append(',').append(Math.round(p.y * 10) / 10f).append(',')
                    .append(p.open ? '1' : '0');
        }
        Aqua.ui().panelLayout.set(b.toString());
    }

    /** Puts every panel back where it starts (Theme tab → Reset panel layout). */
    public void resetLayout() {
        for (PanelState p : panels) {
            if (p.panel != null) {
                p.x = p.panel.defaultX;
                p.y = p.panel.defaultY;
                p.open = true;
            } else if (p == search) {
                p.x = 179;
                p.y = 12.5f;
                p.open = true;
            } else {
                p.x = 327.5f;
                p.y = 12.5f;
                p.open = false;
            }
            p.fold.snap(p.open ? 1f : 0f);
        }
        Aqua.ui().panelLayout.set("");
    }

    private static float clampX(float x) {
        return Math.max(-PW + 20, Math.min(CW - 20, x));
    }

    private static float clampY(float y) {
        return Math.max(0, Math.min(CH - HEADER - 26, y));
    }

    // ------------------------------------------------------------------ lifecycle

    @Override
    public void close() {
        if (closing) {
            return;
        }
        closing = true;
        open.target(0f);
        saveLayout();
        Aqua.config().save();
        if (Aqua.ui().animationScale() <= 0.01f) {
            client.setScreen(parent);
        }
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        float a = open.get();
        if (client.world == null) {
            renderPanoramaBackground(ctx, delta);
        }
        ctx.fill(0, 0, width, height, Colors.withAlpha(0x05070A, Math.round(95 * a)));
        // The world behind the panels is blurred, as in the reference.
        dev.aqua.client.render.Glass.backdrop(ctx, true);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        float a = Anim.easeOutCubic(open.get());
        if (closing && open.get() < 0.02f) {
            client.setScreen(parent);
            return;
        }
        scale = Math.min(width / CW, height / CH) * (float) Aqua.ui().menuScale.d();
        ox = (width - CW * scale) / 2f;
        oy = (height - CH * scale) / 2f;
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(ox, oy);
        ctx.getMatrices().scale(scale, scale);
        float cmx = (mouseX - ox) / scale;
        float cmy = (mouseY - oy) / scale;
        ui.begin(ctx, cmx, cmy);
        Runnable after = null;
        float slide = (1f - a) * 6f;
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(0, slide);
        if (tab == Tab.MODULES) {
            if (card != null) {
                after = card.render(ui, ctx, CW / 2f, 42.5f + SettingsCard.H / 2f, a);
                if (card != null && card.closed()) {
                    card = null;
                }
            } else {
                drawPanels(ctx, a);
            }
        } else {
            drawTabWindow(ctx, a);
        }
        ctx.getMatrices().popMatrix();
        drawTabBar(ctx, a);
        ui.drawTooltip(CW, CH);
        ui.end();
        ctx.getMatrices().popMatrix();
        if (after != null) {
            after.run();
        }
    }

    // ------------------------------------------------------------------ panels

    /** The panel under the mouse that should get input (the top one where panels overlap). */
    @Nullable
    private PanelState hotPanel() {
        if (dragging != null) {
            return dragging;
        }
        for (int i = panels.size() - 1; i >= 0; i--) {
            PanelState p = panels.get(i);
            float pw = p == search ? 70 : PW;
            if (ui.mx >= p.x && ui.mx < p.x + pw && ui.my >= p.y && ui.my < p.y + p.h) {
                return p;
            }
        }
        return null;
    }

    private void drawPanels(DrawContext ctx, float a) {
        PanelState hot = ui.popup == null ? hotPanel() : null;
        if (dragging != null) {
            if (ui.down) {
                dragging.x = clampX(ui.mx - dragDx);
                dragging.y = clampY(ui.my - dragDy);
            } else {
                dragging = null;
                saveLayout();
            }
        }
        Module hoveredRow = null;
        for (PanelState p : List.copyOf(panels)) {
            boolean interactive = p == hot && ui.capturing == null;
            Module h;
            if (p == search) {
                h = drawSearch(ctx, p, interactive, a);
            } else if (p == macro) {
                h = drawMacro(ctx, p, interactive, a);
            } else {
                h = drawPanel(ctx, p, modulesOf(p.panel), interactive, a);
            }
            if (h != null) {
                hoveredRow = h;
            }
        }
        // Tooltip: the module's description, after a short rest on its row.
        long now = System.currentTimeMillis();
        if (hoveredRow != tipModule) {
            tipModule = hoveredRow;
            tipSince = now;
        }
        if (tipModule != null && now - tipSince > 380 && dragging == null && ui.popup == null) {
            drawModuleTooltip(ctx, tipModule, a);
        }
    }

    private void panelFrame(DrawContext ctx, float x, float y, float w, float h, float a) {
        R2D.shadow(ctx, x, y + 1.5f, w, h, 5, 8, Colors.withAlpha(0x000000, 0.28f * a));
        R2D.round(ctx, x, y, w, h, 5, Colors.fade(ClickStyle.PANEL_BG, a));
        // Faint grid, as in the reference.
        ui.pushClip(x + 0.5f, y + 0.5f, w - 1, h - 1);
        float step = ROW / 2f;
        int grid = Colors.fade(ClickStyle.GRID, a);
        for (float gx = x + step; gx < x + w; gx += step) {
            R2D.rect(ctx, gx, y, 0.35f, h, grid);
        }
        for (float gy = y + step; gy < y + h; gy += step) {
            R2D.rect(ctx, x, gy, w, 0.35f, grid);
        }
        ui.popClip();
        R2D.outline(ctx, x, y, w, h, 5, 0.5f, Colors.fade(0x12FFFFFF, a));
    }

    /** Header with icon, title and fold arrow; handles dragging and folding. */
    private void header(DrawContext ctx, PanelState p, float w, Icon icon, String title, boolean foldable, boolean interactive, float a) {
        float x = p.x, y = p.y;
        R2D.icon(ctx, icon, x + 5, y + HEADER / 2f - 2.8f, 5.6f, Colors.fade(ClickStyle.HEADER_TEXT, a * 0.92f));
        R2D.textMid(ctx, title, x + 13.5f, y + HEADER / 2f + 0.2f, 5.2f, AquaFont.Weight.SEMIBOLD, Colors.fade(ClickStyle.HEADER_TEXT, a));
        float chx = x + w - 10.5f;
        if (foldable) {
            float f = p.fold.get();
            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().translate(chx + 3, y + HEADER / 2f);
            ctx.getMatrices().rotate((float) (-Math.PI / 2 * (1f - f)));
            R2D.icon(ctx, Icon.CHEVRON_DOWN, -3, -3, 6, Colors.fade(ClickStyle.MUTED, a));
            ctx.getMatrices().popMatrix();
        }
        if (!interactive) {
            return;
        }
        boolean onArrow = foldable && ui.hover(chx - 2, y, 10, HEADER);
        if (foldable && (ui.clickedIn(chx - 2, y, 10, HEADER) || ui.rightClickedIn(x, y, w, HEADER))) {
            p.open = !p.open;
            p.fold.target(p.open ? 1f : 0f);
            UiSounds.click(p.open ? 1.1f : 0.9f);
            saveLayout();
        } else if (!onArrow && ui.clicked && ui.hover(x, y, w, HEADER)) {
            ui.clicked = false;
            dragging = p;
            dragDx = ui.mx - x;
            dragDy = ui.my - y;
            panels.remove(p);
            panels.add(p);
        }
    }

    @Nullable
    private Module drawPanel(DrawContext ctx, PanelState p, List<Module> mods, boolean interactive, float a) {
        MenuPanel mp = p.panel;
        float x = p.x, y = p.y;
        float listTop = y + HEADER + 0.5f;
        int fit = (int) Math.max(3, Math.floor((TAB_Y - 6 - listTop) / ROW));
        int visible = Math.min(mods.size(), Math.min(mp.maxRows, fit));
        float listH = visible * ROW;
        float f = Anim.easeInOutCubic(p.fold.target(p.open ? 1f : 0f).get());
        float h = HEADER + (listH + 2.5f) * f;
        p.h = h;
        panelFrame(ctx, x, y, PW, h, a);
        header(ctx, p, PW, mp.icon, mp.title, true, interactive, a);
        if (f < 0.01f) {
            return null;
        }
        // Scrolling
        float max = Math.max(0, mods.size() * ROW - listH);
        if (interactive && ui.scroll != 0 && ui.hover(x, listTop, PW, listH)) {
            p.scrollTarget -= ui.scroll * ROW * 2;
            ui.scroll = 0;
        }
        p.scrollTarget = Math.max(0, Math.min(max, p.scrollTarget));
        float scrolled = p.scroll.target(p.scrollTarget).get();
        Module hovered = null;
        ui.pushClip(x, listTop, PW, listH * f);
        for (int i = 0; i < mods.size(); i++) {
            float ry = listTop + i * ROW - scrolled;
            if (ry + ROW < listTop - 1 || ry > listTop + listH + 1) {
                continue;
            }
            Module m = mods.get(i);
            if (moduleRow(ctx, m, x, ry, PW - (max > 0 ? 2 : 0), interactive && f > 0.95f, a * f)) {
                hovered = m;
            }
        }
        ui.popClip();
        if (max > 0) {
            float th = Math.max(10, listH * listH / (mods.size() * ROW));
            float ty = listTop + (listH - th) * (scrolled / max);
            R2D.rect(ctx, x + PW - 2.2f, listTop, 0.5f, listH * f, Colors.fade(0x18FFFFFF, a));
            R2D.round(ctx, x + PW - 2.6f, ty, 1.3f, th * f, 0.65f, Colors.fade(0x66FFFFFF, a));
        }
        return hovered;
    }

    /** One module row; returns true while the mouse is on it. */
    private boolean moduleRow(DrawContext ctx, Module m, float x, float y, float w, boolean interactive, float a) {
        boolean on = m.isEnabled();
        boolean hov = interactive && ui.hover(x + 1.5f, y, w - 3, ROW);
        float en = ui.anim("cg_on_" + m.id(), 140, on ? 1 : 0).target(on).get();
        float ha = ui.anim("cg_h_" + m.id(), 100, 0).target(hov).get();
        if (ha > 0.01f && en < 0.99f) {
            R2D.round(ctx, x + 2, y + 0.6f, w - 4, ROW - 1.2f, 3.2f, Colors.withAlpha(0xFFFFFF, 0.06f * ha * a));
        }
        if (en > 0.01f) {
            R2D.round(ctx, x + 2, y + 0.6f, w - 4, ROW - 1.2f, 3.2f, Colors.fade(Colors.lerp(ClickStyle.blue(), Colors.brighten(ClickStyle.blue(), 0.12f), ha), a * en));
        }
        String capId = "cg_bind_" + m.id();
        boolean cap = capId.equals(ui.capturing);
        String bind = cap ? "..." : (m.bind().isBound() ? m.bind().display().toUpperCase(Locale.ROOT) : "");
        float bw = bind.isEmpty() ? 0 : R2D.width(bind, 4.2f, AquaFont.Weight.MEDIUM) + 4;
        int fg = Colors.lerp(ClickStyle.ROW_TEXT, 0xFFFFFFFF, Math.max(en, ha * 0.5f));
        R2D.textMid(ctx, AquaFont.ellipsize(m.name(), w - 13 - bw, 4.7f, AquaFont.Weight.MEDIUM), x + 6.5f, y + ROW / 2f, 4.7f,
                AquaFont.Weight.MEDIUM, Colors.fade(fg, a));
        if (!bind.isEmpty()) {
            R2D.textRight(ctx, AquaFont.ellipsize(bind, 22, 4.2f, AquaFont.Weight.MEDIUM), x + w - 5.5f, y + ROW / 2f, 4.2f,
                    AquaFont.Weight.MEDIUM, Colors.fade(on ? 0xCCFFFFFF : ClickStyle.MUTED, a));
        }
        if (!interactive) {
            return false;
        }
        if (ui.clickedIn(x + 1.5f, y, w - 3, ROW)) {
            m.setEnabled(!on, false);
            UiSounds.toggle(!on);
        }
        if (ui.rightClickedIn(x + 1.5f, y, w - 3, ROW)) {
            UiSounds.click(1.1f);
            openSettings(m);
        }
        if (ui.middleClickedIn(x + 1.5f, y, w - 3, ROW)) {
            ui.startCapture(capId, c -> {
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
        return hov;
    }

    private void drawModuleTooltip(DrawContext ctx, Module m, float a) {
        float size = 4.9f;
        List<String> lines = SettingsCard.wrap(m.description(), 112, size, AquaFont.Weight.MEDIUM);
        lines.add("Right-click for settings");
        float tw = 0;
        for (String l : lines) {
            tw = Math.max(tw, R2D.width(l, size, AquaFont.Weight.MEDIUM));
        }
        float w = tw + 10;
        float h = lines.size() * 6.6f + 6;
        float x = Math.min(ui.mx + 7, CW - w - 3);
        float y = Math.min(ui.my + 8, CH - h - 3);
        R2D.shadow(ctx, x, y + 1, w, h, 4.5f, 6, Colors.withAlpha(0x000000, 0.4f * a));
        R2D.round(ctx, x, y, w, h, 4.5f, Colors.fade(ClickStyle.TOOLTIP_BG, a));
        float ly = y + 3 + 3.3f;
        for (String l : lines) {
            R2D.textMid(ctx, l, x + 5, ly, size, AquaFont.Weight.MEDIUM, Colors.fade(0xFFE9ECF0, a));
            ly += 6.6f;
        }
    }

    // ------------------------------------------------------------------ search and macro panels

    @Nullable
    private Module drawSearch(DrawContext ctx, PanelState p, boolean interactive, float a) {
        float w = 70;
        List<Module> results = query.isBlank() ? List.of() : Aqua.modules().search(query);
        int shown = Math.min(results.size(), 14);
        float listTop = p.y + 25;
        float h = 25 + (shown > 0 ? shown * ROW + 2.5f : 0) + (results.size() > shown ? 8 : 0);
        p.h = h;
        panelFrame(ctx, p.x, p.y, w, h, a);
        header(ctx, p, w, Icon.SEARCH, "Search", false, interactive, a);
        float fx = p.x + 3.5f, fy = p.y + 12.5f, fw = w - 7, fh = 9.5f;
        boolean focus = "search".equals(ui.focus);
        R2D.round(ctx, fx, fy, fw, fh, 3, Colors.fade(ClickStyle.FIELD, a));
        if (focus) {
            R2D.outline(ctx, fx, fy, fw, fh, 3, 0.5f, Colors.fade(Colors.withAlpha(ClickStyle.blue(), 0.8f), a));
        }
        String before = query;
        ui.bareFields = true;
        query = ui.textField("search", fx, fy, fw, fh, query, "Search modules...", 40, null, q -> {
        }, null);
        ui.bareFields = false;
        if (!query.equals(before)) {
            p.scrollTarget = 0;
        }
        Module hovered = null;
        for (int i = 0; i < shown; i++) {
            if (moduleRow(ctx, results.get(i), p.x, listTop + i * ROW, w, interactive, a)) {
                hovered = results.get(i);
            }
        }
        if (results.size() > shown) {
            R2D.textMid(ctx, "+" + (results.size() - shown) + " more", p.x + 6.5f, listTop + shown * ROW + 4, 4.4f, AquaFont.Weight.MEDIUM,
                    Colors.fade(ClickStyle.MUTED, a));
        }
        if (!query.isBlank() && results.isEmpty()) {
            p.h = h + 9;
            R2D.textMid(ctx, "No modules found", p.x + 6.5f, listTop + 4, 4.6f, AquaFont.Weight.MEDIUM, Colors.fade(ClickStyle.MUTED, a));
        }
        return hovered;
    }

    @Nullable
    private Module drawMacro(DrawContext ctx, PanelState p, boolean interactive, float a) {
        Module chat = Aqua.modules().byId("chat_macro");
        List<String[]> rows = chat instanceof dev.aqua.client.module.qol.ChatMacro cm ? cm.macroRows() : List.of();
        float f = Anim.easeInOutCubic(p.fold.target(p.open ? 1f : 0f).get());
        int n = rows.size() + 1;
        float h = HEADER + (n * ROW + 2.5f) * f;
        p.h = h;
        panelFrame(ctx, p.x, p.y, PW, h, a);
        header(ctx, p, PW, Icon.KEYBOARD, "Macro", true, interactive, a);
        if (f < 0.01f) {
            return null;
        }
        float y = p.y + HEADER + 0.5f;
        ui.pushClip(p.x, y, PW, n * ROW * f);
        for (String[] r : rows) {
            R2D.textMid(ctx, AquaFont.ellipsize(r[0], PW - 30, 4.7f, AquaFont.Weight.MEDIUM), p.x + 6.5f, y + ROW / 2f, 4.7f,
                    AquaFont.Weight.MEDIUM, Colors.fade(ClickStyle.ROW_TEXT, a * f));
            R2D.textRight(ctx, r[1], p.x + PW - 5.5f, y + ROW / 2f, 4.2f, AquaFont.Weight.MEDIUM, Colors.fade(ClickStyle.MUTED, a * f));
            if (interactive && chat != null && (ui.clickedIn(p.x + 1.5f, y, PW - 3, ROW) || ui.rightClickedIn(p.x + 1.5f, y, PW - 3, ROW))) {
                openSettings(chat);
            }
            y += ROW;
        }
        boolean hov = interactive && ui.hover(p.x + 1.5f, y, PW - 3, ROW);
        R2D.icon(ctx, Icon.PLUS, p.x + 5.5f, y + ROW / 2f - 2.6f, 5.2f, Colors.fade(hov ? 0xFFFFFFFF : ClickStyle.MUTED, a * f));
        R2D.textMid(ctx, "New macro", p.x + 12.5f, y + ROW / 2f, 4.7f, AquaFont.Weight.MEDIUM,
                Colors.fade(hov ? 0xFFFFFFFF : ClickStyle.MUTED, a * f));
        if (interactive && chat != null && ui.clickedIn(p.x + 1.5f, y, PW - 3, ROW)) {
            if (chat instanceof dev.aqua.client.module.qol.ChatMacro cm) {
                cm.addSlot();
            }
            openSettings(chat);
        }
        ui.popClip();
        return null;
    }

    // ------------------------------------------------------------------ tab bar and tab windows

    private void drawTabBar(DrawContext ctx, float a) {
        Tab[] tabs = Tab.values();
        float size = 4.9f;
        float[] widths = new float[tabs.length];
        float total = 4;
        for (int i = 0; i < tabs.length; i++) {
            widths[i] = R2D.width(tabs[i].label, size, AquaFont.Weight.MEDIUM) + (i == 0 ? 26 : 18);
            total += widths[i];
        }
        float x = CW / 2f - total / 2f;
        float y = TAB_Y;
        float h = 16;
        R2D.shadow(ctx, x, y + 1, total, h, h / 2f, 7, Colors.withAlpha(0x000000, 0.3f * a));
        R2D.round(ctx, x, y, total, h, h / 2f, Colors.fade(ClickStyle.TAB_BG, a));
        float tx = x + 2;
        float selX = 0, selW = 0;
        for (int i = 0; i < tabs.length; i++) {
            if (tabs[i] == tab) {
                selX = tx;
                selW = widths[i];
            }
            tx += widths[i];
        }
        float sx = tabSlide.target(selX).get();
        if (Math.abs(sx - selX) > 60) {
            tabSlide.snap(selX);
            sx = selX;
        }
        R2D.round(ctx, sx, y + 1.75f, selW, h - 3.5f, (h - 3.5f) / 2f, Colors.fade(ClickStyle.blue(), a));
        tx = x + 2;
        for (int i = 0; i < tabs.length; i++) {
            boolean sel = tabs[i] == tab;
            boolean hov = ui.hover(tx, y, widths[i], h);
            int col = sel ? 0xFFFFFFFF : Colors.lerp(0xFFC8CDD4, 0xFFFFFFFF, hov ? 1f : 0f);
            R2D.textCentered(ctx, tabs[i].label, tx + widths[i] / 2f, y + h / 2f, size, AquaFont.Weight.MEDIUM, Colors.fade(col, a));
            if (ui.popup == null && ui.capturing == null && ui.clickedIn(tx, y, widths[i], h)) {
                setTab(tabs[i]);
            }
            tx += widths[i];
        }
    }

    private void drawTabWindow(DrawContext ctx, float a) {
        float w = 330, h = 286;
        float x = (CW - w) / 2f, y = 40;
        panelFrame(ctx, x, y, w, h, a);
        Icon icon = switch (tab) {
            case FRIENDS -> Icon.USER;
            case THEME -> Icon.PALETTE;
            default -> Icon.FOLDER;
        };
        R2D.icon(ctx, icon, x + 6, y + 6.5f, 7, Colors.fade(ClickStyle.HEADER_TEXT, a));
        R2D.textMid(ctx, tab.label, x + 17, y + 10, 7, AquaFont.Weight.SEMIBOLD, Colors.fade(ClickStyle.HEADER_TEXT, a));
        if (tab == Tab.THEME && ui.button("cg_reset_layout", x + w - 82, y + 4.5f, 76, 11, "Reset panel layout", Icon.RESET, false)) {
            resetLayout();
            Aqua.notifications().info("Menu", "Panels moved back to their starting places");
        }
        float top = y + 20, viewH = h - 26;
        float scrolled = pageScroll.target(pageScrollTarget).get();
        ui.pushClip(x + 2, top, w - 4, viewH);
        float cy = top + 2 - scrolled;
        float cx = x + 9, cw = w - 22;
        switch (tab) {
            case FRIENDS -> cy = friendsPage.render(ui, ctx, cx, cy, cw, a, this);
            case THEME -> {
                cy = themesPage.render(ui, ctx, cx, cy, cw, a, this) + 6;
                cy = settingsPage.render(ui, ctx, cx, cy, cw, a, this);
            }
            default -> {
                cy = profilesPage.render(ui, ctx, cx, cy, cw, a, this) + 6;
                cy = keybindsPage.render(ui, ctx, cx, cy, cw, a, this);
            }
        }
        pageContentH = cy + scrolled - top;
        ui.popClip();
        float max = Math.max(0, pageContentH - viewH);
        if (ui.scroll != 0 && ui.hover(x, top, w, viewH)) {
            pageScrollTarget -= ui.scroll * 22;
            ui.scroll = 0;
        }
        pageScrollTarget = Math.max(0, Math.min(max, pageScrollTarget));
        if (max > 0) {
            float th = Math.max(14, viewH * viewH / pageContentH);
            float ty = top + (viewH - th) * (scrolled / max);
            R2D.round(ctx, x + w - 5, ty, 1.6f, th, 0.8f, Colors.fade(0x66FFFFFF, a));
        }
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
        if (input.key() == GLFW.GLFW_KEY_ESCAPE && ui.popup == null && ui.capturing == null) {
            if ("search".equals(ui.focus) || (ui.focus == null && !query.isEmpty() && card == null && tab == Tab.MODULES)) {
                ui.focus = null;
                if (!query.isEmpty()) {
                    query = "";
                    ui.setText("search", "");
                    return true;
                }
            }
            if (ui.focus != null) {
                ui.focus = null;
                return true;
            }
            if (card != null) {
                card.requestClose();
                return true;
            }
            if (tab != Tab.MODULES) {
                setTab(Tab.MODULES);
                return true;
            }
            close();
            return true;
        }
        if (ui.onKey(input)) {
            return true;
        }
        KeyBinding menu = Aqua.keybinds().menu;
        if (ui.focus == null && menu.matchesKey(input)) {
            close();
            return true;
        }
        return true;
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (ui.focus == null && ui.popup == null && ui.capturing == null && card == null && tab == Tab.MODULES
                && input.isValidChar() && input.codepoint() != ' ') {
            ui.focus = "search";
        }
        ui.onChar(input);
        return true;
    }
}
