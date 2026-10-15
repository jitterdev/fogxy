package net.caffeinemc.mods.sodium.api.config.structure;

import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.option.ControlValueFormatter;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface IntegerOptionBuilder extends StatefulOptionBuilder<Integer> {
    @Override
    IntegerOptionBuilder setName(Component name);

    @Override
    IntegerOptionBuilder setTooltip(Component tooltip);

    @Override
    IntegerOptionBuilder setStorageHandler(StorageEventHandler storage);

    @Override
    IntegerOptionBuilder setImpact(OptionImpact impact);

    @Override
    IntegerOptionBuilder setDefaultValue(Integer value);

    @Override
    IntegerOptionBuilder setBinding(Consumer<Integer> save, Supplier<Integer> load);

    IntegerOptionBuilder setRange(int min, int max, int step);

    IntegerOptionBuilder setValueFormatter(ControlValueFormatter formatter);
}
