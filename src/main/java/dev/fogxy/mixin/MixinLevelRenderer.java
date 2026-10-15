package dev.fogxy.mixin;

import dev.fogxy.fog.FogFrontier;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {
    //? if >=26.2 {
    /*@Inject(method = "render", at = @At("HEAD"))
    *///?} else {
    @Inject(method = "renderLevel", at = @At("HEAD"))
    //?}
    private void fogxy$tickFrontier(CallbackInfo ci) {
        FogFrontier.tick();
    }
}
