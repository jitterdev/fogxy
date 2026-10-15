package dev.fogxy.fog;

public final class FogxyShaderPatcher {
    private static final String FOG_SHADER = "voxy:post/blit_texture_depth_cutout.frag";

    private static final String LEGACY_UNIFORM_ANCHOR = "layout(location = 8) uniform float fogDensity;";
    private static final String LEGACY_LERP_ANCHOR = "float fogLerp = smoothstep(fogParams.x, fogParams.y, dist);";

    private static final String MODERN_UNIFORM_ANCHOR = "layout(location = 2) uniform mat4 projMat;";
    private static final String MODERN_MARKER = "uniform vec4 endParams;";
    private static final String MODERN_VEC3_MARKER = "uniform vec3 endParams;";
    // After the alpha discard so Fogxy runs even when voxy's own fog block is compiled out.
    private static final String MODERN_FOG_GUARD_ANCHOR =
            "    colour = texture(colourTex, UV.xy);\n"
                    + "    if (colour.a == 0.0) {\n"
                    + "        discard;\n"
                    + "    }\n";

    private static final String DISCARD_ANCHOR = "if (depth == 0.0f || depth == 1.0f) {";
    private static final String DISCARD_ANCHOR_NO_F = "if (depth == 0.0f || depth == 1.0) {";

    private static final String MODERN_HOST_MIX =
            "colour.rgb = mix(colour.rgb, fogColour.rgb, fogLerp*fogColour.a);";
    private static final String MODERN_VEC3_HOST_MIX =
            "colour.rgb = mix(colour.rgb, fogColour, fogLerp);";
    private static final String LEGACY_HOST_MIX =
            "colour.rgb = mix(colour.rgb, fogColor.rgb, clamp(fogLerp * fogIntensity, 0.0, 1.0));";

    static final String DEBUG_MARK =
            "\nvec4 fogxyDebugMark(vec2 huv) {"
                    + "\n    vec4 m = texture(fogxyHoleMask, huv);"
                    + "\n    float hole = m.b;"
                    + "\n    float cov = m.r;"
                    + "\n    vec2 tx = 2.0 / vec2(textureSize(fogxyHoleMask, 0));"
                    + "\n    float hxp = texture(fogxyHoleMask, huv + vec2(tx.x, 0.0)).b;"
                    + "\n    float hxm = texture(fogxyHoleMask, huv - vec2(tx.x, 0.0)).b;"
                    + "\n    float hzp = texture(fogxyHoleMask, huv + vec2(0.0, tx.y)).b;"
                    + "\n    float hzm = texture(fogxyHoleMask, huv - vec2(0.0, tx.y)).b;"
                    + "\n    float holeLine = clamp(abs(hole - hxp) + abs(hole - hxm) + abs(hole - hzp) + abs(hole - hzm), 0.0, 1.0);"
                    + "\n    float outer = (cov > 0.01 && cov < 0.2) ? 1.0 : 0.0;"
                    + "\n    float ring = (cov >= 0.2 && hole < 0.5) ? 1.0 : 0.0;"
                    + "\n    vec3 c = vec3(0.0);"
                    + "\n    float a = 0.0;"
                    + "\n    if (ring > 0.5) { c = vec3(1.0, 0.5, 0.0); a = 0.5; }"
                    + "\n    if (outer > 0.5) { c = vec3(0.6, 0.2, 1.0); a = 0.9; }"
                    + "\n    if (holeLine > 0.05) { c = vec3(1.0, 0.15, 0.7); a = 1.0; }"
                    + "\n    return vec4(c, a);"
                    + "\n}";

    private static final String UNIFORMS =
            "\nuniform float fogxyActive;"
                    + "\nuniform sampler2D fogxyFrontier;"
                    + "\nuniform float fogxyFogStart;"
                    + "\nuniform float fogxyFogEnd;"
                    + "\nuniform sampler2D fogxyHoleMask;"
                    + "\nuniform vec2 fogxyHoleOrigin;"
                    + "\nuniform float fogxyHoleSpan;"
                    + "\nuniform vec3 fogxyCamPos;"
                    + "\nuniform float fogxyDebugGradient;"
                    + "\nuniform float fogxyDebugEdges;"
                    + "\nuniform float fogxyFill;"
                    + "\nuniform float fogxyFillTop;"
                    + "\nuniform float fogxySkyFog;"
                    + "\nuniform vec4 fogxySunTint;"
                    + "\nuniform vec3 fogxySunDir;"
                    + DEBUG_MARK;

    private static final String MODERN_UNIFORMS = UNIFORMS
            + "\nuniform vec4 fogxyFogColor;"
            + "\n"
            + "\nvec3 fogxyFog(vec3 point, float dist) {"
            + "\n    if (fogxyActive < 0.5) {"
            + "\n        return vec3(0.0);"
            + "\n    }"
            + "\n    float u = atan(point.z, point.x) * 0.159154943;"
            + "\n    float fogxyLoop = max(32.0, texture(fogxyFrontier, vec2(u, 0.25)).r);"
            + "\n    float fogxyPatch = max(32.0, texture(fogxyFrontier, vec2(u, 0.75)).r);"
            + "\n    float fogxyLerp = 0.0;"
            + "\n    vec3 world = fogxyCamPos + point;"
            + "\n    vec2 huv = (world.xz - fogxyHoleOrigin) / fogxyHoleSpan;"
            + "\n    if (huv.x >= 0.0 && huv.x <= 1.0 && huv.y >= 0.0 && huv.y <= 1.0) {"
            + "\n        vec4 s = texture(fogxyHoleMask, huv);"
            + "\n        float border = s.g * 255.0 * 8.0;"
            + "\n        fogxyLerp = smoothstep(fogxyFogStart, fogxyFogEnd, dist / (dist + border + 1.0));"
            + "\n        fogxyLerp = max(fogxyLerp, smoothstep(0.35, 0.65, s.r));"
            + "\n    }"
            + "\n    return vec3(fogxyLoop, fogxyLerp, fogxyPatch);"
            + "\n}";

    private FogxyShaderPatcher() {
    }

    public static boolean isFogShader(String id) {
        return FOG_SHADER.equals(id);
    }

    public static String patch(String source) {
        if (source == null || (!source.contains("USE_ENV_FOG") && !source.contains("HAS_FOG"))) {
            return null;
        }
        if (source.contains(LEGACY_LERP_ANCHOR) && source.contains(LEGACY_UNIFORM_ANCHOR)) {
            return patchLegacy(source);
        }
        if ((source.contains(MODERN_MARKER) || source.contains(MODERN_VEC3_MARKER))
                && source.contains(MODERN_UNIFORM_ANCHOR)
                && source.contains(MODERN_FOG_GUARD_ANCHOR)) {
            return patchModern(source);
        }
        return null;
    }

    private static String patchLegacy(String source) {
        String patched = source.replace(LEGACY_UNIFORM_ANCHOR, LEGACY_UNIFORM_ANCHOR + UNIFORMS);
        patched = patched.replace(LEGACY_LERP_ANCHOR, LEGACY_LERP_ANCHOR + legacyFrontierBlock());
        patched = patched.replace(LEGACY_HOST_MIX, "if (fogxyDebugGradient < 0.5) "
                + LEGACY_HOST_MIX.replace("fogColor.rgb",
                "(fogxyActive > 0.5 && fogxySkyFog > 0.5 ? fogxySkyColour : fogColor.rgb)")
                + edgeMark(8));
        patched = patched.replace(DISCARD_ANCHOR, voidFill(true, false));
        return patched;
    }

    private static String patchModern(String source) {
        boolean hasDepthUtils = source.contains("NDC2SCREEN_DEPTH");
        String patched = source.replace(MODERN_UNIFORM_ANCHOR, MODERN_UNIFORM_ANCHOR + MODERN_UNIFORMS);
        patched = patched.replace(MODERN_FOG_GUARD_ANCHOR, MODERN_FOG_GUARD_ANCHOR + modernStandaloneFog());
        if (patched.contains(MODERN_HOST_MIX)) {
            patched = patched.replace(MODERN_HOST_MIX, "if (fogxyDebugGradient < 0.5) "
                    + MODERN_HOST_MIX.replace("fogColour.rgb",
                    "(fogxyActive > 0.5 && fogxySkyFog > 0.5 ? fogxySkyColour : fogColour.rgb)")
                    + edgeMark(8));
        } else if (patched.contains(MODERN_VEC3_HOST_MIX)) {
            patched = patched.replace(MODERN_VEC3_HOST_MIX, "if (fogxyDebugGradient < 0.5) "
                    + MODERN_VEC3_HOST_MIX.replace("fogColour",
                    "(fogxyActive > 0.5 && fogxySkyFog > 0.5 ? fogxySkyColour : fogColour)")
                    + edgeMark(8));
        }
        String fill = voidFill(false, hasDepthUtils);
        if (patched.contains(DISCARD_ANCHOR)) {
            patched = patched.replace(DISCARD_ANCHOR, fill);
        } else {
            patched = patched.replace(DISCARD_ANCHOR_NO_F, fill);
        }
        return patched;
    }

    private static String skyColour(String base, String dir, int indent) {
        String pad = " ".repeat(indent);
        return pad + "vec3 fogxySkyColour = " + base + ".rgb;\n"
                + pad + "if (fogxyActive > 0.5 && fogxySkyFog > 0.5) {\n"
                + pad + "    fogxySkyColour = mix(" + base + ".rgb, fogxySunTint.rgb,\n"
                + pad + "            max(0.0, dot(normalize(" + dir + "), fogxySunDir)) * fogxySunTint.a);\n"
                + pad + "}\n";
    }

    private static String edgeMark(int indent) {
        String pad = " ".repeat(indent);
        return "\n" + pad + "if (fogxyActive > 0.5 && fogxyDebugEdges > 0.5) {\n"
                + pad + "    vec3 fogxyWorld = fogxyCamPos + point;\n"
                + pad + "    vec2 fogxyHuv = (fogxyWorld.xz - fogxyHoleOrigin) / fogxyHoleSpan;\n"
                + pad + "    vec4 fogxyMark = fogxyDebugMark(fogxyHuv);\n"
                + pad + "    colour.rgb = mix(colour.rgb, fogxyMark.rgb, fogxyMark.a);\n"
                + pad + "}";
    }

    private static String modernStandaloneFog() {
        return skyColour("fogxyFogColor", "point.xyz", 4)
                + "    if (fogxyActive > 0.5) {\n"
                + "        vec3 fogxy = fogxyFog(point, length(point.xyz));\n"
                + "        if (fogxyDebugGradient > 0.5) {\n"
                + "            colour.rgb = vec3(fogxy.x / 2000.0, fogxy.y, fogxy.z / 2000.0);\n"
                + "            colour.a = 1.0;\n"
                + "        } else {\n"
                + "            colour.rgb = mix(colour.rgb,\n"
                + "                (fogxySkyFog > 0.5 ? fogxySkyColour : fogxyFogColor.rgb),\n"
                + "                clamp(fogxy.y * fogxyFogColor.a, 0.0, 1.0));\n"
                + "        }\n"
                + "    }\n";
    }

    private static String legacyFrontierBlock() {
        return "\n" + skyColour("fogColor", "point.xyz", 8)
                + "        if (fogxyActive > 0.5) {\n"
                + "            float fogxyU = atan(point.z, point.x) * 0.159154943;\n"
                + "            float fogxyLoop = max(32.0, texture(fogxyFrontier, vec2(fogxyU, 0.25)).r);\n"
                + "            float fogxyPatch = max(32.0, texture(fogxyFrontier, vec2(fogxyU, 0.75)).r);\n"
                + "            float fogxyLerp = 0.0;\n"
                + "            vec3 fogxyWorld = fogxyCamPos + point;\n"
                + "            vec2 fogxyHuv = (fogxyWorld.xz - fogxyHoleOrigin) / fogxyHoleSpan;\n"
                + "            if (fogxyHuv.x >= 0.0 && fogxyHuv.x <= 1.0 && fogxyHuv.y >= 0.0 && fogxyHuv.y <= 1.0) {\n"
                + "                vec4 fogxySample = texture(fogxyHoleMask, fogxyHuv);\n"
                + "                float fogxyBorder = fogxySample.g * 255.0 * 8.0;\n"
                + "                float fogxyFrac = dist / (dist + fogxyBorder + 1.0);\n"
                + "                fogxyLerp = smoothstep(fogxyFogStart, fogxyFogEnd, fogxyFrac);\n"
                + "                fogxyLerp = max(fogxyLerp, smoothstep(0.35, 0.65, fogxySample.r));\n"
                + "            }\n"
                + "            if (fogxyDebugGradient > 0.5) {\n"
                + "                colour.rgb = vec3(fogxyLoop / 2000.0, fogxyLerp, fogxyPatch / 2000.0);\n"
                + "                colour.a = 1.0;\n"
                + "            } else {\n"
                + "                fogLerp = max(fogLerp, fogxyLerp);\n"
                + "            }\n"
                + "        }\n";
    }

    // World-space raymarch below fillTop against the inner-hole mask on pixels where voxy drew nothing.
    private static String voidFill(boolean legacy, boolean hasDepthUtils) {
        String empty = hasDepthUtils ? "FAR" : "1.0f";
        String occluded = hasDepthUtils ? "NEAR" : "0.0f";
        String gate = legacy ? "fogxyActive > 0.5 && fogxyFill > 0.5 && fogIntensity > 0.0"
                : "fogxyActive > 0.5 && fogxyFill > 0.5";
        String fogColour = legacy ? "fogColor" : "fogxyFogColor";
        String fillColour = "                    vec4 fogxyFillColour = vec4(" + fogColour + ".rgb, 1.0);\n"
                + "                    if (fogxySkyFog > 0.5) {\n"
                + "                        fogxyFillColour.rgb = mix(" + fogColour + ".rgb, fogxySunTint.rgb,\n"
                + "                                max(0.0, dot(fogxyRay, fogxySunDir)) * fogxySunTint.a);\n"
                + "                    }\n"
                + "                    colour = fogxyDebugEdges > 0.5 ? vec4(0.1, 0.9, 1.0, 1.0) : fogxyFillColour;\n";
        String clamp = !hasDepthUtils
                ? "                    fogxyFillDepth = min(1.0f-(2.0f/((1<<24)-1)), fogxyFillDepth);\n"
                    + "                    fogxyFillDepth = fogxyFillDepth * 0.5f + 0.5f;\n"
                : "                    fogxyFillDepth = REDUCTION2(FAR+CLOSER_SIGN*(2.0f/((1<<24)-1)), fogxyFillDepth);\n"
                    + "                    fogxyFillDepth = NDC2SCREEN_DEPTH(fogxyFillDepth);\n";
        return "if (depth == " + empty + ") {\n"
                + (legacy ? "#ifdef USE_ENV_FOG\n" : "")
                + "        if (" + gate + ") {\n"
                + "            vec3 fogxyRay = normalize(rev3d(vec3(UV.xy, 0.5)));\n"
                + "            float fogxyLo = 32.0;\n"
                + "            float fogxyHi = 1.0e9;\n"
                + "            if (fogxyRay.y > 1.0e-4) {\n"
                + "                fogxyHi = (fogxyFillTop - fogxyCamPos.y) / fogxyRay.y;\n"
                + "            } else if (fogxyRay.y < -1.0e-4) {\n"
                + "                fogxyLo = max(fogxyLo, (fogxyFillTop - fogxyCamPos.y) / fogxyRay.y);\n"
                + "            } else if (fogxyCamPos.y > fogxyFillTop) {\n"
                + "                fogxyHi = -1.0;\n"
                + "            }\n"
                + "            vec2 fogxyInv = 1.0 / fogxyRay.xz;\n"
                + "            vec2 fogxyExit = max((fogxyHoleOrigin - fogxyCamPos.xz) * fogxyInv,\n"
                + "                    (fogxyHoleOrigin + fogxyHoleSpan - fogxyCamPos.xz) * fogxyInv);\n"
                + "            fogxyHi = min(fogxyHi, min(fogxyExit.x, fogxyExit.y));\n"
                + "            for (float fogxyT = fogxyLo; fogxyT <= fogxyHi; fogxyT += 48.0) {\n"
                + "                vec2 fogxyHuv = (fogxyCamPos.xz + fogxyRay.xz * fogxyT - fogxyHoleOrigin) / fogxyHoleSpan;\n"
                + "                if (fogxyHuv.x < 0.0 || fogxyHuv.x > 1.0 || fogxyHuv.y < 0.0 || fogxyHuv.y > 1.0) {\n"
                + "                    break;\n"
                + "                }\n"
                + "                if (texture(fogxyHoleMask, fogxyHuv).a > 0.5) {\n"
                + "                    vec3 fogxyPos = fogxyRay * fogxyT;\n"
                + "                    float fogxyFillDepth = projDepth(fogxyPos);\n"
                + clamp
                + "                    gl_FragDepth = gl_DepthRange.diff * fogxyFillDepth + gl_DepthRange.near;\n"
                + fillColour
                + "                    return;\n"
                + "                }\n"
                + "            }\n"
                + "        }\n"
                + (legacy ? "#endif\n" : "")
                + "        discard;\n"
                + "    }\n"
                + "    if (depth == " + occluded + ") {";
    }
}
