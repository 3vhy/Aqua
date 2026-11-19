package dev.aqua.client.hud.widgets;

import dev.aqua.client.Aqua;
import dev.aqua.client.hud.Anchor;
import dev.aqua.client.hud.HudWidget;
import dev.aqua.client.hud.Panel;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.Icon;
import dev.aqua.client.render.R2D;
import dev.aqua.client.render.font.AquaFont;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.theme.Theme;
import dev.aqua.client.util.TextUtil;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

import java.util.ArrayList;
import java.util.List;

/** "Effects" panel: icon, name + level, remaining time (Fern R2 0:01). Own effects only. */
public final class EffectsWidget extends HudWidget {
    public final BoolSetting hideVanilla = add(new BoolSetting("hide_vanilla", "Hide vanilla effect icons", true));
    private final BoolSetting blinkEnding = add(new BoolSetting("blink", "Pulse when ending", true));

    public EffectsWidget() {
        super("effects", "Effects", "Your status effects and remaining time.", Icon.FLASK, true, Anchor.TOP_LEFT, 100, 37,
                "potions", "potion effects", "status");
    }

    private List<StatusEffectInstance> list(boolean preview) {
        List<StatusEffectInstance> out = new ArrayList<>();
        if (mc.player != null) {
            for (StatusEffectInstance e : mc.player.getStatusEffects()) {
                if (e.shouldShowIcon()) {
                    out.add(e);
                }
            }
        }
        if (out.isEmpty() && preview) {
            out.add(new StatusEffectInstance(StatusEffects.ABSORPTION, 20 * 89, 0));
            out.add(new StatusEffectInstance(StatusEffects.SPEED, 20 * 89, 1));
            out.add(new StatusEffectInstance(StatusEffects.STRENGTH, 20 * 89, 1));
        }
        out.sort((a, b) -> Integer.compare(b.getDuration(), a.getDuration()));
        return out;
    }

    private static String label(StatusEffectInstance e) {
        String n = e.getEffectType().value().getName().getString();
        return e.getAmplifier() > 0 ? n + " " + (e.getAmplifier() + 1) : n;
    }

    @Override
    public boolean hasContent() {
        return !list(false).isEmpty();
    }

    @Override
    public float width(boolean preview) {
        float w = 80;
        for (StatusEffectInstance e : list(preview)) {
            w = Math.max(w, Panel.PAD * 2 + 10 + AquaFont.width(label(e), Panel.TEXT, AquaFont.Weight.MEDIUM) + 12
                    + AquaFont.width(TextUtil.duration(e.isInfinite() ? -1 : e.getDuration()), Panel.TEXT, AquaFont.Weight.MEDIUM));
        }
        return w;
    }

    @Override
    public float height(boolean preview) {
        return Panel.height(Math.max(1, list(preview).size()));
    }

    @Override
    public void draw(DrawContext ctx, boolean preview, float alpha) {
        Theme t = Aqua.theme();
        List<StatusEffectInstance> l = list(preview);
        float w = width(preview);
        Panel.header(ctx, w, "Effects", Icon.FLASK, showBackground(), bg(alpha), alpha, Math.max(1, l.size()));
        for (int i = 0; i < l.size(); i++) {
            StatusEffectInstance e = l.get(i);
            float y = Panel.rowY(i);
            float a = alpha;
            if (blinkEnding.on() && !e.isInfinite() && e.getDuration() < 200) {
                a *= 0.55f + 0.45f * (float) Math.abs(Math.sin(System.currentTimeMillis() / 250.0));
            }
            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().translate(Panel.PAD, y - 3.75f);
            ctx.getMatrices().scale(7.5f / 18f, 7.5f / 18f);
            ctx.drawGuiTexture(RenderPipelines.GUI_TEXTURED, InGameHud.getEffectTexture(e.getEffectType()), 0, 0, 18, 18,
                    Colors.withAlpha(0xFFFFFF, Math.round(a * 255)));
            ctx.getMatrices().popMatrix();
            R2D.textMid(ctx, label(e), Panel.PAD + 10, y, Panel.TEXT, AquaFont.Weight.MEDIUM, Colors.fade(t.text(), a));
            R2D.textRight(ctx, TextUtil.duration(e.isInfinite() ? -1 : e.getDuration()), w - Panel.PAD, y, Panel.TEXT,
                    AquaFont.Weight.MEDIUM, Colors.fade(t.text(), a * 0.9f));
        }
    }
}
