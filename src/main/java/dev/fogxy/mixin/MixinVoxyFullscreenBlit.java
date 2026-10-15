package dev.fogxy.mixin;

import dev.fogxy.fog.FogFrontier;
import me.cortex.voxy.client.core.rendering.post.FullscreenBlit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FullscreenBlit.class)
public class MixinVoxyFullscreenBlit {
    @Inject(
            method = "blit",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/cortex/voxy/client/core/gl/shader/Shader;bind()V",
                    shift = At.Shift.AFTER,
                    remap = false
            ),
            remap = false
    )
    private void fogxy$uploadFrontier(CallbackInfo ci) {
        FogFrontier.bind();
    }
}
