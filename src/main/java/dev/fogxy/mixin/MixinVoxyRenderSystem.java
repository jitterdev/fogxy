package dev.fogxy.mixin;

import dev.fogxy.fog.FogxySectionSource;
import me.cortex.voxy.client.core.rendering.hierachical.AsyncNodeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.LongConsumer;

@Mixin(targets = "me.cortex.voxy.client.core.VoxyRenderSystem", remap = false)
public abstract class MixinVoxyRenderSystem implements FogxySectionSource {
    @Shadow
    private AsyncNodeManager nodeManager;

    @Override
    public void fogxyCollectDrawnSections(LongConsumer out) {
        if (this.nodeManager instanceof FogxySectionSource source) {
            source.fogxyCollectDrawnSections(out);
        }
    }
}
