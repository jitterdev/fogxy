package dev.fogxy;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FogxyConfig {
    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setPrettyPrinting()
            .create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("fogxy.json");

    public static FogxyConfig CONFIG = new FogxyConfig();

    public boolean enabled = true;
    public float fogStart = 0.5f;
    public float fogEnd = 1.0f;
    public int smooth = 16;
    public int slope = 64;
    public int holeReach = 64;
    public int holeFade = 128;
    public int levels = 2;
    public boolean holeFill = true;
    public int fillTop = 96;
    public boolean skyFog = false;
    public boolean debugGradient = false;
    public boolean debugEdges = false;

    public static void load() {
        if (!Files.exists(PATH)) {
            CONFIG.save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(PATH)) {
            FogxyConfig loaded = GSON.fromJson(reader, FogxyConfig.class);
            if (loaded != null) {
                CONFIG = loaded;
                CONFIG.clamp();
                CONFIG.save();
            }
        } catch (Exception e) {
            Fogxy.LOGGER.warn("Failed to read fogxy config, resetting to defaults", e);
            CONFIG = new FogxyConfig();
            CONFIG.save();
        }
    }

    public void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException ignored) {
        }
    }

    private void clamp() {
        fogStart = Math.max(0.0f, Math.min(1.0f, fogStart));
        fogEnd = Math.max(0.05f, Math.min(1.0f, fogEnd));
        smooth = Math.max(0, Math.min(64, smooth));
        slope = Math.max(0, Math.min(512, slope));
        holeReach = Math.max(0, Math.min(1024, holeReach));
        holeFade = Math.max(0, Math.min(1024, holeFade));
        levels = Math.max(1, Math.min(5, levels));
        fillTop = Math.max(-64, Math.min(384, fillTop));
    }
}
