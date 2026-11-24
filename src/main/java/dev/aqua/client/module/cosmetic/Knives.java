package dev.aqua.client.module.cosmetic;

import dev.aqua.client.Aqua;
import dev.aqua.client.cosmetic.Mesh;
import dev.aqua.client.cosmetic.knife.KnifeModels;
import dev.aqua.client.module.visual.Animations;
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
 * Knife and sword skins: draws a karambit, butterfly knife, bayonet, stiletto or fantasy sword in place of your sword in your own
 * hands (first and third person). Only the picture changes - the item, its damage, reach, cooldown and
 * hit detection are exactly the vanilla sword's, and other players still see a sword.
 */
public final class Knives extends Module {
    public enum Type implements EnumSetting.Displayable {
        KARAMBIT("Karambit", false), BUTTERFLY("Butterfly knife", false), BAYONET("M9 Bayonet", false), STILETTO("Stiletto", false),
        FROST("Frostbite", true), SUN("Sunforge", true), VOID("Voidrend", true), THORN("Thornheart", true);
        final String l;
        /** Fantasy swords keep their own colours; knives use the chosen finish. */
        public final boolean sword;

        Type(String l, boolean sword) {
            this.l = l;
            this.sword = sword;
        }

        public String displayName() {
            return l;
        }
    }

    public enum Finish implements EnumSetting.Displayable {
        EMERALD("Emerald"), CRIMSON("Crimson"), SILVER("Silver"), URBAN("Urban"), NIGHT("Night"), FADE("Aqua Fade"), GOLD("Gold"),
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
    private final EnumSetting<Finish> finish = add(new EnumSetting<>("finish", "Finish", Finish.EMERALD));
    private final EnumSetting<Applies> applies = add(new EnumSetting<>("applies", "Replace", Applies.SWORDS));
    private final NumberSetting size = add(new NumberSetting("size", "Size", 1.0, 0.7, 1.3, 0.05).unit("x"));

    public Knives() {
        super("knives", "Knife & Sword Skins", "Karambit, butterfly knife, bayonet, stiletto or a fantasy sword instead of your sword (looks only).",
                Category.COSMETICS, false, "knife", "karambit", "butterfly", "balisong", "bayonet", "stiletto", "sword skin", "3d swords",
                "cs:go", "csgo");
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

    public Finish finish() {
        return finish.get();
    }

    private KnifeModels.Finish finishColours() {
        Finish f = finish.get();
        return new KnifeModels.Finish() {
            @Override
            public int blade(float a) {
                return switch (f) {
                    case EMERALD -> Colors.lerp(0xFF0D6A39, 0xFF34E58F, 0.35f + 0.65f * a);
                    case CRIMSON -> Colors.lerp(0xFF6E0712, 0xFFE3263D, 0.3f + 0.7f * a);
                    case SILVER -> Colors.lerp(0xFFB9C4CE, 0xFFF4F7FA, a);
                    case URBAN -> {
                        // grey steel with rusty patches
                        float n = 0.5f + 0.5f * (float) Math.sin(a * 23f) * (float) Math.cos(a * 9f + 1.3f);
                        yield Colors.lerp(0xFF878C93, 0xFF6E5442, Math.max(0f, n - 0.35f) * 1.4f);
                    }
                    case NIGHT -> Colors.lerp(0xFF171A21, 0xFF394152, a);
                    case FADE -> a < 0.5f ? Colors.lerp(0xFF52E4F2, 0xFFB48CFF, a * 2) : Colors.lerp(0xFFB48CFF, 0xFFFF7FBF, (a - 0.5f) * 2);
                    case GOLD -> Colors.lerp(0xFFB07F1F, 0xFFFFE08A, a);
                    case THEME -> Colors.lerp(Aqua.theme().accent(), Aqua.theme().accent2(), a);
                };
            }

            @Override
            public int edge(float a) {
                return switch (f) {
                    case NIGHT -> 0xFF8A95A8;
                    case EMERALD -> 0xFFE4FFF0;
                    case CRIMSON -> 0xFFFFE4E8;
                    default -> 0xFFF4F7FA;
                };
            }

            @Override
            public int handle() {
                return switch (f) {
                    case EMERALD -> 0xFF15181C;
                    case CRIMSON -> 0xFF8E1622;
                    case SILVER -> 0xFFDCE2E7;
                    case URBAN -> 0xFF3B3F45;
                    case NIGHT -> 0xFF101216;
                    case FADE -> 0xFF1E2228;
                    case GOLD -> 0xFF2A1E12;
                    case THEME -> Colors.darken(Aqua.theme().accent(), 0.55f);
                };
            }

            @Override
            public int accent() {
                return switch (f) {
                    case EMERALD -> 0xFF2FE08A;
                    case CRIMSON -> 0xFFFF4A5E;
                    case SILVER -> 0xFFAEB8C2;
                    case URBAN -> 0xFF8C6A4E;
                    case NIGHT -> 0xFF5B6B8C;
                    case FADE -> 0xFF52E4F2;
                    case GOLD -> 0xFFFFD45C;
                    case THEME -> Aqua.theme().accent();
                };
            }

            @Override
            public int metal() {
                return switch (f) {
                    case SILVER -> 0xFFEEF2F5;
                    case NIGHT -> 0xFF4E5564;
                    case GOLD -> 0xFFE8B43A;
                    default -> 0xFFAEB8C2;
                };
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
    /** First person: knives are pulled in towards the screen centre and drawn a little smaller than a sword. */
    private static final float FP_X = -1.5f, FP_Y = 2.5f, FP_SCALE = 0.8f;
    private static final float KARAMBIT_ROT = 150f, KARAMBIT_DX = 1.5f, KARAMBIT_DY = 5.5f;

    private static boolean thirdPersonContext(ItemDisplayContext ctx) {
        return ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || ctx == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    }

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
        if (!thirdPersonContext(ctx)) {
            // First person: pull the knife in towards the centre of the screen so all of it shows.
            matrices.translate(FP_X / 16f, FP_Y / 16f, 0f);
        }
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-45f));
        float s = size.f() / 16f;
        Type ty = type.get();
        boolean thirdPerson = thirdPersonContext(ctx);
        if (thirdPerson) {
            s *= 1.3f;
        } else {
            s *= FP_SCALE;
        }
        switch (ty) {
            case KARAMBIT -> {
                // Reverse grip: ring on top, claw sweeping down and forward out of the fist.
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(KARAMBIT_ROT));
                matrices.translate(KARAMBIT_DX / 16f, KARAMBIT_DY / 16f, 0f);
                s *= 1.12f;
            }
            case BAYONET -> s *= 0.86f;
            case STILETTO -> s *= 0.9f;
            case FROST, SUN, VOID, THORN -> s *= 0.74f;
            default -> {
            }
        }
        matrices.scale(s, s, s);
        float lf = Mesh.lightFactor(light, 0f);
        KnifeModels.Finish fin = finishColours();
        float time = (System.currentTimeMillis() % 1_000_000L) / 1000f;
        float open = Animations.butterflyOpen();
        int aura = 0;
        dev.aqua.client.module.visual.HandAura ha = Aqua.modules().get(dev.aqua.client.module.visual.HandAura.class);
        if (ha != null && !thirdPerson) {
            aura = ha.outline(!left);
        }
        if (aura != 0) {
            // Hand Aura on a knife skin: a soft glowing shell drawn slightly larger behind the model.
            int glowColor = Colors.fade(aura, 0.45f);
            for (float k : new float[]{1.06f, 1.12f}) {
                matrices.push();
                matrices.scale(k, k, k);
                int c = k > 1.1f ? Colors.fade(aura, 0.22f) : glowColor;
                queue.submitCustom(matrices, FxLayers.GLOW, (entry, vc) -> drawModel(new Mesh(entry, vc, 1f).overrideColor(c), ty, fin, time, open, ctx));
                matrices.pop();
            }
        }
        queue.submitCustom(matrices, FxLayers.SOLID, (entry, vc) -> drawModel(new Mesh(entry, vc, lf), ty, fin, time, open, ctx));
        matrices.pop();
    }

    private static void drawModel(Mesh m, Type ty, KnifeModels.Finish fin, float time, float open, ItemDisplayContext ctx) {
        switch (ty) {
            case KARAMBIT -> KnifeModels.karambit(m, fin);
            case BUTTERFLY -> KnifeModels.butterfly(m, fin, ctx.isFirstPerson() ? open : 0f);
            case BAYONET -> KnifeModels.bayonet(m, fin);
            case STILETTO -> KnifeModels.stiletto(m, fin);
            case FROST -> KnifeModels.frostSword(m, time);
            case SUN -> KnifeModels.sunSword(m, time);
            case VOID -> KnifeModels.voidSword(m, time);
            case THORN -> KnifeModels.thornSword(m, time);
        }
    }
}
