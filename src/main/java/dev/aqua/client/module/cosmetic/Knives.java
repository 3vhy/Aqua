package dev.aqua.client.module.cosmetic;

import dev.aqua.client.Aqua;
import dev.aqua.client.cosmetic.Mesh;
import dev.aqua.client.cosmetic.knife.KnifeModels;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.world.FxLayers;
import dev.aqua.client.setting.EnumSetting;
import dev.aqua.client.setting.NumberSetting;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;

/**
 * Knife skins: draws a karambit, katana, dagger or butterfly knife in place of your sword in your own
 * hands (first and third person). Only the picture changes - the item, its damage, reach, cooldown and
 * hit detection are exactly the vanilla sword's, and other players still see a sword.
 */
public final class Knives extends Module {
    public enum Type implements EnumSetting.Displayable {
        KARAMBIT("Karambit"), KATANA("Katana"), DAGGER("Dagger"), BUTTERFLY("Butterfly knife");
        final String l;

        Type(String l) {
            this.l = l;
        }

        public String displayName() {
            return l;
        }
    }

    public enum Finish implements EnumSetting.Displayable {
        FADE("Aqua Fade"), SAPPHIRE("Sapphire"), RUBY("Ruby"), EMERALD("Emerald"), GOLD("Gold"), OBSIDIAN("Obsidian"), STEEL("Steel"),
        THEME("Theme");
        final String l;

        Finish(String l) {
            this.l = l;
        }

        public String displayName() {
            return l;
        }
    }

    public enum Applies implements EnumSetting.Displayable {
        SWORDS("Swords"), SWORDS_AXES("Swords and axes");
        final String l;

        Applies(String l) {
            this.l = l;
        }

        public String displayName() {
            return l;
        }
    }

    private final EnumSetting<Type> type = add(new EnumSetting<>("type", "Knife", Type.KARAMBIT));
    private final EnumSetting<Finish> finish = add(new EnumSetting<>("finish", "Finish", Finish.FADE));
    private final EnumSetting<Applies> applies = add(new EnumSetting<>("applies", "Replace", Applies.SWORDS));
    private final NumberSetting size = add(new NumberSetting("size", "Size", 1.0, 0.7, 1.3, 0.05).unit("x"));

    public Knives() {
        super("knives", "Knife Skins", "Karambit, katana, dagger or butterfly knife instead of your sword (looks only).",
                Category.COSMETICS, false, "knife", "karambit", "katana", "dagger", "butterfly", "balisong", "sword skin");
    }

    public Type type() {
        return type.get();
    }

    public void select(Type t) {
        type.set(t);
    }

    public boolean appliesTo(ItemStack stack) {
        if (!isEnabled() || stack.isEmpty()) {
            return false;
        }
        return stack.isIn(ItemTags.SWORDS) || (applies.is(Applies.SWORDS_AXES) && stack.isIn(ItemTags.AXES));
    }

    private KnifeModels.Finish finishColours() {
        Finish f = finish.get();
        return new KnifeModels.Finish() {
            @Override
            public int blade(float a) {
                return switch (f) {
                    case FADE -> a < 0.5f ? Colors.lerp(0xFF52E4F2, 0xFFB48CFF, a * 2) : Colors.lerp(0xFFB48CFF, 0xFFFF7FBF, (a - 0.5f) * 2);
                    case SAPPHIRE -> Colors.lerp(0xFF0E3B8C, 0xFF3FA3FF, 0.5f + 0.5f * (float) Math.sin(a * 9));
                    case RUBY -> Colors.lerp(0xFF6E0712, 0xFFE0263C, 0.5f + 0.5f * (float) Math.sin(a * 9));
                    case EMERALD -> Colors.lerp(0xFF0B5A33, 0xFF3BE08A, 0.5f + 0.5f * (float) Math.sin(a * 9));
                    case GOLD -> Colors.lerp(0xFFB07F1F, 0xFFFFE08A, a);
                    case OBSIDIAN -> Colors.lerp(0xFF121418, 0xFF2E3340, a);
                    case STEEL -> Colors.lerp(0xFF8E99A4, 0xFFDDE3E8, a);
                    case THEME -> Colors.lerp(Aqua.theme().accent(), Aqua.theme().accent2(), a);
                };
            }

            @Override
            public int edge(float a) {
                return f == Finish.OBSIDIAN ? 0xFF5A6070 : 0xFFF2F6F9;
            }
        };
    }

    /** Vanilla "handheld" display transforms (translation in pixels, rotation in degrees). */
    private static float[] transform(ItemDisplayContext ctx) {
        return switch (ctx) {
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND -> new float[]{1.13f, 3.2f, 1.13f, 0f, -90f, 25f, 0.68f};
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> new float[]{0f, 4f, 0.5f, 0f, -90f, 55f, 0.85f};
            default -> new float[]{0f, 0f, 0f, 0f, 0f, 0f, 1f};
        };
    }

    /** Draws the knife where the vanilla item would be drawn. */
    /** How many knives have been submitted for drawing (used by the in-game tests). */
    public static long drawn() {
        return drawn;
    }

    private static long drawn;

    public void render(MatrixStack matrices, OrderedRenderCommandQueue queue, ItemDisplayContext ctx, int light) {
        drawn++;
        float[] t = transform(ctx);
        boolean left = ctx.isLeftHand();
        matrices.push();
        matrices.translate((left ? -t[0] : t[0]) / 16f, t[1] / 16f, t[2] / 16f);
        matrices.multiply(new Quaternionf().rotationXYZ((float) Math.toRadians(t[3]), (float) Math.toRadians(left ? -t[4] : t[4]),
                (float) Math.toRadians(left ? -t[5] : t[5])));
        matrices.scale(t[6], t[6], t[6]);
        matrices.translate(-0.5f, -0.5f, -0.5f);
        // Sword sprites run diagonally from the grip (bottom left) to the tip (top right).
        matrices.translate(6f / 16f, 6f / 16f, 0.5f);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-45f));
        float s = size.f() / 16f;
        Type ty = type.get();
        boolean thirdPerson = ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || ctx == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        if (thirdPerson) {
            s *= 1.3f;
        }
        if (ty == Type.KATANA) {
            s *= 0.78f;
        } else if (ty == Type.KARAMBIT) {
            // Hold the claw so it curves upwards out of the fist.
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(35f));
            matrices.translate(0f, 1.5f / 16f, 0f);
        }
        matrices.scale(s, s, s);
        float lf = Mesh.lightFactor(light, 0f);
        KnifeModels.Finish fin = finishColours();
        queue.submitCustom(matrices, FxLayers.SOLID, (entry, vc) -> {
            Mesh m = new Mesh(entry, vc, lf);
            switch (ty) {
                case KARAMBIT -> KnifeModels.karambit(m, fin, 0f);
                case KATANA -> KnifeModels.katana(m, fin);
                case DAGGER -> KnifeModels.dagger(m, fin);
                case BUTTERFLY -> KnifeModels.butterfly(m, fin);
            }
        });
        matrices.pop();
    }
}
