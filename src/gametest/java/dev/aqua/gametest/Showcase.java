package dev.aqua.gametest;

import dev.aqua.client.Aqua;
import dev.aqua.client.cosmetic.character.Characters;
import dev.aqua.client.module.Module;
import dev.aqua.client.module.cosmetic.CharacterSkin;
import dev.aqua.client.module.cosmetic.Knives;
import dev.aqua.client.setting.EnumSetting;
import dev.aqua.client.setting.Setting;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.option.Perspective;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.function.Consumer;

/** Renders every new cosmetic for visual review (AQUA_SHOWCASE=1). */
@SuppressWarnings({"UnstableApiUsage", "unchecked", "rawtypes"})
final class Showcase {
    private Showcase() {}

    static Setting<?> find(String module, String id) {
        for (Setting<?> s : Aqua.modules().byId(module).settings()) {
            if (s.id().equals(id)) {
                return s;
            }
        }
        throw new IllegalStateException(module + "." + id);
    }

    static void run(ClientGameTestContext context, Consumer<String> log) {
        context.runOnClient(mc -> mc.options.getMaxFps().setValue(30));
        context.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext sp = AquaClientGameTest.createWorldFor(context)) {
            sp.getServer().runCommand("time set noon");
            sp.getServer().runCommand("gamerule doDaylightCycle false");
            sp.getServer().runCommand("item replace entity @a weapon.mainhand with minecraft:diamond_sword");
            context.waitTicks(80);
            context.runOnClient(mc -> {
                for (Module m : Aqua.modules().all()) {
                    if (m.category() == dev.aqua.client.module.Category.HUD) {
                        m.setEnabled(false, false);
                    }
                }
                mc.options.setPerspective(Perspective.THIRD_PERSON_FRONT);
                mc.player.setPitch(10f);
            });
            context.runOnClient(mc -> Aqua.modules().byId("character_skin").setEnabled(true, false));
            for (Characters c : Characters.values()) {
                context.runOnClient(mc -> Aqua.modules().get(CharacterSkin.class).select(c));
                context.waitTicks(6);
                context.takeScreenshot("sc_char_" + c.name().toLowerCase());
            }
            context.runOnClient(mc -> Aqua.modules().byId("character_skin").setEnabled(false, false));
            for (String w : new String[]{"ANGEL", "DEVIL", "BUTTERFLY", "CRYSTAL", "FEATHER"}) {
                context.runOnClient(mc -> {
                    Aqua.modules().byId("wings").setEnabled(true, false);
                    EnumSetting e = (EnumSetting) find("wings", "style");
                    e.fromJson(new com.google.gson.JsonPrimitive(w));
                    mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
                });
                context.waitTicks(6);
                context.takeScreenshot("sc_wings_" + w.toLowerCase());
            }
            context.runOnClient(mc -> {
                Aqua.modules().byId("wings").setEnabled(false, false);
                Aqua.modules().byId("cape").setEnabled(true, false);
            });
            for (String c : new String[]{"AQUA", "GALAXY", "TOPOGRAPHIC"}) {
                context.runOnClient(mc -> ((EnumSetting) find("cape", "style")).fromJson(new com.google.gson.JsonPrimitive(c)));
                context.waitTicks(6);
                context.takeScreenshot("sc_cape_" + c.toLowerCase());
            }
            context.runOnClient(mc -> {
                Aqua.modules().byId("cape").setEnabled(false, false);
                Aqua.modules().byId("hats").setEnabled(true, false);
                mc.options.setPerspective(Perspective.THIRD_PERSON_FRONT);
            });
            for (String h : new String[]{"GLOW_CONE", "TOP_HAT", "CROWN", "WITCH", "HORNS", "CAT_EARS"}) {
                context.runOnClient(mc -> ((EnumSetting) find("hats", "style")).fromJson(new com.google.gson.JsonPrimitive(h)));
                context.waitTicks(6);
                context.takeScreenshot("sc_hat_" + h.toLowerCase());
            }
            context.runOnClient(mc -> {
                Aqua.modules().byId("hats").setEnabled(false, false);
                Aqua.modules().byId("knives").setEnabled(true, false);
                mc.options.setPerspective(Perspective.FIRST_PERSON);
                mc.player.setPitch(0f);
            });
            for (Knives.Type k : Knives.Type.values()) {
                context.runOnClient(mc -> Aqua.modules().get(Knives.class).select(k));
                context.waitTicks(6);
                context.takeScreenshot("sc_knife_" + k.name().toLowerCase());
            }
            context.runOnClient(mc -> mc.options.setPerspective(Perspective.THIRD_PERSON_FRONT));
            context.waitTicks(6);
            context.takeScreenshot("sc_knife_third_person");
            log.accept("Showcase rendered");
            context.runOnClient(mc -> mc.player.getInventory().setStack(0, ItemStack.EMPTY));
        }
    }

    static ItemStack sword() {
        return new ItemStack(Items.DIAMOND_SWORD);
    }
}
