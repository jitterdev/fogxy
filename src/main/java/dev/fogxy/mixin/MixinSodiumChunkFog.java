package dev.fogxy.mixin;

import dev.fogxy.fog.FogFrontier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >=26.2 {
/*@Mixin(targets = "net.caffeinemc.mods.sodium.client.gpu.device.context.GLDrawContext", remap = false)
public class MixinSodiumChunkFog {
    @Inject(method = "setContext", at = @At("TAIL"), remap = false)
    private void fogxy$uploadFrontier(CallbackInfo ci) {
        FogFrontier.bind();
    }
}
*///?} else {
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.shader.DefaultShaderInterface", remap = false)
public class MixinSodiumChunkFog {
    @Inject(method = "setupState", at = @At("TAIL"), remap = false)
    private void fogxy$uploadFrontier(CallbackInfo ci) {
        FogFrontier.bind();
    }
}
//?}
