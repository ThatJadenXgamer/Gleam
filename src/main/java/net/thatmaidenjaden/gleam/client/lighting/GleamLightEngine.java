package net.thatmaidenjaden.gleam.client.lighting;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL43;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class GleamLightEngine {
    public static final int MAX_TOTAL_LIGHTS = 16384;
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

    private final int lightBufferHandle;
    private final int sceneBufferHandle;
    private final int gridBufferHandle;

    private final ByteBuffer cpuLightBuffer;
    private final ByteBuffer cpuSceneBuffer;
    private final ByteBuffer cpuGridBuffer;

    private final Set<ShaderInstance> registeredShaders = new LinkedHashSet<>();
    private final Set<SectionLightHolder> activeLightSections = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Integer> registeredPrograms = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private GleamLightEngine() {
        RenderSystem.assertOnRenderThread();

        this.cpuLightBuffer = ByteBuffer.allocateDirect(MAX_TOTAL_LIGHTS * LIGHT_BYTES).order(ByteOrder.nativeOrder());
        this.cpuGridBuffer = ByteBuffer.allocateDirect(GRID_SIZE_BYTES).order(ByteOrder.nativeOrder());
        this.cpuSceneBuffer = ByteBuffer.allocateDirect(SCENE_SIZE).order(ByteOrder.nativeOrder());

        this.lightBufferHandle = GL30.glGenBuffers();
        this.gridBufferHandle = GL30.glGenBuffers();
        this.sceneBufferHandle = GL30.glGenBuffers();
    }

    public static synchronized GleamLightEngine getInstance() {
        if (instance == null) instance = new GleamLightEngine();
        return instance;
    }

    public void trackSection(SectionLightHolder section) { activeLightSections.add(section); }
    public void untrackSection(SectionLightHolder section) { activeLightSections.remove(section); }
    public Set<SectionLightHolder> getActiveSections() { return activeLightSections; }

    public void registerShader(ShaderInstance shader) { registeredShaders.add(shader); }
    public void clearShaders() { registeredShaders.clear(); }

    public void registerProgram(int program) { registeredPrograms.add(program); }
    public void clearPrograms() { registeredPrograms.clear(); }

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

    public void uploadLights(List<GleamLight> gatheredLights, double camX, double camY, double camZ) {
        RenderSystem.assertOnRenderThread();

        cpuLightBuffer.clear();
        int count = gatheredLights.size();
        for (GleamLight light : gatheredLights) {
            float intensity = light.intensity();
            cpuLightBuffer.putFloat(light.r() * intensity).putFloat(light.g() * intensity).putFloat(light.b() * intensity).putFloat(intensity);
            cpuLightBuffer.putFloat((float) (light.x() - camX)).putFloat((float) (light.y() - camY)).putFloat((float) (light.z() - camZ));
            cpuLightBuffer.putFloat(1.0f / (light.radius() * light.radius()));
        }
        cpuLightBuffer.flip();

        buildSpatialGrid(gatheredLights, count, camX, camY, camZ);
        cpuGridBuffer.position(0).limit(GRID_SIZE_BYTES);

        cpuSceneBuffer.clear();
        cpuSceneBuffer.putInt(count).putInt(0).putInt(0).putInt(0);
        cpuSceneBuffer.flip();

        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, lightBufferHandle);
        GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, cpuLightBuffer, GL15.GL_DYNAMIC_DRAW);

        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, gridBufferHandle);
        GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, cpuGridBuffer, GL15.GL_DYNAMIC_DRAW);

        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, sceneBufferHandle);
        GL15.glBufferData(GL31.GL_UNIFORM_BUFFER, cpuSceneBuffer, GL15.GL_DYNAMIC_DRAW);

        bindBuffers();
    }

    private void buildSpatialGrid(List<GleamLight> lights, int limit, double camX, double camY, double camZ) {
        for (int i = 0; i < GRID_CELLS; i++) cpuGridBuffer.putInt(i * CELL_BYTES, 0);

        for (int i = 0; i < limit; i++) {
            GleamLight light = lights.get(i);
            float radius = light.radius();

            float viewX = (float) (light.x() - camX) + 1024.0f;
            float viewY = (float) (light.y() - camY) + 1024.0f;
            float viewZ = (float) (light.z() - camZ) + 1024.0f;

            int minX = (int) Math.floor((viewX - radius) / 16.0);
            int maxX = (int) Math.floor((viewX + radius) / 16.0);
            int minY = (int) Math.floor((viewY - radius) / 16.0);
            int maxY = (int) Math.floor((viewY + radius) / 16.0);
            int minZ = (int) Math.floor((viewZ - radius) / 16.0);
            int maxZ = (int) Math.floor((viewZ + radius) / 16.0);

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        int gridX = x & 31;
                        int gridY = y & 31;
                        int gridZ = z & 31;

                        int cellOffset = ((gridX * 1024) + (gridY * 32) + gridZ) * CELL_BYTES;
                        int currentCount = cpuGridBuffer.getInt(cellOffset);

                        if (currentCount < 127) {
                            cpuGridBuffer.putInt(cellOffset + (currentCount + 1) * Integer.BYTES, i);
                            cpuGridBuffer.putInt(cellOffset, currentCount + 1);
                        }
                    }
                }
            }
        }
    }

    public void dispose() {
        if (lightBufferHandle != 0) GL30.glDeleteBuffers(lightBufferHandle);
        if (gridBufferHandle != 0) GL30.glDeleteBuffers(gridBufferHandle);
        if (sceneBufferHandle != 0) GL30.glDeleteBuffers(sceneBufferHandle);
        registeredShaders.clear();
    }
}