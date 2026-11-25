package dev.aqua.client.hud.widgets;

import dev.aqua.client.Aqua;
import dev.aqua.client.hud.Anchor;
import dev.aqua.client.hud.HudWidget;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.Glass;
import dev.aqua.client.render.Icon;
import dev.aqua.client.render.R2D;
import dev.aqua.client.render.font.AquaFont;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.StringSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/** Counts configurable items in your own inventory (baseline "configurable item counts"). */
public final class ItemCounterWidget extends HudWidget {
    private final StringSetting items = add(new StringSetting("items", "Items (comma separated ids)",
            "minecraft:totem_of_undying, minecraft:ender_pearl, minecraft:golden_apple, minecraft:arrow", 400));
    private final BoolSetting hideZero = add(new BoolSetting("hide_zero", "Hide items you have none of", false));
    private final BoolSetting horizontal = add(new BoolSetting("horizontal", "Horizontal", false));

    private String parsedFrom;
    private List<Item> parsed = new ArrayList<>();

    public ItemCounterWidget() {
        super("item_counter", "Item Counter", "Counts chosen items in your inventory.", Icon.BOX, false, Anchor.BOTTOM_RIGHT, -6, -84,
                "count", "totems", "pearls", "arrows");
    }

    private List<Item> itemList() {
        if (!items.get().equals(parsedFrom)) {
            parsedFrom = items.get();
            parsed = new ArrayList<>();
            for (String s : parsedFrom.split(",")) {
                Identifier id = Identifier.tryParse(s.trim());
                if (id != null && Registries.ITEM.containsId(id)) {
                    parsed.add(Registries.ITEM.get(id));
                }
            }
        }
        return parsed;
    }

    private int count(Item item) {
        if (mc.player == null) {
            return 0;
        }
        int c = 0;
        var inv = mc.player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack st = inv.getStack(i);
            if (st.isOf(item)) {
                c += st.getCount();
            }
        }
        return c;
    }

    private List<Item> rows(boolean preview) {
        List<Item> out = new ArrayList<>();
        for (Item it : itemList()) {
            if (!hideZero.on() || preview || count(it) > 0) {
                out.add(it);
            }
        }
        return out;
    }

    @Override
    public boolean hasContent() {
        return mc.player != null && !rows(false).isEmpty();
    }

    @Override
    public float width(boolean preview) {
        int n = Math.max(1, rows(preview).size());
        return horizontal.on() ? n * 34 : 40;
    }

    @Override
    public float height(boolean preview) {
        int n = Math.max(1, rows(preview).size());
        return horizontal.on() ? 18 : n * 18;
    }

    @Override
    public void draw(DrawContext ctx, boolean preview, float alpha) {
        List<Item> r = rows(preview);
        for (int i = 0; i < r.size(); i++) {
            Item it = r.get(i);
            float x = horizontal.on() ? i * 34 : 0;
            float y = horizontal.on() ? 0 : i * 18;
            if (showBackground()) {
                Glass.hud(ctx, x, y + 1, 32, 16, 4, bg(alpha));
            }
            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().translate(x + 1, y + 1);
            ctx.drawItem(new ItemStack(it), 0, 0);
            ctx.getMatrices().popMatrix();
            int n = mc.player != null ? count(it) : 12;
            R2D.textMid(ctx, String.valueOf(n), x + 19, y + 9, 5.8f, AquaFont.Weight.SEMIBOLD,
                    Colors.fade(n == 0 ? Aqua.theme().muted() : Aqua.theme().text(), alpha));
        }
    }
}
