package net.caffeinemc.mods.sodium.api.config.structure;

import net.minecraft.network.chat.Component;

public interface OptionBuilder {
    OptionBuilder setName(Component name);

    OptionBuilder setTooltip(Component tooltip);

    OptionBuilder setEnabled(boolean available);
}
