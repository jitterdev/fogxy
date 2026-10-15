package dev.fogxy.mixin;

import dev.fogxy.fog.FogxySectionSource;
import me.cortex.voxy.client.core.rendering.hierachical.NodeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.LongConsumer;

@Mixin(targets = "me.cortex.voxy.client.core.rendering.hierachical.AsyncNodeManager", remap = false)
public abstract class MixinAsyncNodeManager implements FogxySectionSource {
    @Shadow
    private NodeManager manager;

    @Override
    public void fogxyCollectDrawnSections(LongConsumer out) {
        if (this.manager instanceof FogxySectionSource source) {
            source.fogxyCollectDrawnSections(out);
        }
    }
}
