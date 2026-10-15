package dev.fogxy.mixin;

import dev.fogxy.fog.SodiumShaderPatcher;
//? if <26.2 {
import dev.fogxy.fog.SodiumParsedShader;
import net.caffeinemc.mods.sodium.client.gl.shader.ShaderConstants;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//? if >=26.2 {
/*@Mixin(targets = "com.mojang.blaze3d.preprocessor.GlslPreprocessor")
public class MixinSodiumShaderParserNew {
    @Inject(method = "injectDefines", at = @At("RETURN"), cancellable = true)
    private static void fogxy$patchTerrainShader(CallbackInfoReturnable<String> cir) {
        String current = cir.getReturnValue();
        String patched = SodiumShaderPatcher.patchFragment(current);
        if (patched == null) {
            patched = SodiumShaderPatcher.patchVertex(current);
        }
        if (patched != null) {
            cir.setReturnValue(patched);
        }
    }
}
*///?} else {
@Mixin(targets = "net.caffeinemc.mods.sodium.client.gl.shader.ShaderParser", remap = false)
public class MixinSodiumShaderParserNew {
    @Inject(
            method = "parseShader(Ljava/lang/String;Lnet/caffeinemc/mods/sodium/client/gl/shader/ShaderConstants;)Lnet/caffeinemc/mods/sodium/client/gl/shader/ShaderParser$ParsedShader;",
            at = @At("RETURN"),
            cancellable = true,
            require = 0,
            remap = false
    )
    private static void fogxy$patchTerrainShader(String source, ShaderConstants constants,
                                                 CallbackInfoReturnable<Object> cir) {
        Object parsed = cir.getReturnValue();
        if (parsed == null) {
            return;
        }
        String current = SodiumParsedShader.source(parsed);
        if (current == null) {
            return;
        }
        String patched = SodiumShaderPatcher.patchFragment(current);
        if (patched == null) {
            patched = SodiumShaderPatcher.patchVertex(current);
        }
        if (patched == null) {
            return;
        }
        Object rebuilt = SodiumParsedShader.rebuild(parsed, patched);
        if (rebuilt != null) {
            cir.setReturnValue(rebuilt);
        }
    }
}
//?}
