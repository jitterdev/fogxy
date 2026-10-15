package dev.fogxy.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

// Probes the classpath instead of mod version strings so Connector and Roxy repackaging still match.
public final class FogxyMixinPlugin implements IMixinConfigPlugin {
    private static final boolean VOXY_PRESENT = FabricLoader.getInstance().isModLoaded("voxy")
            || classExists("me.cortex.voxy.client.core.VoxyRenderSystem");

    private static final boolean SODIUM_SHADER_PARSER =
            classExists("net.caffeinemc.mods.sodium.client.gl.shader.ShaderParser");
    private static final boolean SODIUM_PARSED_SHADER =
            classExists("net.caffeinemc.mods.sodium.client.gl.shader.ShaderParser$ParsedShader");
    private static final boolean GLSL_PREPROCESSOR =
            classExists("com.mojang.blaze3d.preprocessor.GlslPreprocessor");
    private static final boolean SODIUM_FOG_BIND =
            classExists("net.caffeinemc.mods.sodium.client.render.chunk.shader.DefaultShaderInterface")
                    || classExists("net.caffeinemc.mods.sodium.client.gpu.device.context.GLDrawContext");
    private static final boolean SODIUM_OLD_OPTIONS =
            classExists("net.caffeinemc.mods.sodium.client.gui.SodiumOptionsGUI")
                    && classExists("net.caffeinemc.mods.sodium.client.gui.options.OptionPage");
    private static final boolean SODIUM_NEW_OPTIONS =
            classExists("net.caffeinemc.mods.sodium.client.gui.SodiumConfigBuilder")
                    && classExists("net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder");

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith("MixinSodiumShaderParser")) {
            return VOXY_PRESENT && SODIUM_SHADER_PARSER && !SODIUM_PARSED_SHADER;
        }
        if (mixinClassName.endsWith("MixinSodiumShaderParserNew")) {
            return VOXY_PRESENT && (SODIUM_PARSED_SHADER || (!SODIUM_SHADER_PARSER && GLSL_PREPROCESSOR));
        }
        if (mixinClassName.endsWith("MixinSodiumChunkFog")) {
            return VOXY_PRESENT && SODIUM_FOG_BIND;
        }
        if (mixinClassName.endsWith("MixinSodiumOptionsGUI")) {
            return SODIUM_OLD_OPTIONS;
        }
        if (mixinClassName.endsWith("MixinSodiumConfigBuilderNew")) {
            return SODIUM_NEW_OPTIONS;
        }
        if (mixinClassName.endsWith("MixinVoxyShaderLoader") || mixinClassName.endsWith("MixinVoxyFullscreenBlit")
                || mixinClassName.endsWith("MixinLevelRenderer") || mixinClassName.endsWith("MixinVoxyRenderSystem")
                || mixinClassName.endsWith("MixinAsyncNodeManager") || mixinClassName.endsWith("MixinNodeManager")) {
            return VOXY_PRESENT;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    private static boolean classExists(String name) {
        String path = name.replace('.', '/') + ".class";
        if (FogxyMixinPlugin.class.getClassLoader().getResource(path) != null) {
            return true;
        }
        return FabricLoader.getInstance().getAllMods().stream().anyMatch(mod -> mod.findPath(path).isPresent());
    }
}
