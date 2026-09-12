package net.thatmaidenjaden.gleam.client.lighting;

import com.google.common.collect.MapMaker;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.client.renderer.ShaderInstance;
import net.thatmaidenjaden.gleam.config.GleamConfigs;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL43;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class GleamLightEngine {
    public static final int MAX_TOTAL_LIGHTS = 32768;
    private static final int LIGHT_FLOATS = 8;
    private static final int LIGHT_BYTES = LIGHT_FLOATS * Float.BYTES;

    public static final int GRID_DIM = 32;
    public static final int GRID_CELLS = GRID_DIM * GRID_DIM * GRID_DIM;
    private static final int CELL_BYTES = 128 * Integer.BYTES;
    private static final int GRID_SIZE_BYTES = GRID_CELLS * CELL_BYTES;

    private static final int SCENE_SIZE = 16;
    private static final int LIGHT_BINDING = 10;
    private static final int SCENE_BINDING = 11;
    private static final int GRID_BINDING = 12;

    private static GleamLightEngine instance;

    private final int[] localGrid = new int[GRID_CELLS * 128];
    private final boolean[] dirtySlices = new boolean[GRID_DIM];

    private final int lightBufferHandle;
    private final int sceneBufferHandle;
    private final int gridBufferHandle;

    private final ByteBuffer cpuLightBuffer;
    private final ByteBuffer cpuSceneBuffer;
    private final ByteBuffer cpuGridBuffer;

    private double anchorX, anchorY, anchorZ;
    private boolean dirty = true;
    private boolean buffersAllocated = false;

    private final Set<ShaderInstance> registeredShaders = new LinkedHashSet<>();
    private final Set<SectionLightHolder> activeLightSections = Collections.newSetFromMap(
            new MapMaker().weakKeys().concurrencyLevel(4).makeMap()
    );

    private final Set<Integer> registeredPrograms = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final IntArrayList dirtyCells = new IntArrayList();
    private final List<GleamLight> uploadedLights = new ArrayList<>(MAX_TOTAL_LIGHTS);

    private GleamLightEngine() {
        RenderSystem.assertOnRenderThread();

        this.cpuLightBuffer = ByteBuffer.allocateDirect(MAX_TOTAL_LIGHTS * LIGHT_BYTES).order(ByteOrder.nativeOrder());
        this.cpuGridBuffer = ByteBuffer.allocateDirect(GRID_SIZE_BYTES).order(ByteOrder.nativeOrder());
        this.cpuSceneBuffer = ByteBuffer.allocateDirect(SCENE_SIZE).order(ByteOrder.nativeOrder());

        cpuGridBuffer.position(0);
        for (int i = 0; i < GRID_CELLS; i++) for (int j = 0; j < 128; j++) cpuGridBuffer.putInt(0);
        cpuGridBuffer.position(0);

        this.lightBufferHandle = GL30.glGenBuffers();
        this.gridBufferHandle = GL30.glGenBuffers();
        this.sceneBufferHandle = GL30.glGenBuffers();
    }

    public static synchronized GleamLightEngine getInstance() {
        if (instance == null) instance = new GleamLightEngine();
        return instance;
    }

    public void trackSection(SectionLightHolder section) {
        activeLightSections.add(section);
        markDirty();
    }

    public void untrackSection(SectionLightHolder section) {
        activeLightSections.remove(section);
        markDirty();
    }

    public void setAnchor(double camX, double camY, double camZ) {
        this.anchorX = camX;
        this.anchorY = camY;
        this.anchorZ = camZ;
    }

    public Set<SectionLightHolder> getActiveSections() { return activeLightSections; }

    public void registerShader(ShaderInstance shader) { registeredShaders.add(shader); }
    public void clearShaders() { registeredShaders.clear(); }

    public void registerProgram(int program) { registeredPrograms.add(program); }
    public void clearPrograms() { registeredPrograms.clear(); }

    public void markDirty() { this.dirty = true; }
    public boolean isDirty() { return this.dirty; }
    public void clearDirty() { this.dirty = false; }

    public void rebindBlocks() {
        RenderSystem.assertOnRenderThread();
        for (ShaderInstance shader : registeredShaders) {
            int program = shader.getId();
            int index = GL43.glGetProgramResourceIndex(program, GL43.GL_SHADER_STORAGE_BLOCK, "GleamLight");
            if (index != GL31.GL_INVALID_INDEX) GL43.glShaderStorageBlockBinding(program, index, LIGHT_BINDING);

            index = GL31.glGetUniformBlockIndex(program, "GleamScene");
            if (index != GL31.GL_INVALID_INDEX) GL31.glUniformBlockBinding(program, index, SCENE_BINDING);

            index = GL43.glGetProgramResourceIndex(program, GL43.GL_SHADER_STORAGE_BLOCK, "GleamGrid");
            if (index != GL31.GL_INVALID_INDEX) GL43.glShaderStorageBlockBinding(program, index, GRID_BINDING);
        }
    }

    public void bindBuffers() {
        RenderSystem.assertOnRenderThread();
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, LIGHT_BINDING, lightBufferHandle);
        GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, SCENE_BINDING, sceneBufferHandle);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, GRID_BINDING, gridBufferHandle);
    }

    public void updateSceneUniform(double currentCamX, double currentCamY, double currentCamZ) {
        float dx = (float) (anchorX - currentCamX);
        float dy = (float) (anchorY - currentCamY);
        float dz = (float) (anchorZ - currentCamZ);

        cpuSceneBuffer.clear();
        cpuSceneBuffer.putInt(uploadedLights.size()).putFloat(dx).putFloat(dy).putFloat(dz);
        cpuSceneBuffer.flip();

        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, sceneBufferHandle);
        GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 0, cpuSceneBuffer);
    }

    public void uploadLights(List<GleamLight> gatheredLights, double camX, double camY, double camZ) {
        RenderSystem.assertOnRenderThread();

        if (!buffersAllocated) {
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, lightBufferHandle);
            GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, (long) MAX_TOTAL_LIGHTS * LIGHT_BYTES, GL15.GL_DYNAMIC_DRAW);

            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, gridBufferHandle);
            GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, GRID_SIZE_BYTES, GL15.GL_DYNAMIC_DRAW);

            GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, sceneBufferHandle);
            GL15.glBufferData(GL31.GL_UNIFORM_BUFFER, SCENE_SIZE, GL15.GL_DYNAMIC_DRAW);

            buffersAllocated = true;
        }

        cpuLightBuffer.clear();
        uploadedLights.clear();

        float globalIntensity = (float) (double) GleamConfigs.GLOBAL_LIGHT_INTENSITY.get();
        float globalSaturation = (float) (double) GleamConfigs.GLOBAL_LIGHT_SATURATION.get();
        boolean blacklightsEnabled = GleamConfigs.ENABLE_UV_BLACKLIGHTS.get();
        double renderDistance = GleamConfigs.LIGHT_RENDER_DISTANCE.get();
        double renderDistanceSq = renderDistance * renderDistance;

        for (GleamLight light : gatheredLights) {
            boolean isBlacklight = light.blacklight();
            if (isBlacklight && !blacklightsEnabled) continue;

            double distX = light.x() - camX;
            double distY = light.y() - camY;
            double distZ = light.z() - camZ;
            double camDistSq = distX * distX + distY * distY + distZ * distZ;

            if (camDistSq > renderDistanceSq) continue;

            float intensity = getIntensity(light, camDistSq, globalIntensity, GleamConfigs.DIM_FARTHER_LIGHTS.get(), renderDistance);
            if (intensity <= 0.001f) continue;

            float packedIntensity = isBlacklight ? -intensity : intensity;

            float baseR = light.r();
            float baseG = light.g();
            float baseB = light.b();

            float luma = baseR * 0.2126f + baseG * 0.7152f + baseB * 0.0722f;
            float satR = luma + (baseR - luma) * globalSaturation;
            float satG = luma + (baseG - luma) * globalSaturation;
            float satB = luma + (baseB - luma) * globalSaturation;

            float invRadiusSq = 1.0f / (light.radius() * light.radius());
            if (!light.occludeToBlocklight()) invRadiusSq = -invRadiusSq;

            cpuLightBuffer.putFloat(satR * intensity).putFloat(satG * intensity).putFloat(satB * intensity).putFloat(packedIntensity);
            cpuLightBuffer.putFloat((float) distX).putFloat((float) distY).putFloat((float) distZ);
            cpuLightBuffer.putFloat(invRadiusSq);

            uploadedLights.add(light);
        }
        cpuLightBuffer.flip();

        int uploadedCount = uploadedLights.size();
        buildSpatialGrid(uploadedLights, uploadedCount, camX, camY, camZ);

        cpuSceneBuffer.clear();
        cpuSceneBuffer.putInt(uploadedCount).putInt(0).putInt(0).putInt(0);
        cpuSceneBuffer.flip();

        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, lightBufferHandle);
        if (cpuLightBuffer.limit() > 0) GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0, cpuLightBuffer);

        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, gridBufferHandle);
        int sliceBytes = 1024 * CELL_BYTES;

        for (int x = 0; x < GRID_DIM; x++) {
            if (dirtySlices[x]) {
                int byteOffset = x * sliceBytes;
                cpuGridBuffer.limit(byteOffset + sliceBytes);
                cpuGridBuffer.position(byteOffset);
                GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, byteOffset, cpuGridBuffer);
            }
        }
        cpuGridBuffer.clear();

        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, sceneBufferHandle);
        GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 0, cpuSceneBuffer);

        bindBuffers();
    }

    private static float getIntensity(GleamLight light, double camDistSq, float globalIntensity, boolean dimFarther, double renderDist) {
        float baseIntensity = light.intensity() * globalIntensity;
        if (!dimFarther) return baseIntensity;

        double cullSq = renderDist * renderDist;
        double innerFull = cullSq / 36.0;
        double innerHalf = cullSq / 9.0;
        double outerHalf = cullSq * 25.0 / 36.0;

        float lodDimmer;
        if (camDistSq <= innerFull) {
            lodDimmer = 1.0f;
        } else if (camDistSq <= innerHalf) {
            float t = (float) ((camDistSq - innerFull) / (innerHalf - innerFull));
            lodDimmer = 1.0f - 0.5f * (t * t * (3.0f - 2.0f * t));
        } else if (camDistSq <= outerHalf) {
            lodDimmer = 0.5f;
        } else {
            float t = (float) ((cullSq - camDistSq) / (cullSq - outerHalf));
            lodDimmer = 0.5f * (t * t * (3.0f - 2.0f * t));
        }

        return baseIntensity * lodDimmer;
    }

    private void buildSpatialGrid(List<GleamLight> lights, int limit, double camX, double camY, double camZ) {
        Arrays.fill(dirtySlices, false);

        for (int i = 0; i < dirtyCells.size(); i++) {
            int baseIndex = dirtyCells.getInt(i);
            localGrid[baseIndex] = 0;
            cpuGridBuffer.putInt(baseIndex * Integer.BYTES, 0);

            int cellIndex = baseIndex / 128;
            int gridX = cellIndex / 1024;
            dirtySlices[gridX] = true;
        }
        dirtyCells.clear();

        for (int i = 0; i < limit; i++) {
            GleamLight light = lights.get(i);
            float radius = light.radius();

            float viewX = (float) (light.x() - camX) + 1024.0f;
            float viewY = (float) (light.y() - camY) + 1024.0f;
            float viewZ = (float) (light.z() - camZ) + 1024.0f;

            int minX = ((int) (viewX - radius)) >> 4;
            int maxX = ((int) (viewX + radius)) >> 4;
            int minY = ((int) (viewY - radius)) >> 4;
            int maxY = ((int) (viewY + radius)) >> 4;
            int minZ = ((int) (viewZ - radius)) >> 4;
            int maxZ = ((int) (viewZ + radius)) >> 4;

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        int gridX = x & 31;
                        int gridY = y & 31;
                        int gridZ = z & 31;

                        int cellIndex = (gridX * 1024) + (gridY * 32) + gridZ;
                        int baseIndex = cellIndex * 128;

                        int currentCount = localGrid[baseIndex];

                        if (currentCount >= 0) {
                            if (currentCount < 127) {
                                if (currentCount == 0) dirtyCells.add(baseIndex);
                                localGrid[baseIndex + currentCount + 1] = i;
                                localGrid[baseIndex] = currentCount + 1;
                                dirtySlices[gridX] = true;
                            } else if (currentCount == 127) {
                                int[] temp = new int[127];
                                for (int j = 0; j < 127; j++) {
                                    temp[j] = localGrid[baseIndex + 1 + j];
                                }
                                for (int j = 0; j < 127; j += 2) {
                                    int low = temp[j] & 0xFFFF;
                                    int high = (j + 1 < 127) ? (temp[j + 1] & 0xFFFF) : 0;
                                    localGrid[baseIndex + 1 + (j / 2)] = low | (high << 16);
                                }
                                int packedIndex = 127 / 2;
                                localGrid[baseIndex + 1 + packedIndex] |= (i & 0xFFFF) << 16;
                                localGrid[baseIndex] = -128;
                                dirtySlices[gridX] = true;
                            }
                        } else {
                            int newIndex = -currentCount;
                            if (newIndex < 254) {
                                int intOffset = newIndex / 2;
                                if (newIndex % 2 == 0) {
                                    localGrid[baseIndex + 1 + intOffset] = i & 0xFFFF;
                                } else {
                                    localGrid[baseIndex + 1 + intOffset] |= (i & 0xFFFF) << 16;
                                }
                                localGrid[baseIndex] = -(newIndex + 1);
                                dirtySlices[gridX] = true;
                            }
                        }
                    }
                }
            }
        }

        for (int i = 0; i < dirtyCells.size(); i++) {
            int baseIndex = dirtyCells.getInt(i);
            int count = localGrid[baseIndex];

            int byteOffset = baseIndex * Integer.BYTES;
            cpuGridBuffer.putInt(byteOffset, count);

            int intsToWrite;
            if (count >= 0) intsToWrite = count;
            else intsToWrite = (-count + 1) / 2;
            for (int j = 1; j <= intsToWrite; j++) cpuGridBuffer.putInt(byteOffset + j * Integer.BYTES, localGrid[baseIndex + j]);
        }
    }

    public void dispose() {
        if (lightBufferHandle != 0) GL30.glDeleteBuffers(lightBufferHandle);
        if (gridBufferHandle != 0) GL30.glDeleteBuffers(gridBufferHandle);
        if (sceneBufferHandle != 0) GL30.glDeleteBuffers(sceneBufferHandle);
        registeredShaders.clear();
    }
}