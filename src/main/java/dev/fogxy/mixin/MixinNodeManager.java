package dev.fogxy.mixin;

import dev.fogxy.fog.FogxySectionSource;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import me.cortex.voxy.client.core.rendering.hierachical.NodeStore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.LongConsumer;

@Mixin(targets = "me.cortex.voxy.client.core.rendering.hierachical.NodeManager", remap = false)
public abstract class MixinNodeManager implements FogxySectionSource {
    @Shadow
    private Long2IntOpenHashMap activeSectionMap;
    @Shadow
    private NodeStore nodeData;

    @Override
    public void fogxyCollectDrawnSections(LongConsumer out) {
        for (Long2IntMap.Entry entry : this.activeSectionMap.long2IntEntrySet()) {
            int value = entry.getIntValue();
            // High bits are node type; 0x80000000 is an unfulfilled request slot.
            if ((value & 0xC0000000) == 0x80000000) {
                continue;
            }
            // Geometry >= 0 has a mesh; -1 is null and -2 is an empty section.
            if (this.nodeData.getNodeGeometry(value & 0x00FFFFFF) >= 0) {
                out.accept(entry.getLongKey());
            }
        }
    }
}
