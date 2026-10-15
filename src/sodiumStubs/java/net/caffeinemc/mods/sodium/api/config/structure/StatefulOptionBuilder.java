package net.caffeinemc.mods.sodium.api.config.structure;

import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface StatefulOptionBuilder<V> extends OptionBuilder {
    StatefulOptionBuilder<V> setTooltip(Component tooltip);

    StatefulOptionBuilder<V> setStorageHandler(StorageEventHandler storage);

    StatefulOptionBuilder<V> setImpact(OptionImpact impact);

    StatefulOptionBuilder<V> setDefaultValue(V value);

    StatefulOptionBuilder<V> setBinding(Consumer<V> save, Supplier<V> load);
}
