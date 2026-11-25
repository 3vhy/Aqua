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
import dev.aqua.client.setting.EnumSetting;
import dev.aqua.client.theme.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Your own armour, held item and off-hand with durability (R3 0:02 hotbar-row layout). */
public final class ArmorWidget extends HudWidget {
    public enum Layout implements EnumSetting.Displayable {
        ROW("Row (boxes)"), COLUMN("Column with durability");
        final String l;

        Layout(String l) {
            this.l = l;
        }

        public String displayName() {
            return l;
        }
    }

    public enum Durability implements EnumSetting.Displayable {
        BAR("Bar"), PERCENT("Percent"), VALUE("Remaining"), NONE("Hidden");
        final String l;

        Durability(String l) {
            this.l = l;
        }

        public String displayName() {
            return l;
        }
    }

    private final EnumSetting<Layout> layout = add(new EnumSetting<>("layout", "Layout", Layout.ROW));
    private final EnumSetting<Durability> durability = add(new EnumSetting<>("durability", "Durability", Durability.BAR));
    private final BoolSetting held = add(new BoolSetting("held", "Held item", true));
    private final BoolSetting offhand = add(new BoolSetting("offhand", "Off-hand", true));
    private final BoolSetting hideEmpty = add(new BoolSetting("hide_empty", "Hide empty slots", true));

    private static final float BOX = 18f;
    private static final float GAP = 2f;

    public ArmorWidget() {
        super("armor", "Armor Status", "Armour, held item and durability.", Icon.SHIELD, true, Anchor.BOTTOM_CENTER, 150, -2,
                "armour", "durability", "equipment");
    }

    private List<ItemStack> items(boolean preview) {
        List<ItemStack> out = new ArrayList<>();
        if (mc.player == null) {
            if (preview) {
                out.add(new ItemStack(Items.DIAMOND_HELMET));
                out.add(new ItemStack(Items.DIAMOND_CHESTPLATE));
                out.add(new ItemStack(Items.DIAMOND_LEGGINGS));
                out.add(new ItemStack(Items.DIAMOND_BOOTS));
                out.add(new ItemStack(Items.DIAMOND_SWORD));
            }
            return out;
        }
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (EquipmentSlot s : slots) {
            ItemStack st = mc.player.getEquippedStack(s);
            if (!(hideEmpty.on() && st.isEmpty())) {
                out.add(st);
            }
        }
        if (held.on()) {
            ItemStack st = mc.player.getMainHandStack();
            if (!(hideEmpty.on() && st.isEmpty())) {
                out.add(st);
            }
        }
        if (offhand.on()) {
            ItemStack st = mc.player.getOffHandStack();
            if (!(hideEmpty.on() && st.isEmpty())) {
                out.add(st);
            }
        }
        return out;
    }

    @Override
    public boolean hasContent() {
        return mc.player != null && !items(false).isEmpty();
    }

    @Override
    public float width(boolean preview) {
        int n = Math.max(1, items(preview).size());
        if (layout.is(Layout.ROW)) {
            return n * BOX + (n - 1) * GAP;
        }
        return BOX + 36;
    }

    @Override
    public float height(boolean preview) {
        int n = Math.max(1, items(preview).size());
        if (layout.is(Layout.ROW)) {
            return BOX;
        }
        return n * BOX + (n - 1) * GAP;
    }

    private static String durText(ItemStack st, Durability mode) {
        if (!st.isDamageable()) {
            return st.getCount() > 1 ? "x" + st.getCount() : "";
        }
        int max = st.getMaxDamage();
        int left = max - st.getDamage();
        return mode == Durability.PERCENT ? Math.round(left * 100f / max) + "%" : String.valueOf(left);
    }

    @Override
    public void draw(DrawContext ctx, boolean preview, float alpha) {
        Theme t = Aqua.theme();
        List<ItemStack> list = items(preview);
        boolean row = layout.is(Layout.ROW);
        for (int i = 0; i < list.size(); i++) {
            ItemStack st = list.get(i);
            float x = row ? i * (BOX + GAP) : 0;
            float y = row ? 0 : i * (BOX + GAP);
            if (showBackground()) {
                Glass.hud(ctx, x, y, BOX, BOX, 3.5f, bg(alpha));
            }
            if (!st.isEmpty()) {
                ctx.getMatrices().pushMatrix();
                ctx.getMatrices().translate(x + 1, y + 1);
                ctx.drawItem(st, 0, 0);
                if (!st.isDamageable() && st.getCount() > 1) {
                    ctx.drawStackOverlay(mc.textRenderer, st, 0, 0);
                }
                ctx.getMatrices().popMatrix();
                if (st.isDamageable() && durability.get() != Durability.NONE) {
                    float frac = 1f - st.getDamage() / (float) st.getMaxDamage();
                    int col = Colors.lerp(0xFFFF5C5C, 0xFF3DDC84, frac);
                    if (durability.is(Durability.BAR) || row) {
                        R2D.round(ctx, x + 3, y + BOX - 3, BOX - 6, 1.3f, 0.65f, Colors.fade(0x66000000, alpha));
                        R2D.round(ctx, x + 3, y + BOX - 3, (BOX - 6) * frac, 1.3f, 0.65f, Colors.fade(col, alpha));
                    }
                    if (!row && !durability.is(Durability.BAR)) {
                        R2D.textMid(ctx, durText(st, durability.get()), x + BOX + 4, y + BOX / 2f, 5.6f,
                                AquaFont.Weight.SEMIBOLD, Colors.fade(col, alpha));
                    }
                }
                if (!row && !st.isDamageable() && st.getCount() > 1) {
                    R2D.textMid(ctx, "x" + st.getCount(), x + BOX + 4, y + BOX / 2f, 5.6f, AquaFont.Weight.SEMIBOLD,
                            Colors.fade(t.text(), alpha));
                }
            }
        }
    }
}
