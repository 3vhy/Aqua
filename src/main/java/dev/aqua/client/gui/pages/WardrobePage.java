package dev.aqua.client.gui.pages;

import dev.aqua.client.Aqua;
import dev.aqua.client.cosmetic.Emotes;
import dev.aqua.client.cosmetic.Look;
import dev.aqua.client.cosmetic.character.Characters;
import dev.aqua.client.gui.AquaMenuScreen;
import dev.aqua.client.gui.Ui;
import dev.aqua.client.gui.UiSounds;
import dev.aqua.client.module.Module;
import dev.aqua.client.module.cosmetic.Cape;
import dev.aqua.client.module.cosmetic.CharacterSkin;
import dev.aqua.client.module.cosmetic.Hats;
import dev.aqua.client.module.cosmetic.KillEffects;
import dev.aqua.client.module.cosmetic.Knives;
import dev.aqua.client.module.cosmetic.Wings;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.Icon;
import dev.aqua.client.render.R2D;
import dev.aqua.client.render.font.AquaFont;
import dev.aqua.client.setting.EnumSetting;
import dev.aqua.client.setting.Setting;
import dev.aqua.client.theme.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 67-style wardrobe: a large live preview, category tabs and a grid of cards that each show a 3D
 * preview of that cosmetic on your own player. Hovering a card previews it in the large view.
 * Everything here is visible only to you.
 */
public final class WardrobePage implements Page {
    private enum Tab {
        SKINS("3D Skins", Icon.PERSON), WINGS("Wings", Icon.WING), HATS("Hats", Icon.HAT), CAPES("Capes", Icon.CAPE),
        KNIVES("Knives", Icon.KNIFE_KARAMBIT), EMOTES("Emotes", Icon.SMILE), KILL("Kill FX", Icon.SKULL);

        final String label;
        final Icon icon;

        Tab(String label, Icon icon) {
            this.label = label;
            this.icon = icon;
        }
    }

    private record Card(String label, String sub, @Nullable Look look, @Nullable Icon icon, boolean selected, Runnable select) {}

    private Tab tab = Tab.SKINS;
    private boolean spin = true;

    private static <M extends Module> M mod(Class<M> c) {
        return Aqua.modules().get(c);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <E extends Enum<E>> EnumSetting<E> enumSetting(Module m, String id) {
        for (Setting<?> s : m.settings()) {
            if (s.id().equals(id)) {
                return (EnumSetting) s;
            }
        }
        throw new IllegalStateException(id);
    }

    @Override
    public float render(Ui ui, DrawContext ctx, float x, float y, float w, float a, AquaMenuScreen menu) {
        Theme t = Aqua.theme();
        MinecraftClient mc = MinecraftClient.getInstance();
        // ------------------------------------------------------------- tabs
        float tx = x;
        for (Tab tb : Tab.values()) {
            float tw = R2D.width(tb.label, 5.2f, AquaFont.Weight.SEMIBOLD) + 18;
            boolean sel = tb == tab;
            boolean hov = ui.hover(tx, y, tw, 13);
            R2D.round(ctx, tx, y, tw, 13, 6.5f, Colors.fade(sel ? Colors.withAlpha(t.accent(), 0.22f)
                    : Colors.withAlpha(t.panel(), hov ? 1f : 0.85f), a));
            R2D.outline(ctx, tx, y, tw, 13, 6.5f, 0.6f, Colors.fade(sel ? Colors.withAlpha(t.accent(), 0.7f) : Colors.withAlpha(t.border(), 0.5f), a));
            R2D.icon(ctx, tb.icon, tx + 5, y + 3, 7, Colors.fade(sel ? t.accent() : t.muted(), a));
            R2D.textMid(ctx, tb.label, tx + 14, y + 6.5f, 5.2f, AquaFont.Weight.SEMIBOLD, Colors.fade(sel ? t.text() : t.muted(), a));
            if (ui.clickedIn(tx, y, tw, 13)) {
                tab = tb;
                UiSounds.click(1.05f);
            }
            tx += tw + 3;
        }
        y += 18;

        // ------------------------------------------------------------- cards for this tab
        List<Card> cards = cards();
        Module owner = owner();
        float pw = Math.min(118, w * 0.36f);
        float ph = 172;
        float gx = x + pw + 6;
        float gw = w - pw - 6;
        int cols = gw > 230 ? 4 : 3;
        float gap = 4;
        float cw = (gw - gap * (cols - 1)) / cols;
        float chh = tab == Tab.EMOTES || tab == Tab.KILL || tab == Tab.KNIVES ? 40 : 62;

        // header row: Show in game toggle
        if (owner != null) {
            R2D.textRight(ctx, "Show in game", gx + gw - 22, y + 5, 5.2f, AquaFont.Weight.MEDIUM, Colors.fade(t.muted(), a));
            boolean on = ui.toggle("wd_on_" + tab, gx + gw - 18, y + 0.25f, owner.isEnabled());
            if (on != owner.isEnabled()) {
                owner.setEnabled(on, false);
            }
            if (tab == Tab.KNIVES) {
                R2D.textMid(ctx, "Finish", gx, y + 5, 5.2f, AquaFont.Weight.MEDIUM, Colors.fade(t.muted(), a));
                ui.dropdown("wd_finish", gx + 22, y - 0.5f, 74, WardrobePage.<Knives.Finish>enumSetting(owner, "finish"));
            }
        } else if (tab == Tab.EMOTES) {
            R2D.textMid(ctx, "Plays on your player (you only)", gx, y + 5, 5.2f, AquaFont.Weight.MEDIUM, Colors.fade(t.muted(), a));
        }
        float gy = y + 14;

        Look hoverLook = null;
        String hoverLabel = null;
        for (int i = 0; i < cards.size(); i++) {
            Card c = cards.get(i);
            float cx = gx + (i % cols) * (cw + gap);
            float cy = gy + (i / cols) * (chh + gap);
            boolean hov = ui.hover(cx, cy, cw, chh);
            float ha = ui.anim("wdc" + tab + i, 120, 0).target(hov).get();
            int bg = Colors.lerp(Colors.withAlpha(t.panel(), 0.92f), Colors.brighten(t.panel(), 0.08f), ha);
            R2D.round(ctx, cx, cy, cw, chh, 6, Colors.fade(bg, a));
            R2D.outline(ctx, cx, cy, cw, chh, 6, c.selected ? 1f : 0.6f,
                    Colors.fade(c.selected ? t.accent() : Colors.withAlpha(t.border(), 0.4f + ha * 0.3f), a));
            if (c.selected) {
                R2D.circle(ctx, cx + cw - 6, cy + 6, 2.2f, Colors.fade(t.accent(), a));
            }
            if (c.look != null && mc.player != null) {
                Look l = c.look;
                int ex1 = Math.round(cx + 4), ey1 = Math.round(cy + 3), ex2 = Math.round(cx + cw - 4), ey2 = Math.round(cy + chh - 12);
                float fakeX = cx + cw * 0.15f;
                float fakeY = cy + chh * 0.35f;
                Look.preview(l, () -> InventoryScreen.drawEntity(ctx, ex1, ey1, ex2, ey2, Math.round((chh - 14) * 0.42f), 0.0625f,
                        fakeX, fakeY, mc.player));
            } else if (c.icon != null) {
                float is = Math.min(22, chh - 18);
                R2D.icon(ctx, c.icon, cx + cw / 2f - is / 2f, cy + 4, is, Colors.fade(c.selected ? t.accent() : t.text(), a));
            }
            R2D.textCentered(ctx, AquaFont.ellipsize(c.label, cw - 6, 5f, AquaFont.Weight.SEMIBOLD), cx + cw / 2f, cy + chh - 6.5f, 5f,
                    AquaFont.Weight.SEMIBOLD, Colors.fade(t.text(), a));
            if (hov) {
                hoverLook = c.look;
                hoverLabel = c.label;
                if (!c.sub.isEmpty()) {
                    ui.tooltip(c.sub);
                }
            }
            if (ui.clickedIn(cx, cy, cw, chh)) {
                c.select.run();
                UiSounds.click(1.1f);
            }
        }
        int rows = (cards.size() + cols - 1) / cols;
        float gridBottom = gy + rows * (chh + gap);

        // ------------------------------------------------------------- big preview
        Page.card(ctx, t, x, y, pw, ph, a);
        R2D.roundGradientV(ctx, x + 3, y + 3, pw - 6, ph - 6, 5, Colors.fade(Colors.withAlpha(t.accent(), 0.12f), a),
                Colors.fade(Colors.withAlpha(t.background(), 0f), a));
        Look show = hoverLook != null ? hoverLook : Look.current();
        String name = hoverLabel != null ? hoverLabel : currentLabel();
        if (mc.player != null) {
            float cxp = x + pw / 2f;
            float time = (System.currentTimeMillis() % 100000L) / 1000f;
            float mx = spin ? cxp + (float) Math.sin(time * 0.9f) * 120f : ui.mx;
            float my = spin ? y + ph * 0.38f : ui.my;
            int px1 = Math.round(x + 6), py1 = Math.round(y + 16), px2 = Math.round(x + pw - 6), py2 = Math.round(y + ph - 26);
            Look.preview(show, () -> InventoryScreen.drawEntity(ctx, px1, py1, px2, py2, 46, 0.0625f, mx, my, mc.player));
        } else {
            R2D.textCentered(ctx, "Join a world", x + pw / 2f, y + ph / 2f - 5, 6f, AquaFont.Weight.SEMIBOLD, Colors.fade(t.text(), a));
            R2D.textCentered(ctx, "to preview your player", x + pw / 2f, y + ph / 2f + 5, 5f, AquaFont.Weight.MEDIUM, Colors.fade(t.muted(), a));
        }
        R2D.text(ctx, AquaFont.ellipsize(name, pw - 14, 6.4f, AquaFont.Weight.SEMIBOLD), x + 7, y + 6, 6.4f, AquaFont.Weight.SEMIBOLD,
                Colors.fade(t.text(), a));
        R2D.textMid(ctx, "Spin", x + 7, y + ph - 12, 5f, AquaFont.Weight.MEDIUM, Colors.fade(t.muted(), a));
        spin = ui.toggle("wd_spin", x + 24, y + ph - 16.75f, spin);
        R2D.textRight(ctx, "Only you see these", x + pw - 7, y + ph - 12, 4.4f, AquaFont.Weight.MEDIUM, Colors.fade(t.muted(), a));
        return Math.max(y + ph, gridBottom) + 4;
    }

    @Nullable
    private Module owner() {
        return switch (tab) {
            case SKINS -> mod(CharacterSkin.class);
            case WINGS -> mod(Wings.class);
            case HATS -> mod(Hats.class);
            case CAPES -> mod(Cape.class);
            case KNIVES -> mod(Knives.class);
            case KILL -> mod(KillEffects.class);
            case EMOTES -> null;
        };
    }

    private String currentLabel() {
        Look l = Look.current();
        String none = "Nothing equipped";
        return switch (tab) {
            case SKINS -> l.character() != null ? l.character().label : "Your skin";
            case WINGS -> l.wings() != null ? l.wings().displayName() + " wings" : none;
            case HATS -> l.hat() != null ? l.hat().displayName() : none;
            case CAPES -> l.cape() != null ? l.cape().displayName() + " cape" : none;
            case KNIVES -> mod(Knives.class).isEnabled() ? mod(Knives.class).type().displayName() : none;
            case EMOTES -> Emotes.current() != null ? Emotes.current().label : "Emotes";
            case KILL -> mod(KillEffects.class).isEnabled()
                    ? WardrobePage.<KillEffects.Effect>enumSetting(mod(KillEffects.class), "effect").get().displayName() : none;
        };
    }

    @SuppressWarnings("unchecked")
    private List<Card> cards() {
        List<Card> out = new ArrayList<>();
        Look base = Look.current();
        switch (tab) {
            case SKINS -> {
                CharacterSkin cs = mod(CharacterSkin.class);
                out.add(new Card("Your skin", "Your normal Minecraft skin", base.withCharacter(null), null, !cs.isEnabled(),
                        () -> cs.setEnabled(false, false)));
                for (Characters c : Characters.values()) {
                    boolean sel = cs.isEnabled() && cs.character() == c;
                    out.add(new Card(c.label, c.description, base.withCharacter(c), null, sel, () -> {
                        cs.select(c);
                        cs.setEnabled(true, false);
                    }));
                }
            }
            case WINGS -> {
                Wings wm = mod(Wings.class);
                EnumSetting<Wings.Style> st = enumSetting(wm, "style");
                for (Wings.Style s : Wings.Style.values()) {
                    out.add(new Card(s.displayName(), "", base.withCharacter(null).withWings(s), null, wm.isEnabled() && st.get() == s, () -> {
                        st.set(s);
                        wm.setEnabled(true, false);
                    }));
                }
            }
            case HATS -> {
                Hats hm = mod(Hats.class);
                EnumSetting<Hats.Style> st = enumSetting(hm, "style");
                for (Hats.Style s : Hats.Style.values()) {
                    out.add(new Card(s.displayName(), "", base.withCharacter(null).withHat(s), null, hm.isEnabled() && st.get() == s, () -> {
                        st.set(s);
                        hm.setEnabled(true, false);
                    }));
                }
            }
            case CAPES -> {
                Cape cm = mod(Cape.class);
                EnumSetting<Cape.Style> st = enumSetting(cm, "style");
                for (Cape.Style s : Cape.Style.values()) {
                    out.add(new Card(s.displayName(), "Shown from behind in the large preview when spinning",
                            base.withCharacter(null).withCape(s), null, cm.isEnabled() && st.get() == s, () -> {
                        st.set(s);
                        cm.setEnabled(true, false);
                    }));
                }
            }
            case KNIVES -> {
                Knives km = mod(Knives.class);
                for (Knives.Type k : Knives.Type.values()) {
                    Icon icon = switch (k) {
                        case KARAMBIT -> Icon.KNIFE_KARAMBIT;
                        case BUTTERFLY -> Icon.KNIFE_BUTTERFLY;
                        case BAYONET -> Icon.KNIFE_BAYONET;
                        case STILETTO -> Icon.KNIFE_DAGGER;
                        default -> Icon.SWORD;
                    };
                    out.add(new Card(k.displayName(), k.sword ? "Fantasy sword in place of your sword; press Y to inspect"
                            : "Replaces how swords look in your hands; press Y to inspect", null, icon,
                            km.isEnabled() && km.type() == k, () -> {
                        km.select(k);
                        km.setEnabled(true, false);
                    }));
                }
            }
            case EMOTES -> {
                for (Emotes.Emote e : Emotes.Emote.values()) {
                    out.add(new Card(e.label, "Click to play", null, Icon.SMILE, Emotes.current() == e, () -> Emotes.play(e)));
                }
            }
            case KILL -> {
                KillEffects ke = mod(KillEffects.class);
                EnumSetting<KillEffects.Effect> st = enumSetting(ke, "effect");
                Icon[] icons = {Icon.BOLT, Icon.DROP, Icon.SPARKLE, Icon.DROP, Icon.STAR, Icon.SUN};
                for (KillEffects.Effect e : KillEffects.Effect.values()) {
                    out.add(new Card(e.displayName(), "Plays where something you hit is defeated", null, icons[e.ordinal()],
                            ke.isEnabled() && st.get() == e, () -> {
                        st.set(e);
                        ke.setEnabled(true, false);
                    }));
                }
            }
        }
        return out;
    }
}
