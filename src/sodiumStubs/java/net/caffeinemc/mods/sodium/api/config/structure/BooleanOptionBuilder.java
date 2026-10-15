package net.caffeinemc.mods.sodium.api.config.structure;

import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface BooleanOptionBuilder extends StatefulOptionBuilder<Boolean> {
    @Override
    BooleanOptionBuilder setName(Component name);

    @Override
    BooleanOptionBuilder setTooltip(Component tooltip);

    @Override
    BooleanOptionBuilder setStorageHandler(StorageEventHandler storage);

    @Override
    BooleanOptionBuilder setImpact(OptionImpact impact);

    @Override
    BooleanOptionBuilder setDefaultValue(Boolean value);

    @Override
    BooleanOptionBuilder setBinding(Consumer<Boolean> save, Supplier<Boolean> load);
}
