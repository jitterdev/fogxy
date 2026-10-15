package dev.fogxy.mixin;

import dev.fogxy.fog.FogxyShaderPatcher;
import me.cortex.voxy.client.core.gl.shader.ShaderLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShaderLoader.class)
public class MixinVoxyShaderLoader {
    @Inject(method = "parse", at = @At("RETURN"), cancellable = true, remap = false)
    private static void fogxy$patchFogShader(String id, CallbackInfoReturnable<String> cir) {
        if (!FogxyShaderPatcher.isFogShader(id)) {
            return;
        }
        String patched = FogxyShaderPatcher.patch(cir.getReturnValue());
        if (patched != null) {
            cir.setReturnValue(patched);
        }
    }
}
