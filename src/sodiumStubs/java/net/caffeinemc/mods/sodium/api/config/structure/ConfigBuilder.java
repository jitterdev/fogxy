package net.caffeinemc.mods.sodium.api.config.structure;

import net.minecraft.resources.ResourceLocation;

public interface ConfigBuilder {
    OptionPageBuilder createOptionPage();

    OptionGroupBuilder createOptionGroup();

    BooleanOptionBuilder createBooleanOption(ResourceLocation id);

    IntegerOptionBuilder createIntegerOption(ResourceLocation id);

    ModOptionsBuilder registerModOptions(String configId, String name, String version);
}
