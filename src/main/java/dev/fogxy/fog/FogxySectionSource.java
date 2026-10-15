package dev.fogxy.fog;

import java.util.function.LongConsumer;

public interface FogxySectionSource {
    void fogxyCollectDrawnSections(LongConsumer out);
}
