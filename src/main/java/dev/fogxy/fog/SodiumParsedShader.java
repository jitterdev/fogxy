package dev.fogxy.fog;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

// Reaches ShaderParser.ParsedShader reflectively so builds against Sodium 0.6 still load on 0.7+.
public final class SodiumParsedShader {
    private static Method src;
    private static Method includeIds;
    private static Constructor<?> ctor;

    private SodiumParsedShader() {
    }

    public static String source(Object parsed) {
        try {
            if (src == null) {
                src = parsed.getClass().getMethod("src");
            }
            return (String) src.invoke(parsed);
        } catch (Throwable t) {
            return null;
        }
    }

    public static Object rebuild(Object parsed, String source) {
        try {
            if (includeIds == null) {
                includeIds = parsed.getClass().getMethod("includeIds");
            }
            if (ctor == null) {
                ctor = parsed.getClass().getConstructor(String.class, String[].class);
            }
            return ctor.newInstance(source, (String[]) includeIds.invoke(parsed));
        } catch (Throwable t) {
            return null;
        }
    }
}
