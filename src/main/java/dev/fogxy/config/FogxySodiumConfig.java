package dev.fogxy.config;

import dev.fogxy.FogxyConfig;
import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.option.ControlValueFormatter;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionPageBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class FogxySodiumConfig {
    private static final StorageEventHandler STORAGE = () -> FogxyConfig.CONFIG.save();

    private FogxySodiumConfig() {
    }

    public static void build(ConfigBuilder builder) {
        OptionPageBuilder page = builder.createOptionPage()
                .setName(Component.translatable("fogxy.config.title"));

        page.addOptionGroup(builder.createOptionGroup()
                .addOption(builder.createBooleanOption(id("enabled"))
                        .setName(Component.translatable("fogxy.config.enabled"))
                        .setTooltip(Component.translatable("fogxy.config.enabled.tooltip"))
                        .setDefaultValue(true)
                        .setStorageHandler(STORAGE)
                        .setBinding(value -> FogxyConfig.CONFIG.enabled = value, () -> FogxyConfig.CONFIG.enabled)
                        .setImpact(OptionImpact.LOW))
                .addOption(builder.createBooleanOption(id("debug_gradient"))
                        .setName(Component.translatable("fogxy.config.debug_gradient"))
                        .setTooltip(Component.translatable("fogxy.config.debug_gradient.tooltip"))
                        .setDefaultValue(false)
                        .setStorageHandler(STORAGE)
                        .setBinding(value -> FogxyConfig.CONFIG.debugGradient = value, () -> FogxyConfig.CONFIG.debugGradient)
                        .setImpact(OptionImpact.LOW))
                .addOption(builder.createBooleanOption(id("debug_edges"))
                        .setName(Component.translatable("fogxy.config.debug_edges"))
                        .setTooltip(Component.translatable("fogxy.config.debug_edges.tooltip"))
                        .setDefaultValue(false)
                        .setStorageHandler(STORAGE)
                        .setBinding(value -> FogxyConfig.CONFIG.debugEdges = value, () -> FogxyConfig.CONFIG.debugEdges)
                        .setImpact(OptionImpact.LOW))
                .addOption(builder.createBooleanOption(id("hole_fill"))
                        .setName(Component.translatable("fogxy.config.hole_fill"))
                        .setTooltip(Component.translatable("fogxy.config.hole_fill.tooltip"))
                        .setDefaultValue(true)
                        .setStorageHandler(STORAGE)
                        .setBinding(value -> FogxyConfig.CONFIG.holeFill = value, () -> FogxyConfig.CONFIG.holeFill)
                        .setImpact(OptionImpact.LOW))
                .addOption(builder.createIntegerOption(id("fill_top"))
                        .setName(Component.translatable("fogxy.config.fill_top"))
                        .setTooltip(Component.translatable("fogxy.config.fill_top.tooltip"))
                        .setDefaultValue(96)
                        .setRange(-64, 384, 8)
                        .setValueFormatter(unit(" blocks"))
                        .setStorageHandler(STORAGE)
                        .setBinding(value -> FogxyConfig.CONFIG.fillTop = value, () -> FogxyConfig.CONFIG.fillTop)
                        .setImpact(OptionImpact.LOW)));

        page.addOptionGroup(builder.createOptionGroup()
                .addOption(builder.createBooleanOption(id("sky_fog"))
                        .setName(Component.translatable("fogxy.config.sky_fog"))
                        .setTooltip(Component.translatable("fogxy.config.sky_fog.tooltip"))
                        .setDefaultValue(false)
                        .setStorageHandler(STORAGE)
                        .setBinding(value -> FogxyConfig.CONFIG.skyFog = value, () -> FogxyConfig.CONFIG.skyFog)
                        .setImpact(OptionImpact.LOW)));

        page.addOptionGroup(builder.createOptionGroup()
                .addOption(builder.createIntegerOption(id("fog_start"))
                        .setName(Component.translatable("fogxy.config.fog_start"))
                        .setTooltip(Component.translatable("fogxy.config.fog_start.tooltip"))
                        .setDefaultValue(50)
                        .setRange(0, 100, 5)
                        .setValueFormatter(percent())
                        .setStorageHandler(STORAGE)
                        .setBinding(value -> FogxyConfig.CONFIG.fogStart = value / 100.0f,
                                () -> Math.round(FogxyConfig.CONFIG.fogStart * 100.0f))
                        .setImpact(OptionImpact.LOW))
                .addOption(builder.createIntegerOption(id("fog_end"))
                        .setName(Component.translatable("fogxy.config.fog_end"))
                        .setTooltip(Component.translatable("fogxy.config.fog_end.tooltip"))
                        .setDefaultValue(100)
                        .setRange(5, 100, 5)
                        .setValueFormatter(percent())
                        .setStorageHandler(STORAGE)
                        .setBinding(value -> FogxyConfig.CONFIG.fogEnd = value / 100.0f,
                                () -> Math.round(FogxyConfig.CONFIG.fogEnd * 100.0f))
                        .setImpact(OptionImpact.LOW))
                .addOption(builder.createIntegerOption(id("smooth"))
                        .setName(Component.translatable("fogxy.config.smooth"))
                        .setTooltip(Component.translatable("fogxy.config.smooth.tooltip"))
                        .setDefaultValue(16)
                        .setRange(0, 48, 4)
                        .setValueFormatter(offOrUnit(" rays"))
                        .setStorageHandler(STORAGE)
                        .setBinding(value -> FogxyConfig.CONFIG.smooth = value, () -> FogxyConfig.CONFIG.smooth)
                        .setImpact(OptionImpact.LOW))
                .addOption(builder.createIntegerOption(id("slope"))
                        .setName(Component.translatable("fogxy.config.slope"))
                        .setTooltip(Component.translatable("fogxy.config.slope.tooltip"))
                        .setDefaultValue(64)
                        .setRange(0, 128, 4)
                        .setValueFormatter(offOrUnit(" blocks/ray"))
                        .setStorageHandler(STORAGE)
                        .setBinding(value -> FogxyConfig.CONFIG.slope = value, () -> FogxyConfig.CONFIG.slope)
                        .setImpact(OptionImpact.LOW))
                .addOption(builder.createIntegerOption(id("hole_reach"))
                        .setName(Component.translatable("fogxy.config.hole_reach"))
                        .setTooltip(Component.translatable("fogxy.config.hole_reach.tooltip"))
                        .setDefaultValue(64)
                        .setRange(0, 512, 16)
                        .setValueFormatter(unit(" blocks"))
                        .setStorageHandler(STORAGE)
                        .setBinding(value -> FogxyConfig.CONFIG.holeReach = value, () -> FogxyConfig.CONFIG.holeReach)
                        .setImpact(OptionImpact.LOW))
                .addOption(builder.createIntegerOption(id("hole_fade"))
                        .setName(Component.translatable("fogxy.config.hole_fade"))
                        .setTooltip(Component.translatable("fogxy.config.hole_fade.tooltip"))
                        .setDefaultValue(128)
                        .setRange(0, 1024, 32)
                        .setValueFormatter(value -> value == 0
                                ? Component.literal("Hard edge")
                                : Component.literal(value + " blocks"))
                        .setStorageHandler(STORAGE)
                        .setBinding(value -> FogxyConfig.CONFIG.holeFade = value, () -> FogxyConfig.CONFIG.holeFade)
                        .setImpact(OptionImpact.LOW))
                .addOption(builder.createIntegerOption(id("levels"))
                        .setName(Component.translatable("fogxy.config.levels"))
                        .setTooltip(Component.translatable("fogxy.config.levels.tooltip"))
                        .setDefaultValue(2)
                        .setRange(1, 5, 1)
                        .setValueFormatter(value -> Component.literal(value == 1 ? "Finest only" : "Finest " + value))
                        .setStorageHandler(STORAGE)
                        .setBinding(value -> FogxyConfig.CONFIG.levels = value, () -> FogxyConfig.CONFIG.levels)
                        .setImpact(OptionImpact.LOW)));

        builder.registerModOptions("fogxy", "Fogxy", version()).addPage(page);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("fogxy", path);
    }

    private static ControlValueFormatter percent() {
        return value -> Component.literal(value + "%");
    }

    private static ControlValueFormatter unit(String suffix) {
        return value -> Component.literal(value + suffix);
    }

    private static ControlValueFormatter offOrUnit(String suffix) {
        return value -> value == 0 ? Component.literal("Off") : Component.literal(value + suffix);
    }

    private static String version() {
        return FabricLoader.getInstance().getModContainer("fogxy")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("0.1.0-alpha");
    }
}
