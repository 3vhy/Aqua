package dev.aqua.client.module.visual;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.setting.EnumSetting;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.jetbrains.annotations.Nullable;

/**
 * Water Colour (R6 "Realistic Water", adapted): recolours water surfaces. Only the colour changes:
 * underwater fog and how far you can see in water stay exactly as in vanilla.
 */
public final class WaterColour extends Module {
    public enum Preset implements EnumSetting.Displayable {
        TROPICAL("Tropical", 0xFF2FD4E0), DEEP("Deep ocean", 0xFF1E4FD0), LAGOON("Lagoon", 0xFF2FE0A8), THEME("Theme", 0), CUSTOM("Custom", 0);
        final String l;
        final int c;

        Preset(String l, int c) {
            this.l = l;
            this.c = c;
        }

        public String displayName() {
            return l;
        }
    }

    private final EnumSetting<Preset> preset = add(new EnumSetting<>("preset", "Colour", Preset.TROPICAL));
    private final ColorSetting custom = add(new ColorSetting("custom", "Custom colour", 0xFF3FA9F5, false, false)
            .visibleWhen(() -> preset.is(Preset.CUSTOM)));

    private static volatile int colour;
    private static boolean registered;

    public WaterColour() {
        super("water_colour", "Water Colour", "Recolours water (looks only; underwater visibility is unchanged).", Category.VISUALS, false,
                "water", "realistic water", "water colour", "water color", "ocean");
        preset.onChange(v -> refresh());
        custom.onChange(v -> refresh());
    }

    private int pick() {
        return switch (preset.get()) {
            case THEME -> dev.aqua.client.Aqua.theme().accent();
            case CUSTOM -> custom.color();
            default -> preset.get().c;
        };
    }

    private void refresh() {
        int next = isEnabled() ? (pick() & 0xFFFFFF) : 0;
        if (next != colour) {
            colour = next;
            NoBlockRotation.reloadChunks();
        }
    }

    @Override
    protected void onEnable() {
        register();
        refresh();
    }

    @Override
    protected void onDisable() {
        refresh();
    }

    /** Wraps the vanilla water renderers once; while off they return the normal biome colour. */
    private static void register() {
        if (registered) {
            return;
        }
        registered = true;
        for (var fluid : new net.minecraft.fluid.Fluid[]{Fluids.WATER, Fluids.FLOWING_WATER}) {
            FluidRenderHandler vanilla = FluidRenderHandlerRegistry.INSTANCE.get(fluid);
            if (vanilla == null) {
                continue;
            }
            FluidRenderHandlerRegistry.INSTANCE.register(fluid, new FluidRenderHandler() {
                @Override
                public net.minecraft.client.texture.Sprite[] getFluidSprites(@Nullable BlockRenderView view, @Nullable BlockPos pos, FluidState state) {
                    return vanilla.getFluidSprites(view, pos, state);
                }

                @Override
                public int getFluidColor(@Nullable BlockRenderView view, @Nullable BlockPos pos, FluidState state) {
                    int c = colour;
                    return c != 0 ? c : vanilla.getFluidColor(view, pos, state);
                }
            });
        }
    }
}
