package dev.fogxy.fog;

import dev.fogxy.FogxyConfig;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;
import org.lwjgl.system.MemoryUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongConsumer;

// Publishes the masks the shaders sample: the loop, the frontier and the hole volume.
public final class FogFrontier {
    private static final int CELL = 32;
    private static final int RAYS = 512;
    private static final int LOOP_TEXTURE_UNIT = 4;
    private static final int HOLE_TEXTURE_UNIT = 5;

    private static final long MASK_INTERVAL_MS = 1000L;
    private static final long TIME_BUDGET_NS = 200_000_000L;
    private static final long BUDGET_CHECK_MASK = 0x3FFL;
    private static final double RAY_EPSILON_SQ = 1.0;

    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Fogxy frontier");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    });

    private static final AtomicBoolean BUSY = new AtomicBoolean();
    private static volatile long lastBuildMs;
    private static Field storageField;

    private static volatile Mask pending;

    private static Mask mask;
    private static float[] frontier;
    private static float[] holeFrontier;
    private static double frontierX;
    private static double frontierZ;
    private static boolean dirtyLoop;
    private static boolean dirtyHoles;
    private static int loopTextureId = -1;
    private static FloatBuffer loopBuffer;
    private static int holeTextureId = -1;
    private static ByteBuffer holeBuffer;
    private static int BINDS;
    private static int BUILDS;

    private FogFrontier() {
    }

    public static void tick() {
        Mask fresh = pending;
        if (fresh != null) {
            pending = null;
            mask = fresh;
            frontier = null;
            holeFrontier = null;
            dirtyHoles = true;
        }

        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        if (mask != null && camera != null && camera.isInitialized()) {
            double cameraX = camera.getPosition().x;
            double cameraZ = camera.getPosition().z;
            double dx = cameraX - frontierX;
            double dz = cameraZ - frontierZ;
            if (frontier == null || dx * dx + dz * dz >= RAY_EPSILON_SQ) {
                float[] marched = march(mask, mask.field, cameraX, cameraZ, FogxyConfig.CONFIG.slope);
                float[] patches = march(mask, mask.holeField, cameraX, cameraZ, FogxyConfig.CONFIG.slope);
                if (marched != null && patches != null) {
                    frontier = marched;
                    holeFrontier = patches;
                    frontierX = cameraX;
                    frontierZ = cameraZ;
                    dirtyLoop = true;
                }
            }
        }

        scheduleBuild(camera);
    }

    private static void scheduleBuild(Camera camera) {
        long now = System.currentTimeMillis();
        if (camera == null || !camera.isInitialized() || now - lastBuildMs < MASK_INTERVAL_MS
                || !BUSY.compareAndSet(false, true)) {
            return;
        }
        lastBuildMs = now;

        Object engine = VoxyCompat.getEngine();
        if (engine == null) {
            BUSY.set(false);
            return;
        }

        double cameraX = camera.getPosition().x;
        double cameraZ = camera.getPosition().z;
        double radius = VoxyCompat.renderDistanceBlocks() + 1024.0;
        double vanillaDistance = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16.0;
        // Snapshot on the render thread: voxy mutates its node map on its own worker.
        long[] drawn = VoxyCompat.snapshotDrawnSections();

        WORKER.execute(() -> {
            try {
                Mask result = build(engine, cameraX, cameraZ, radius, vanillaDistance, drawn);
                if (result != null) {
                    pending = result;
                }
            } catch (Throwable ignored) {
            } finally {
                BUSY.set(false);
            }
        });
    }

    public static void bind() {
        int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        if (program == 0) {
            return;
        }
        int fieldLocation = GL20.glGetUniformLocation(program, "fogxyFrontier");
        if (fieldLocation == -1) {
            return;
        }
        int fogColorLocation = GL20.glGetUniformLocation(program, "fogxyFogColor");

        boolean active = FogxyConfig.CONFIG.enabled && frontier != null && holeFrontier != null && mask != null
                && VoxyCompat.isRenderingEnabled() && VoxyCompat.fogIntensity() > 0.0f;
        if (active && BINDS++ < 8) {
            float[] fogColor = VoxyCompat.fogColor();
            dev.fogxy.Fogxy.LOGGER.info(
                    "fogxy bind#{} program={} field={} fogColorLoc={} fogColor=[{}, {}, {}, {}] envFog={} rendering={} start={} end={} levels={} holeReach={} frontier=[{}, {}, {}]",
                    BINDS, program, fieldLocation, fogColorLocation,
                    fogColor[0], fogColor[1], fogColor[2], fogColor[3], VoxyCompat.isEnvironmentalFogEnabled(),
                    VoxyCompat.isRenderingEnabled(), FogxyConfig.CONFIG.fogStart, FogxyConfig.CONFIG.fogEnd,
                    FogxyConfig.CONFIG.levels, FogxyConfig.CONFIG.holeReach,
                    minOf(frontier), avgOf(frontier), maxOf(frontier));
            dev.fogxy.Fogxy.LOGGER.info("fogxy patch edge=[{}, {}, {}]",
                    minOf(holeFrontier), avgOf(holeFrontier), maxOf(holeFrontier));
        }
        int activeLocation = GL20.glGetUniformLocation(program, "fogxyActive");
        if (activeLocation != -1) {
            GL20.glUniform1f(activeLocation, active ? 1.0f : 0.0f);
        }
        if (!active) {
            return;
        }

        if (loopTextureId == -1) {
            loopTextureId = GL11.glGenTextures();
        }
        if (holeTextureId == -1) {
            holeTextureId = GL11.glGenTextures();
        }

        // Bind our own unit first; Sodium leaves the block atlas on unit 0.
        GlStateManager._activeTexture(GL13.GL_TEXTURE0 + LOOP_TEXTURE_UNIT);
        GlStateManager._bindTexture(loopTextureId);
        GL33.glBindSampler(LOOP_TEXTURE_UNIT, 0);
        if (dirtyLoop) {
            uploadLoop();
            dirtyLoop = false;
        }
        GL20.glUniform1i(fieldLocation, LOOP_TEXTURE_UNIT);

        GlStateManager._activeTexture(GL13.GL_TEXTURE0 + HOLE_TEXTURE_UNIT);
        GlStateManager._bindTexture(holeTextureId);
        GL33.glBindSampler(HOLE_TEXTURE_UNIT, 0);
        // The hole mask only changes when a new mask lands, not on every camera move.
        if (dirtyHoles) {
            uploadHoles();
            dirtyHoles = false;
        }
        GlStateManager._activeTexture(GL13.GL_TEXTURE0);

        int holeFieldLocation = GL20.glGetUniformLocation(program, "fogxyHoleMask");
        if (holeFieldLocation != -1) {
            GL20.glUniform1i(holeFieldLocation, HOLE_TEXTURE_UNIT);
        }
        int originLocation = GL20.glGetUniformLocation(program, "fogxyHoleOrigin");
        if (originLocation != -1) {
            GL20.glUniform2f(originLocation, (float) mask.originX, (float) mask.originZ);
        }
        int spanLocation = GL20.glGetUniformLocation(program, "fogxyHoleSpan");
        if (spanLocation != -1) {
            GL20.glUniform1f(spanLocation, (float) (mask.grid * CELL));
        }
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        int camLocation = GL20.glGetUniformLocation(program, "fogxyCamPos");
        if (camLocation != -1 && camera != null) {
            GL20.glUniform3f(camLocation, (float) camera.getPosition().x, (float) camera.getPosition().y,
                    (float) camera.getPosition().z);
        }

        int startLocation = GL20.glGetUniformLocation(program, "fogxyFogStart");
        if (startLocation != -1) {
            GL20.glUniform1f(startLocation, FogxyConfig.CONFIG.fogStart);
        }
        int endLocation = GL20.glGetUniformLocation(program, "fogxyFogEnd");
        if (endLocation != -1) {
            GL20.glUniform1f(endLocation, FogxyConfig.CONFIG.fogEnd);
        }
        int gradientLocation = GL20.glGetUniformLocation(program, "fogxyDebugGradient");
        if (gradientLocation != -1) {
            GL20.glUniform1f(gradientLocation, FogxyConfig.CONFIG.debugGradient ? 1.0f : 0.0f);
        }
        int edgesLocation = GL20.glGetUniformLocation(program, "fogxyDebugEdges");
        if (edgesLocation != -1) {
            GL20.glUniform1f(edgesLocation, FogxyConfig.CONFIG.debugEdges ? 1.0f : 0.0f);
        }
        int fillLocation = GL20.glGetUniformLocation(program, "fogxyFill");
        if (fillLocation != -1) {
            GL20.glUniform1f(fillLocation, FogxyConfig.CONFIG.holeFill ? 1.0f : 0.0f);
        }
        int fillTopLocation = GL20.glGetUniformLocation(program, "fogxyFillTop");
        if (fillTopLocation != -1) {
            GL20.glUniform1f(fillTopLocation, FogxyConfig.CONFIG.fillTop);
        }
        int skyFogLocation = GL20.glGetUniformLocation(program, "fogxySkyFog");
        if (skyFogLocation != -1) {
            GL20.glUniform1f(skyFogLocation, FogxyConfig.CONFIG.skyFog ? 1.0f : 0.0f);
        }
        if (FogxyConfig.CONFIG.skyFog) {
            net.minecraft.client.multiplayer.ClientLevel level = Minecraft.getInstance().level;
            if (level != null) {
                float partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
                //? if >=1.21.11 {
                /*int sunriseArgb = camera != null
                        ? camera.attributeProbe().getValue(net.minecraft.world.attribute.EnvironmentAttributes.SUNRISE_SUNSET_COLOR, partial)
                        : 0;
                float sunriseAlpha = net.minecraft.util.ARGB.alphaFloat(sunriseArgb);
                float[] sunrise = sunriseAlpha <= 0.0f ? null : new float[]{
                        net.minecraft.util.ARGB.redFloat(sunriseArgb),
                        net.minecraft.util.ARGB.greenFloat(sunriseArgb),
                        net.minecraft.util.ARGB.blueFloat(sunriseArgb),
                        sunriseAlpha
                };
                float sunAngle = camera != null
                        ? camera.attributeProbe().getValue(net.minecraft.world.attribute.EnvironmentAttributes.SUN_ANGLE, partial) * ((float) Math.PI / 180.0f)
                        : 0.0f;
                *///?} elif >=1.21.8 {
                /*float timeOfDay = level.getTimeOfDay(partial);
                int sunriseArgb = level.effects().isSunriseOrSunset(timeOfDay)
                        ? level.effects().getSunriseOrSunsetColor(timeOfDay)
                        : 0;
                float[] sunrise = sunriseArgb == 0 ? null : new float[]{
                        net.minecraft.util.ARGB.redFloat(sunriseArgb),
                        net.minecraft.util.ARGB.greenFloat(sunriseArgb),
                        net.minecraft.util.ARGB.blueFloat(sunriseArgb),
                        net.minecraft.util.ARGB.alphaFloat(sunriseArgb)
                };
                float sunAngle = level.getSunAngle(partial);
                *///?} else {
                float timeOfDay = level.getTimeOfDay(partial);
                float[] sunrise = level.effects().getSunriseColor(timeOfDay, partial);
                float sunAngle = level.getSunAngle(partial);
                //?}
                int tintLocation = GL20.glGetUniformLocation(program, "fogxySunTint");
                if (tintLocation != -1) {
                    GL20.glUniform4f(tintLocation,
                            sunrise == null ? 0.0f : sunrise[0],
                            sunrise == null ? 0.0f : sunrise[1],
                            sunrise == null ? 0.0f : sunrise[2],
                            sunrise == null ? 0.0f : sunrise[3]);
                }
                int sunDirLocation = GL20.glGetUniformLocation(program, "fogxySunDir");
                if (sunDirLocation != -1) {
                    float sunSide = net.minecraft.util.Mth.sin(sunAngle) > 0.0f ? -1.0f : 1.0f;
                    GL20.glUniform3f(sunDirLocation, sunSide, 0.0f, 0.0f);
                }
            }
        }
        if (fogColorLocation != -1) {
            float[] fogColor = VoxyCompat.fogColor();
            GL20.glUniform4f(fogColorLocation, fogColor[0], fogColor[1], fogColor[2], fogColor[3]);
        }
    }

    private static float minOf(float[] values) {
        float min = Float.MAX_VALUE;
        for (float value : values) {
            min = Math.min(min, value);
        }
        return min;
    }

    // Minecraft leaves GL_UNPACK_ROW_LENGTH and the skip offsets behind from its own texture
    // uploads; a stale row length silently reinterprets our tightly packed rows as garbage.
    private static void beginUpload() {
        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
    }

    private static void endUpload() {
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4);
    }

    private static float maxOf(float[] values) {
        float max = 0.0f;
        for (float value : values) {
            max = Math.max(max, value);
        }
        return max;
    }

    private static float avgOf(float[] values) {
        float sum = 0.0f;
        for (float value : values) {
            sum += value;
        }
        return sum / values.length;
    }

    private static void uploadLoop() {
        if (loopBuffer == null) {
            loopBuffer = MemoryUtil.memAllocFloat(RAYS * 2);
        }
        loopBuffer.clear();
        // Row 0 is the terrain-edge loop, row 1 the coarse-patch edge; sampled at each row's centre.
        loopBuffer.put(frontier, 0, Math.min(frontier.length, RAYS));
        loopBuffer.put(holeFrontier, 0, Math.min(holeFrontier.length, RAYS));
        loopBuffer.flip();

        beginUpload();
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_R16F, RAYS, 2, 0,
                GL11.GL_RED, GL11.GL_FLOAT, loopBuffer);
        endUpload();
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
    }

    private static void uploadHoles() {
        int size = mask.grid;
        if (holeBuffer == null || holeBuffer.capacity() < size * size * 4) {
            if (holeBuffer != null) {
                MemoryUtil.memFree(holeBuffer);
            }
            holeBuffer = MemoryUtil.memAlloc(size * size * 4);
        }
        holeBuffer.clear();
        holeBuffer.put(mask.holeMask, 0, size * size * 4);
        holeBuffer.flip();

        beginUpload();
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA8, size, size, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, holeBuffer);
        endUpload();
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
    }

    private static float[] march(Mask mask, float[] field, double cameraX, double cameraZ, double slope) {
        int playerCellX = (int) Math.floor((cameraX - mask.originX) / CELL);
        int playerCellZ = (int) Math.floor((cameraZ - mask.originZ) / CELL);
        if (playerCellX < 0 || playerCellX >= mask.grid || playerCellZ < 0 || playerCellZ >= mask.grid) {
            return null;
        }

        float[] result = new float[RAYS];
        double step = CELL * 0.5;
        for (int i = 0; i < RAYS; i++) {
            double angle = (i + 0.5) * (2.0 * Math.PI / RAYS);
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);
            double distance = mask.radius;
            for (double t = step; t <= mask.radius; t += step) {
                if (occupancy(mask, field, cameraX + dx * t, cameraZ + dz * t) < 0.5f) {
                    // Binary search the exact crossing to avoid 16 m stair-stepping.
                    double lo = t - step;
                    double hi = t;
                    for (int it = 0; it < 5; it++) {
                        double mid = (lo + hi) * 0.5;
                        if (occupancy(mask, field, cameraX + dx * mid, cameraZ + dz * mid) < 0.5f) {
                            hi = mid;
                        } else {
                            lo = mid;
                        }
                    }
                    distance = Math.max(0.0, hi - CELL * 2.0);
                    break;
                }
            }
            result[i] = (float) distance;
        }
        return slopeLimit(smooth(result, Math.max(0, Math.min(RAYS / 2 - 1, FogxyConfig.CONFIG.smooth))),
                slope);
    }

    private static float occupancy(Mask mask, float[] field, double x, double z) {
        double gx = (x - mask.originX) / CELL - 0.5;
        double gz = (z - mask.originZ) / CELL - 0.5;
        int x0 = (int) Math.floor(gx);
        int z0 = (int) Math.floor(gz);
        float fx = (float) (gx - x0);
        float fz = (float) (gz - z0);
        float v00 = cell(mask, field, x0, z0);
        float v10 = cell(mask, field, x0 + 1, z0);
        float v01 = cell(mask, field, x0, z0 + 1);
        float v11 = cell(mask, field, x0 + 1, z0 + 1);
        return (v00 * (1.0f - fx) + v10 * fx) * (1.0f - fz)
                + (v01 * (1.0f - fx) + v11 * fx) * fz;
    }

    private static float cell(Mask mask, float[] field, int cx, int cz) {
        if (cx < 0 || cx >= mask.grid || cz < 0 || cz >= mask.grid) {
            return 0.0f;
        }
        return field[cz * mask.grid + cx];
    }

    // Clamp to raw march so far arms of a C-shape don't push fog past the near rim.
    private static float[] smooth(float[] values, int radius) {
        if (radius <= 0) {
            return values;
        }
        int n = values.length;
        float[] out = new float[n];
        float scale = 1.0f / (2 * radius + 1);
        for (int i = 0; i < n; i++) {
            float sum = 0.0f;
            for (int j = -radius; j <= radius; j++) {
                sum += values[Math.floorMod(i + j, n)];
            }
            out[i] = Math.min(sum * scale, values[i]);
        }
        return out;
    }

    private static float[] slopeLimit(float[] values, double slope) {
        if (slope <= 0.0) {
            return values;
        }
        int n = values.length;
        // Two passes each way keep wrap-around at ray 0 consistent.
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 1; i < n; i++) {
                values[i] = (float) Math.min(values[i], values[i - 1] + slope);
            }
            values[0] = (float) Math.min(values[0], values[n - 1] + slope);
            for (int i = n - 2; i >= 0; i--) {
                values[i] = (float) Math.min(values[i], values[i + 1] + slope);
            }
            values[n - 1] = (float) Math.min(values[n - 1], values[0] + slope);
        }
        return values;
    }

    private static Mask build(Object engine, double cameraX, double cameraZ, double radius,
                              double vanillaDistance, long[] drawn) {
        int halfCells = (int) Math.ceil(radius / CELL);
        int grid = halfCells * 2;
        int cells = grid * grid;
        double originX = Math.floor((cameraX - radius) / CELL) * CELL;
        double originZ = Math.floor((cameraZ - radius) / CELL) * CELL;
        byte[] fine = new byte[cells];
        byte[] any = new byte[cells];

        int playerCellX = (int) Math.floor((cameraX - originX) / CELL);
        int playerCellZ = (int) Math.floor((cameraZ - originZ) / CELL);
        fine[playerCellZ * grid + playerCellX] = 1;
        any[playerCellZ * grid + playerCellX] = 1;

        int vanillaCells = (int) Math.ceil(vanillaDistance / CELL);
        for (int cz = Math.max(0, playerCellZ - vanillaCells); cz <= Math.min(grid - 1, playerCellZ + vanillaCells); cz++) {
            for (int cx = Math.max(0, playerCellX - vanillaCells); cx <= Math.min(grid - 1, playerCellX + vanillaCells); cx++) {
                double dx = originX + (cx + 0.5) * CELL - cameraX;
                double dz = originZ + (cz + 0.5) * CELL - cameraZ;
                if (dx * dx + dz * dz <= vanillaDistance * vanillaDistance) {
                    fine[cz * grid + cx] = 1;
                    any[cz * grid + cx] = 1;
                }
            }
        }

        long deadline = System.nanoTime() + TIME_BUDGET_NS;
        int[] seen = new int[1];
        LongConsumer consumer = id -> {
            if ((seen[0]++ & BUDGET_CHECK_MASK) == 0 && System.nanoTime() > deadline) {
                throw new Abort();
            }
            int level = (int) ((id >>> 60) & 0xF);
            boolean isFine = level < FogxyConfig.CONFIG.levels;
            int size = 32 << level;
            double minX = (double) ((id << 36) >> 40) * size;
            double minZ = (double) ((id << 12) >> 40) * size;
            int x0 = (int) Math.floor((minX - originX) / CELL);
            int x1 = (int) Math.floor((minX + size - 1.0 - originX) / CELL);
            int z0 = (int) Math.floor((minZ - originZ) / CELL);
            int z1 = (int) Math.floor((minZ + size - 1.0 - originZ) / CELL);
            for (int cz = Math.max(0, z0); cz <= Math.min(grid - 1, z1); cz++) {
                int row = cz * grid;
                for (int cx = Math.max(0, x0); cx <= Math.min(grid - 1, x1); cx++) {
                    int index = row + cx;
                    any[index] = 1;
                    if (isFine) {
                        fine[index] = 1;
                    }
                }
            }
        };

        try {
            if (drawn != null) {
                for (long id : drawn) {
                    consumer.accept(id);
                }
            } else {
                Object storage = storageOf(engine);
                if (storage == null) {
                    return null;
                }
                Method iterate = storage.getClass().getMethod("iteratePositions", int.class, LongConsumer.class);
                iterate.invoke(storage, -1, consumer);
            }
        } catch (Throwable t) {
            return null;
        }

        // Flood open void from the border; unreachable cells are enclosed missing chunks.
        byte[] reachable = new byte[cells];
        int[] stack = new int[cells];
        floodFromBorder(any, reachable, stack, grid);
        int enclosed = 0;
        for (int i = 0; i < cells; i++) {
            if (any[i] == 0 && reachable[i] == 0) {
                any[i] = 1;
                enclosed++;
            }
        }

        byte[] hole = new byte[cells];
        int holes = 0;
        for (int i = 0; i < cells; i++) {
            if (any[i] != 0 && fine[i] == 0) {
                hole[i] = 1;
                holes++;
            }
        }

        // Flood non-fine cells from the border so void fill only paints inland holes, not the outer edge.
        byte[] outer = new byte[cells];
        floodFromBorder(fine, outer, stack, grid);
        byte[] inner = new byte[cells];
        int inners = 0;
        for (int i = 0; i < cells; i++) {
            if (hole[i] != 0 && outer[i] == 0) {
                inner[i] = 1;
                inners++;
            }
        }
        dilate(inner, grid, 1);

        // March undilated holes so fog ends where coarse patches start.
        byte[] edgeFlag = hole.clone();
        int fadeCells = Math.max(1, (int) Math.ceil(FogxyConfig.CONFIG.holeFade / (double) CELL));
        int reachCells = (int) Math.ceil(FogxyConfig.CONFIG.holeReach / (double) CELL);
        dilate(hole, grid, reachCells);

        // Cancel coverage over detailed terrain past the reach so fade doesn't bridge across rings.
        byte[] blocked = new byte[cells];
        for (int i = 0; i < cells; i++) {
            blocked[i] = fine[i] != 0 && hole[i] == 0 ? (byte) 1 : 0;
        }
        int[] block = new int[cells];
        boxBlur(blocked, block, grid, fadeCells);

        // Drive coverage by distance; blurring a binary mask dilutes small holes below the shader gate.
        int[] distance = distanceToBlock(edgeFlag, grid);
        byte[] holeCoverage = new byte[cells];
        for (int i = 0; i < cells; i++) {
            double cellsAway = distance[i] / 100.0;
            int coverage;
            if (cellsAway <= reachCells) {
                coverage = 255;
            } else {
                double t = (cellsAway - reachCells) / fadeCells;
                coverage = (int) Math.round(255.0 * Math.max(0.0, 1.0 - t));
            }
            if (edgeFlag[i] == 0) {
                coverage = coverage * (255 - block[i]) / 255;
            }
            holeCoverage[i] = (byte) Math.min(255, Math.max(0, coverage));
        }

        // Distance to the fog block in cells so shaders anchor the fade to the border, not the camera.
        int[] borderDistance = distanceToBlock(hole, grid);
        boxBlurInt(borderDistance, grid, 2);
        for (int i = 0; i < cells; i++) {
            if (hole[i] != 0) {
                borderDistance[i] = 0;
            }
        }
        // Pack distance in quarter-cells (8 m) to avoid 32 m stepping; alpha marks inland holes.
        byte[] holeMask = new byte[cells * 4];
        for (int i = 0; i < cells; i++) {
            holeMask[i * 4] = holeCoverage[i];
            holeMask[i * 4 + 1] = (byte) Math.min(255, borderDistance[i] / 25);
            holeMask[i * 4 + 2] = edgeFlag[i] != 0 ? (byte) 255 : 0;
            holeMask[i * 4 + 3] = inner[i] != 0 ? (byte) 255 : 0;
        }

        if (BUILDS++ % 5 == 0) {
            int fineCount = 0;
            int anyCount = 0;
            for (int i = 0; i < cells; i++) {
                if (fine[i] != 0) {
                    fineCount++;
                }
                if (any[i] != 0) {
                    anyCount++;
                }
            }
            dev.fogxy.Fogxy.LOGGER.info(
                    "fogxy mask#{} cam=({}, {}) radius={} grid={} fine={} any={} holes={} inner={} enclosedVoids={} reach={} drawn={}",
                    BUILDS, (long) cameraX, (long) cameraZ, (long) radius, grid, fineCount, anyCount, holes,
                    inners, enclosed, FogxyConfig.CONFIG.holeReach, drawn == null ? -1 : drawn.length);
        }

        return new Mask(blur(fine, grid), blur(edgeFlag, grid), holeMask, grid, originX, originZ,
                halfCells * CELL);
    }

    private static void floodFromBorder(byte[] wall, byte[] reachable, int[] stack, int grid) {
        int sp = 0;
        for (int i = 0; i < grid; i++) {
            sp = flood(wall, reachable, stack, sp, i);
            sp = flood(wall, reachable, stack, sp, (grid - 1) * grid + i);
            sp = flood(wall, reachable, stack, sp, i * grid);
            sp = flood(wall, reachable, stack, sp, i * grid + grid - 1);
        }
        while (sp > 0) {
            int c = stack[--sp];
            int cx = c % grid;
            int cz = c / grid;
            if (cx > 0) {
                sp = flood(wall, reachable, stack, sp, c - 1);
            }
            if (cx < grid - 1) {
                sp = flood(wall, reachable, stack, sp, c + 1);
            }
            if (cz > 0) {
                sp = flood(wall, reachable, stack, sp, c - grid);
            }
            if (cz < grid - 1) {
                sp = flood(wall, reachable, stack, sp, c + grid);
            }
        }
    }

    private static int flood(byte[] wall, byte[] reachable, int[] stack, int sp, int index) {
        if (wall[index] == 0 && reachable[index] == 0) {
            reachable[index] = 1;
            stack[sp++] = index;
        }
        return sp;
    }

    private static void boxBlurInt(int[] values, int grid, int radius) {
        int[] tmp = new int[grid * grid];
        for (int z = 0; z < grid; z++) {
            int row = z * grid;
            for (int x = 0; x < grid; x++) {
                long sum = 0;
                int count = 0;
                for (int j = -radius; j <= radius; j++) {
                    int xx = x + j;
                    if (xx >= 0 && xx < grid) {
                        sum += values[row + xx];
                        count++;
                    }
                }
                tmp[row + x] = (int) (sum / count);
            }
        }
        for (int z = 0; z < grid; z++) {
            int row = z * grid;
            for (int x = 0; x < grid; x++) {
                long sum = 0;
                int count = 0;
                for (int j = -radius; j <= radius; j++) {
                    int zz = z + j;
                    if (zz >= 0 && zz < grid) {
                        sum += tmp[zz * grid + x];
                        count++;
                    }
                }
                values[row + x] = (int) (sum / count);
            }
        }
    }

    // Two-pass chamfer distance in hundredths of a cell (100 orthogonal, 141 diagonal).
    private static int[] distanceToBlock(byte[] hole, int grid) {
        int far = 1 << 24;
        int[] distance = new int[grid * grid];
        for (int i = 0; i < distance.length; i++) {
            distance[i] = hole[i] != 0 ? 0 : far;
        }
        for (int z = 0; z < grid; z++) {
            for (int x = 0; x < grid; x++) {
                int i = z * grid + x;
                if (distance[i] == 0) {
                    continue;
                }
                int best = distance[i];
                if (x > 0) {
                    best = Math.min(best, distance[i - 1] + 100);
                }
                if (z > 0) {
                    best = Math.min(best, distance[i - grid] + 100);
                }
                if (x > 0 && z > 0) {
                    best = Math.min(best, distance[i - grid - 1] + 141);
                }
                if (x < grid - 1 && z > 0) {
                    best = Math.min(best, distance[i - grid + 1] + 141);
                }
                distance[i] = best;
            }
        }
        for (int z = grid - 1; z >= 0; z--) {
            for (int x = grid - 1; x >= 0; x--) {
                int i = z * grid + x;
                if (distance[i] == 0) {
                    continue;
                }
                int best = distance[i];
                if (x < grid - 1) {
                    best = Math.min(best, distance[i + 1] + 100);
                }
                if (z < grid - 1) {
                    best = Math.min(best, distance[i + grid] + 100);
                }
                if (x < grid - 1 && z < grid - 1) {
                    best = Math.min(best, distance[i + grid + 1] + 141);
                }
                if (x > 0 && z < grid - 1) {
                    best = Math.min(best, distance[i + grid - 1] + 141);
                }
                distance[i] = best;
            }
        }
        return distance;
    }

    private static void dilate(byte[] cells, int grid, int radius) {
        if (radius <= 0) {
            return;
        }
        byte[] tmp = new byte[grid * grid];
        for (int z = 0; z < grid; z++) {
            int row = z * grid;
            for (int x = 0; x < grid; x++) {
                byte value = 0;
                for (int j = -radius; j <= radius && value == 0; j++) {
                    int xx = x + j;
                    if (xx >= 0 && xx < grid && cells[row + xx] != 0) {
                        value = 1;
                    }
                }
                tmp[row + x] = value;
            }
        }
        for (int z = 0; z < grid; z++) {
            int row = z * grid;
            for (int x = 0; x < grid; x++) {
                byte value = 0;
                for (int j = -radius; j <= radius && value == 0; j++) {
                    int zz = z + j;
                    if (zz >= 0 && zz < grid && tmp[zz * grid + x] != 0) {
                        value = 1;
                    }
                }
                cells[row + x] = value;
            }
        }
    }

    private static void boxBlur(byte[] cells, int[] out, int grid, int radius) {
        int[] tmp = new int[grid * grid];
        for (int z = 0; z < grid; z++) {
            int row = z * grid;
            for (int x = 0; x < grid; x++) {
                int sum = 0;
                for (int j = -radius; j <= radius; j++) {
                    int xx = x + j;
                    if (xx >= 0 && xx < grid) {
                        sum += cells[row + xx];
                    }
                }
                tmp[row + x] = sum;
            }
        }
        int window = (2 * radius + 1) * (2 * radius + 1);
        for (int z = 0; z < grid; z++) {
            int row = z * grid;
            for (int x = 0; x < grid; x++) {
                int sum = 0;
                for (int j = -radius; j <= radius; j++) {
                    int zz = z + j;
                    if (zz >= 0 && zz < grid) {
                        sum += tmp[zz * grid + x];
                    }
                }
                out[row + x] = sum * 255 / window;
            }
        }
    }

    private static float[] blur(byte[] cells, int grid) {
        float[] tmp = new float[grid * grid];
        float[] out = new float[grid * grid];
        for (int z = 0; z < grid; z++) {
            int row = z * grid;
            for (int x = 0; x < grid; x++) {
                int sum = cells[row + x];
                if (x > 0) {
                    sum += cells[row + x - 1];
                }
                if (x < grid - 1) {
                    sum += cells[row + x + 1];
                }
                tmp[row + x] = sum;
            }
        }
        for (int z = 0; z < grid; z++) {
            int row = z * grid;
            for (int x = 0; x < grid; x++) {
                float sum = tmp[row + x];
                if (z > 0) {
                    sum += tmp[row - grid + x];
                }
                if (z < grid - 1) {
                    sum += tmp[row + grid + x];
                }
                out[row + x] = sum * (1.0f / 9.0f);
            }
        }
        return out;
    }

    private static Object storageOf(Object engine) {
        try {
            if (storageField == null) {
                storageField = engine.getClass().getField("storage");
            }
            return storageField.get(engine);
        } catch (Throwable t) {
            return null;
        }
    }

    private static final class Mask {
        final float[] field;
        final float[] holeField;
        final byte[] holeMask;
        final int grid;
        final double originX;
        final double originZ;
        final double radius;

        Mask(float[] field, float[] holeField, byte[] holeMask, int grid, double originX, double originZ,
             double radius) {
            this.field = field;
            this.holeField = holeField;
            this.holeMask = holeMask;
            this.grid = grid;
            this.originX = originX;
            this.originZ = originZ;
            this.radius = radius;
        }
    }

    private static final class Abort extends RuntimeException {
        private Abort() {
            super(null, null, false, false);
        }
    }
}
