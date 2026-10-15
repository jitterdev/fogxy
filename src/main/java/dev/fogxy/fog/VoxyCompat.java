package dev.fogxy.fog;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import it.unimi.dsi.fastutil.longs.LongArrayList;

public final class VoxyCompat {
    private static final String RENDER_SYSTEM = "me.cortex.voxy.client.core.VoxyRenderSystem";
    private static final String GET_RENDER_SYSTEM = "me.cortex.voxy.client.core.IGetVoxyRenderSystem";
    private static final String RENDER_SYSTEM_HOLDER = "me.cortex.voxy.client.core.IVoxyRenderSystemHolder";
    private static final String CONFIG = "me.cortex.voxy.client.config.VoxyConfig";
    private static final String FOG_STORAGE = "net.caffeinemc.mods.sodium.client.util.FogStorage";
    private static final float[] DEFAULT_FOG_COLOR = new float[]{1.0f, 1.0f, 1.0f, 1.0f};

    private static Boolean loaded;
    private static boolean resolved;
    private static volatile long[] lastDrawn;
    private static Method getNullable;
    private static Method getRenderSystemFromLevel;
    private static Method getViewportMethod;
    private static Method getCapturedFogColorMethod;
    private static Field viewportFogParametersField;
    private static Method fogRedMethod;
    private static Method fogGreenMethod;
    private static Method fogBlueMethod;
    private static Method fogAlphaMethod;
    private static Method sodiumGetFogParametersMethod;
    private static Method isRenderingEnabled;
    private static Field configField;
    private static Field environmentalFogField;
    private static Method getFogModeMethod;
    private static Field fogModeHasFogField;
    private static Field enableRenderingField;
    private static Field fogIntensityField;
    private static Field sectionRenderDistanceField;

    private VoxyCompat() {
    }

    public static boolean isLoaded() {
        if (loaded == null) {
            loaded = FabricLoader.getInstance().isModLoaded("voxy") || classExists(RENDER_SYSTEM);
        }
        return loaded;
    }

    private static synchronized void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;

        for (String holderName : new String[]{RENDER_SYSTEM_HOLDER, GET_RENDER_SYSTEM}) {
            try {
                Class<?> holder = Class.forName(holderName);
                try {
                    getNullable = holder.getMethod("getNullable");
                } catch (Throwable ignored) {
                }
                for (String getter : new String[]{"voxy$getRenderSystem", "getVoxyRenderSystem"}) {
                    try {
                        getRenderSystemFromLevel = holder.getMethod(getter);
                        break;
                    } catch (Throwable ignored) {
                    }
                }
                if (getNullable != null || getRenderSystemFromLevel != null) {
                    break;
                }
            } catch (Throwable ignored) {
            }
        }

        try {
            Class<?> rs = Class.forName(RENDER_SYSTEM);
            try {
                getViewportMethod = rs.getMethod("getViewport");
            } catch (Throwable ignored) {
            }
            try {
                getCapturedFogColorMethod = rs.getMethod("getCapturedFogColor");
            } catch (Throwable ignored) {
            }
        } catch (Throwable ignored) {
        }

        try {
            Class<?> storage = Class.forName(FOG_STORAGE);
            sodiumGetFogParametersMethod = storage.getMethod("sodium$getFogParameters");
        } catch (Throwable ignored) {
        }

        try {
            Class<?> config = Class.forName(CONFIG);
            configField = field(config, "CONFIG");
            environmentalFogField = field(config, "renderVoxyFog", "useEnvironmentalFog");
            enableRenderingField = field(config, "enableRendering");
            fogIntensityField = field(config, "fogIntensity");
            sectionRenderDistanceField = field(config, "sectionRenderDistance");
            try {
                getFogModeMethod = config.getMethod("getFogMode");
                fogModeHasFogField = getFogModeMethod.getReturnType().getField("hasFog");
            } catch (Throwable ignored) {
                getFogModeMethod = null;
            }
            try {
                isRenderingEnabled = config.getMethod("isRenderingEnabled");
            } catch (Throwable ignored) {
                isRenderingEnabled = null;
            }
        } catch (Throwable t) {
            configField = null;
        }
    }

    private static Field field(Class<?> owner, String... names) {
        for (String name : names) {
            try {
                return owner.getField(name);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    public static Object getRenderSystem() {
        if (!isLoaded()) {
            return null;
        }
        resolve();
        if (getNullable != null) {
            try {
                return getNullable.invoke(null);
            } catch (Throwable ignored) {
            }
        }
        if (getRenderSystemFromLevel != null) {
            try {
                Object levelRenderer = Minecraft.getInstance().levelRenderer;
                if (levelRenderer != null) {
                    return getRenderSystemFromLevel.invoke(levelRenderer);
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    public static boolean isRendering() {
        return getRenderSystem() != null;
    }

    public static Object getEngine() {
        Object renderSystem = getRenderSystem();
        if (renderSystem == null) {
            return null;
        }
        try {
            return renderSystem.getClass().getMethod("getEngine").invoke(renderSystem);
        } catch (Throwable t) {
            return null;
        }
    }

    // Falls back to the previous snapshot if voxy's worker mutates the node map mid-read.
    public static long[] snapshotDrawnSections() {
        Object renderSystem = getRenderSystem();
        if (!(renderSystem instanceof FogxySectionSource source)) {
            return null;
        }
        LongArrayList out = new LongArrayList();
        try {
            source.fogxyCollectDrawnSections(out::add);
        } catch (Throwable t) {
            return lastDrawn;
        }
        long[] snapshot = out.toLongArray();
        lastDrawn = snapshot;
        return snapshot;
    }

    public static boolean isRenderingEnabled() {
        resolve();
        try {
            if (isRenderingEnabled != null && configField != null) {
                return (Boolean) isRenderingEnabled.invoke(configField.get(null));
            }
            if (enableRenderingField != null && configField != null) {
                return enableRenderingField.getBoolean(configField.get(null));
            }
        } catch (Throwable ignored) {
        }
        return true;
    }

    public static boolean isEnvironmentalFogEnabled() {
        resolve();
        if (configField == null) {
            return true;
        }
        try {
            Object cfg = configField.get(null);
            if (environmentalFogField != null) {
                return environmentalFogField.getBoolean(cfg);
            }
            if (getFogModeMethod != null && fogModeHasFogField != null) {
                Object mode = getFogModeMethod.invoke(cfg);
                if (mode != null) {
                    return fogModeHasFogField.getBoolean(mode);
                }
            }
        } catch (Throwable ignored) {
        }
        return true;
    }

    public static float fogIntensity() {
        resolve();
        if (configField == null || fogIntensityField == null) {
            return 1.0f;
        }
        try {
            return fogIntensityField.getFloat(configField.get(null));
        } catch (Throwable t) {
            return 1.0f;
        }
    }

    public static float renderDistanceBlocks() {
        resolve();
        if (configField == null || sectionRenderDistanceField == null) {
            return 8192.0f;
        }
        try {
            return ((Number) sectionRenderDistanceField.get(configField.get(null))).floatValue() * 512.0f;
        } catch (Throwable t) {
            return 8192.0f;
        }
    }

    public static float[] fogColor() {
        resolve();
        Object renderSystem = getRenderSystem();
        if (renderSystem != null) {
            if (getViewportMethod != null) {
                try {
                    Object viewport = getViewportMethod.invoke(renderSystem);
                    if (viewport != null) {
                        if (viewportFogParametersField == null) {
                            viewportFogParametersField = viewport.getClass().getField("fogParameters");
                        }
                        Object params = viewportFogParametersField.get(viewport);
                        float[] unpacked = unpackFogParameters(params);
                        if (unpacked != null) {
                            return unpacked;
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
            if (getCapturedFogColorMethod != null) {
                try {
                    float[] captured = (float[]) getCapturedFogColorMethod.invoke(renderSystem);
                    if (captured != null && captured.length >= 4) {
                        return captured;
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        //? if <1.21.8 {
        return com.mojang.blaze3d.systems.RenderSystem.getShaderFogColor();
        //?} else {
        /*if (sodiumGetFogParametersMethod != null) {
            try {
                Object params = sodiumGetFogParametersMethod.invoke(Minecraft.getInstance().gameRenderer);
                float[] unpacked = unpackFogParameters(params);
                if (unpacked != null) {
                    return unpacked;
                }
            } catch (Throwable ignored) {
            }
        }
        return DEFAULT_FOG_COLOR;
        *///?}
    }

    private static float[] unpackFogParameters(Object params) {
        if (params == null) {
            return null;
        }
        try {
            if (fogRedMethod == null) {
                Class<?> cls = params.getClass();
                fogRedMethod = cls.getMethod("red");
                fogGreenMethod = cls.getMethod("green");
                fogBlueMethod = cls.getMethod("blue");
                fogAlphaMethod = cls.getMethod("alpha");
            }
            return new float[]{
                    (Float) fogRedMethod.invoke(params),
                    (Float) fogGreenMethod.invoke(params),
                    (Float) fogBlueMethod.invoke(params),
                    (Float) fogAlphaMethod.invoke(params)
            };
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name, false, VoxyCompat.class.getClassLoader());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
