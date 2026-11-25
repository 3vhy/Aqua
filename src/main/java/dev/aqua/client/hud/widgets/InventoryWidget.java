package dev.aqua.client.hud.widgets;

import dev.aqua.client.hud.Anchor;
import dev.aqua.client.hud.HudWidget;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.Glass;
import dev.aqua.client.render.Icon;
import dev.aqua.client.render.R2D;
import dev.aqua.client.setting.BoolSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/** Preview of your own main inventory (27 slots). */
public final class InventoryWidget extends HudWidget {
    private final BoolSetting slotBackgrounds = add(new BoolSetting("slots", "Slot backgrounds", true));
    private static final float S = 17f;

    public InventoryWidget() {
        super("inventory", "Inventory", "A preview of your inventory.", Icon.BOX, false, Anchor.MIDDLE_LEFT, 5, 75,
                "items", "storage", "backpack");
    }

    @Override
    public boolean hasContent() {
        return mc.player != null;
    }

    @Override
    public float width(boolean preview) {
        return 9 * S + 6;
    }

    @Override
    public float height(boolean preview) {
        return 3 * S + 6;
    }

    @Override
    public void draw(DrawContext ctx, boolean preview, float alpha) {
        if (showBackground()) {
            Glass.hud(ctx, 0, 0, width(preview), height(preview), 5, bg(alpha));
        }
        for (int i = 0; i < 27; i++) {
            float x = 3 + (i % 9) * S;
            float y = 3 + (i / 9) * S;
            if (slotBackgrounds.on()) {
                R2D.round(ctx, x + 0.5f, y + 0.5f, S - 1, S - 1, 2.5f, Colors.fade(0x22FFFFFF, alpha));
            }
            ItemStack st = mc.player != null ? mc.player.getInventory().getStack(9 + i)
                    : (preview && i % 4 == 0 ? new ItemStack(Items.OAK_LOG, 32) : ItemStack.EMPTY);
            if (!st.isEmpty()) {
                ctx.getMatrices().pushMatrix();
                ctx.getMatrices().translate(x + 0.5f, y + 0.5f);
                ctx.drawItem(st, 0, 0);
                ctx.drawStackOverlay(mc.textRenderer, st, 0, 0);
                ctx.getMatrices().popMatrix();
            }
        }
    }
}
