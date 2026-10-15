package dev.fogxy.mixin;

//? if <26.2 {
import dev.fogxy.fog.SodiumShaderPatcher;
import net.caffeinemc.mods.sodium.client.gl.shader.ShaderConstants;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//?}
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "net.caffeinemc.mods.sodium.client.gl.shader.ShaderParser", remap = false)
public class MixinSodiumShaderParser {
    //? if <26.2 {
    @Inject(
            method = "parseShader(Ljava/lang/String;Lnet/caffeinemc/mods/sodium/client/gl/shader/ShaderConstants;)Ljava/lang/String;",
            at = @At("RETURN"),
            cancellable = true,
            require = 0,
            remap = false
    )
    private static void fogxy$patchTerrainShader(String source, ShaderConstants constants,
                                                 CallbackInfoReturnable<String> cir) {
        String current = cir.getReturnValue();
        String patched = SodiumShaderPatcher.patchFragment(current);
        if (patched == null) {
            patched = SodiumShaderPatcher.patchVertex(current);
        }
        if (patched != null) {
            cir.setReturnValue(patched);
        }
    }
    //?}
}
