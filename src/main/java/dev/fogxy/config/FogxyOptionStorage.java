//? if <1.21.11 {
package dev.fogxy.config;

import dev.fogxy.FogxyConfig;
import net.caffeinemc.mods.sodium.client.gui.options.storage.OptionStorage;

public final class FogxyOptionStorage implements OptionStorage<FogxyConfig> {
    @Override
    public FogxyConfig getData() {
        return FogxyConfig.CONFIG;
    }

    @Override
    public void save() {
        FogxyConfig.CONFIG.save();
    }
}
//?}
