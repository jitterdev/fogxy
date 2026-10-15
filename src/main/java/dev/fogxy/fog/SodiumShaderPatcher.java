package dev.fogxy.fog;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SodiumShaderPatcher {
    private static final String FRAG_VARYING_SCALAR = "in float v_FragDistance;";
    private static final String FRAG_VARYING_VEC2 = "in vec2 v_FragDistance;";
    private static final String VERT_VARYING_ANCHOR = "out vec2 v_TexCoord;";
    private static final String VERT_POSITION_ANCHOR = "vec3 position = _vert_position + translation;";

    private static final Pattern FRAG_CALL_LEGACY = Pattern.compile(
            "_linearFog\\(([A-Za-z_][A-Za-z0-9_]*), v_FragDistance, u_FogColor, u_FogStart, u_FogEnd\\)");
    private static final Pattern FRAG_CALL_ENV = Pattern.compile(
            "_linearFog\\(([A-Za-z_][A-Za-z0-9_]*), v_FragDistance, u_FogColor, u_EnvironmentFog, u_RenderFog\\)");
    private static final Pattern FRAG_CALL_FADE = Pattern.compile(
            "_linearFog\\(([A-Za-z_][A-Za-z0-9_]*), v_FragDistance, u_FogColor, u_EnvironmentFog, u_RenderFog, fadeFactor\\)");

    private SodiumShaderPatcher() {
    }

    public static String patchFragment(String source) {
        if (source == null || !source.contains("u_FogColor")) {
            return null;
        }
        String varyingAnchor;
        String replacementCall;
        String signature;
        String distInit;
        String fallbackReturn;

        Matcher legacyCall = FRAG_CALL_LEGACY.matcher(source);
        Matcher envCall = FRAG_CALL_ENV.matcher(source);
        Matcher fadeCall = FRAG_CALL_FADE.matcher(source);
        String matchedGroup;

        if (source.contains(FRAG_VARYING_SCALAR) && legacyCall.find()) {
            varyingAnchor = FRAG_VARYING_SCALAR;
            matchedGroup = legacyCall.group();
            replacementCall = "fogxyFog(" + legacyCall.group(1)
                    + ", v_FragDistance, u_FogColor, u_FogStart, u_FogEnd, v_FogxyPos)";
            signature = "vec4 fogxyFog(vec4 color, float dist, vec4 fogColor, float fogStart, float fogEnd, vec3 pos)";
            distInit = "";
            fallbackReturn = "    return _linearFog(color, dist, fogColor, fogStart, fogEnd);\n";
        } else if (source.contains(FRAG_VARYING_VEC2) && fadeCall.find()) {
            varyingAnchor = FRAG_VARYING_VEC2;
            matchedGroup = fadeCall.group();
            replacementCall = "fogxyFog(" + fadeCall.group(1)
                    + ", v_FragDistance, u_FogColor, u_EnvironmentFog, u_RenderFog, fadeFactor, v_FogxyPos)";
            signature = "vec4 fogxyFog(vec4 color, vec2 fragDistance, vec4 fogColor, vec2 environmentFog, vec2 renderFog, float fadeFactor, vec3 pos)";
            distInit = "        float dist = fragDistance.y;\n";
            fallbackReturn = "    return _linearFog(color, fragDistance, fogColor, environmentFog, renderFog, fadeFactor);\n";
        } else if (source.contains(FRAG_VARYING_VEC2) && envCall.find()) {
            varyingAnchor = FRAG_VARYING_VEC2;
            matchedGroup = envCall.group();
            replacementCall = "fogxyFog(" + envCall.group(1)
                    + ", v_FragDistance, u_FogColor, u_EnvironmentFog, u_RenderFog, v_FogxyPos)";
            signature = "vec4 fogxyFog(vec4 color, vec2 fragDistance, vec4 fogColor, vec2 environmentFog, vec2 renderFog, vec3 pos)";
            distInit = "        float dist = fragDistance.y;\n";
            fallbackReturn = "    return _linearFog(color, fragDistance, fogColor, environmentFog, renderFog);\n";
        } else {
            return null;
        }

        String patched = source.replace(varyingAnchor, varyingAnchor + "\nin vec3 v_FogxyPos;");
        patched = patched.replace(matchedGroup, replacementCall);

        String block = "uniform float fogxyActive;\n"
                + "uniform sampler2D fogxyFrontier;\n"
                + "uniform float fogxyFogStart;\n"
                + "uniform float fogxyFogEnd;\n"
                + "uniform sampler2D fogxyHoleMask;\n"
                + "uniform vec2 fogxyHoleOrigin;\n"
                + "uniform float fogxyHoleSpan;\n"
                + "uniform vec3 fogxyCamPos;\n"
                + "uniform float fogxyDebugGradient;\n"
                + "uniform float fogxyDebugEdges;\n"
                + "uniform float fogxySkyFog;\n"
                + "uniform vec4 fogxySunTint;\n"
                + "uniform vec3 fogxySunDir;\n"
                + FogxyShaderPatcher.DEBUG_MARK + "\n"
                + "\n"
                + signature + " {\n"
                + "#ifdef USE_FOG\n"
                + "    if (fogxyActive > 0.5) {\n"
                + distInit
                + "        vec3 world = fogxyCamPos + pos;\n"
                + "        float u = atan(pos.z, pos.x) * 0.159154943;\n"
                + "        float loop = max(32.0, texture(fogxyFrontier, vec2(u, 0.25)).r);\n"
                + "        float patch = max(32.0, texture(fogxyFrontier, vec2(u, 0.75)).r);\n"
                + "        float reshaped = 0.0;\n"
                + "        vec2 huv = (world.xz - fogxyHoleOrigin) / fogxyHoleSpan;\n"
                + "        if (huv.x >= 0.0 && huv.x <= 1.0 && huv.y >= 0.0 && huv.y <= 1.0) {\n"
                + "            vec4 sample2 = texture(fogxyHoleMask, huv);\n"
                + "            float border = sample2.g * 255.0 * 8.0;\n"
                + "            float frac = dist / (dist + border + 1.0);\n"
                + "            reshaped = smoothstep(fogxyFogStart, fogxyFogEnd, frac);\n"
                + "            reshaped = max(reshaped, smoothstep(0.35, 0.65, sample2.r));\n"
                + "        }\n"
                + "        if (fogxyDebugGradient > 0.5) {\n"
                + "            return vec4(vec3(loop / 2000.0, reshaped, patch / 2000.0), 1.0);\n"
                + "        }\n"
                + "        vec3 fogxySkyColour = fogColor.rgb;\n"
                + "        if (fogxySkyFog > 0.5) {\n"
                + "            fogxySkyColour = mix(fogColor.rgb, fogxySunTint.rgb,\n"
                + "                    max(0.0, dot(normalize(pos), fogxySunDir)) * fogxySunTint.a);\n"
                + "        }\n"
                + "        color.rgb = mix(color.rgb, (fogxySkyFog > 0.5 ? fogxySkyColour : fogColor.rgb),\n"
                + "                clamp(reshaped * fogColor.a, 0.0, 1.0));\n"
                + "        if (fogxyDebugEdges > 0.5) {\n"
                + "            vec4 mark = fogxyDebugMark(huv);\n"
                + "            color.rgb = mix(color.rgb, mark.rgb, mark.a);\n"
                + "            return vec4(color.rgb, 1.0);\n"
                + "        }\n"
                + "    }\n"
                + "#endif\n"
                + fallbackReturn
                + "}\n\n";

        int main = patched.indexOf("void main()");
        if (main < 0) {
            return null;
        }
        return patched.substring(0, main) + block + patched.substring(main);
    }

    public static String patchVertex(String source) {
        if (source == null || !source.contains(VERT_POSITION_ANCHOR)) {
            return null;
        }
        String patched = source.replace(VERT_VARYING_ANCHOR, VERT_VARYING_ANCHOR + "\nout vec3 v_FogxyPos;");
        patched = patched.replace(VERT_POSITION_ANCHOR, VERT_POSITION_ANCHOR + "\n    v_FogxyPos = position;");
        return patched;
    }
}
