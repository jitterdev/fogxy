package dev.fogxy.mixin;

import dev.fogxy.Fogxy;
import dev.fogxy.config.FogxySodiumConfig;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.caffeinemc.mods.sodium.client.gui.SodiumConfigBuilder", remap = false)
public class MixinSodiumConfigBuilderNew {
    @Inject(method = "buildFullConfig", at = @At("TAIL"), require = 0, remap = false)
    private void fogxy$addFogPage(ConfigBuilder builder, CallbackInfo ci) {
        try {
            FogxySodiumConfig.build(builder);
        } catch (Throwable t) {
            Fogxy.LOGGER.warn("Could not add the Fog options page to Sodium", t);
        }
    }
}
