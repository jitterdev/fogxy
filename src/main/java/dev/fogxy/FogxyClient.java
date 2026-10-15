package dev.fogxy;

import net.fabricmc.api.ClientModInitializer;

public final class FogxyClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FogxyConfig.load();
    }
}
