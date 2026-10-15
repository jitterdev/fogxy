//? if <1.21.11 {
package dev.fogxy.config;

import com.google.common.collect.ImmutableList;
import dev.fogxy.FogxyConfig;
import net.caffeinemc.mods.sodium.client.gui.options.OptionGroup;
import net.caffeinemc.mods.sodium.client.gui.options.OptionImpact;
import net.caffeinemc.mods.sodium.client.gui.options.OptionImpl;
import net.caffeinemc.mods.sodium.client.gui.options.OptionPage;
import net.caffeinemc.mods.sodium.client.gui.options.control.SliderControl;
import net.caffeinemc.mods.sodium.client.gui.options.control.TickBoxControl;
import net.caffeinemc.mods.sodium.client.gui.options.storage.OptionStorage;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class FogxyConfigScreenPages {
    private FogxyConfigScreenPages() {
    }

    public static OptionPage page() {
        OptionStorage<FogxyConfig> storage = new FogxyOptionStorage();
        List<OptionGroup> groups = new ArrayList<>();

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(boolean.class, storage)
                        .setName(Component.translatable("fogxy.config.enabled"))
                        .setTooltip(Component.translatable("fogxy.config.enabled.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((config, value) -> config.enabled = value, config -> config.enabled)
                        .setImpact(OptionImpact.LOW)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, storage)
                        .setName(Component.translatable("fogxy.config.debug_gradient"))
                        .setTooltip(Component.translatable("fogxy.config.debug_gradient.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((config, value) -> config.debugGradient = value, config -> config.debugGradient)
                        .setImpact(OptionImpact.LOW)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, storage)
                        .setName(Component.translatable("fogxy.config.debug_edges"))
                        .setTooltip(Component.translatable("fogxy.config.debug_edges.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((config, value) -> config.debugEdges = value, config -> config.debugEdges)
                        .setImpact(OptionImpact.LOW)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, storage)
                        .setName(Component.translatable("fogxy.config.hole_fill"))
                        .setTooltip(Component.translatable("fogxy.config.hole_fill.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((config, value) -> config.holeFill = value, config -> config.holeFill)
                        .setImpact(OptionImpact.LOW)
                        .build())
                .add(OptionImpl.createBuilder(int.class, storage)
                        .setName(Component.translatable("fogxy.config.fill_top"))
                        .setTooltip(Component.translatable("fogxy.config.fill_top.tooltip"))
                        .setControl(option -> new SliderControl(option, -64, 384, 8,
                                value -> Component.literal(value + " blocks")))
                        .setBinding((config, value) -> config.fillTop = value, config -> config.fillTop)
                        .setImpact(OptionImpact.LOW)
                        .build())
                .build());

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(boolean.class, storage)
                        .setName(Component.translatable("fogxy.config.sky_fog"))
                        .setTooltip(Component.translatable("fogxy.config.sky_fog.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((config, value) -> config.skyFog = value, config -> config.skyFog)
                        .setImpact(OptionImpact.LOW)
                        .build())
                .build());

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(int.class, storage)
                        .setName(Component.translatable("fogxy.config.fog_start"))
                        .setTooltip(Component.translatable("fogxy.config.fog_start.tooltip"))
                        .setControl(option -> new SliderControl(option, 0, 100, 5,
                                value -> Component.literal(value + "%")))
                        .setBinding((config, value) -> config.fogStart = value / 100.0f,
                                config -> Math.round(config.fogStart * 100.0f))
                        .setImpact(OptionImpact.LOW)
                        .build())
                .add(OptionImpl.createBuilder(int.class, storage)
                        .setName(Component.translatable("fogxy.config.fog_end"))
                        .setTooltip(Component.translatable("fogxy.config.fog_end.tooltip"))
                        .setControl(option -> new SliderControl(option, 5, 100, 5,
                                value -> Component.literal(value + "%")))
                        .setBinding((config, value) -> config.fogEnd = value / 100.0f,
                                config -> Math.round(config.fogEnd * 100.0f))
                        .setImpact(OptionImpact.LOW)
                        .build())
                .add(OptionImpl.createBuilder(int.class, storage)
                        .setName(Component.translatable("fogxy.config.smooth"))
                        .setTooltip(Component.translatable("fogxy.config.smooth.tooltip"))
                        .setControl(option -> new SliderControl(option, 0, 48, 4,
                                value -> value == 0 ? Component.literal("Off") : Component.literal(value + " rays")))
                        .setBinding((config, value) -> config.smooth = value, config -> config.smooth)
                        .setImpact(OptionImpact.LOW)
                        .build())
                .add(OptionImpl.createBuilder(int.class, storage)
                        .setName(Component.translatable("fogxy.config.slope"))
                        .setTooltip(Component.translatable("fogxy.config.slope.tooltip"))
                        .setControl(option -> new SliderControl(option, 0, 128, 4,
                                value -> value == 0 ? Component.literal("Off") : Component.literal(value + " blocks/ray")))
                        .setBinding((config, value) -> config.slope = value, config -> config.slope)
                        .setImpact(OptionImpact.LOW)
                        .build())
                .add(OptionImpl.createBuilder(int.class, storage)
                        .setName(Component.translatable("fogxy.config.hole_reach"))
                        .setTooltip(Component.translatable("fogxy.config.hole_reach.tooltip"))
                        .setControl(option -> new SliderControl(option, 0, 512, 16,
                                value -> Component.literal(value + " blocks")))
                        .setBinding((config, value) -> config.holeReach = value, config -> config.holeReach)
                        .setImpact(OptionImpact.LOW)
                        .build())
                .add(OptionImpl.createBuilder(int.class, storage)
                        .setName(Component.translatable("fogxy.config.hole_fade"))
                        .setTooltip(Component.translatable("fogxy.config.hole_fade.tooltip"))
                        .setControl(option -> new SliderControl(option, 0, 1024, 32,
                                value -> value == 0 ? Component.literal("Hard edge") : Component.literal(value + " blocks")))
                        .setBinding((config, value) -> config.holeFade = value, config -> config.holeFade)
                        .setImpact(OptionImpact.LOW)
                        .build())
                .add(OptionImpl.createBuilder(int.class, storage)
                        .setName(Component.translatable("fogxy.config.levels"))
                        .setTooltip(Component.translatable("fogxy.config.levels.tooltip"))
                        .setControl(option -> new SliderControl(option, 1, 5, 1,
                                value -> Component.literal(value == 1 ? "Finest only" : "Finest " + value)))
                        .setBinding((config, value) -> config.levels = value, config -> config.levels)
                        .setImpact(OptionImpact.LOW)
                        .build())
                .build());

        return new OptionPage(Component.translatable("fogxy.config.title"), ImmutableList.copyOf(groups));
    }
}
//?}
