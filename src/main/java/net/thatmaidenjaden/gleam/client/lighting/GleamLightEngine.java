package net.thatmaidenjaden.gleam.client.lighting;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class GleamLightEngine {
    public static final int MAX_LIGHTS = 64;
    private static final int LIGHT_FLOATS = 8;
    private static final int SCENE_SIZE = 32;
    private static final int LIGHT_BINDING = 10;
    private static final int SCENE_BINDING = 11;
    private static GleamLightEngine instance;

    private final int lightBufferHandle;
    private final int sceneBufferHandle;
    private final FloatBuffer lightStagingBuffer;
    private final ByteBuffer sceneStagingBuffer;
    private final Set<ShaderInstance> registeredShaders = new LinkedHashSet<>();

    private GleamLightEngine() {
        RenderSystem.assertOnRenderThread();
        this.lightStagingBuffer = ByteBuffer.allocateDirect(MAX_LIGHTS * (LIGHT_FLOATS * Float.BYTES)).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.sceneStagingBuffer = ByteBuffer.allocateDirect(SCENE_SIZE).order(ByteOrder.nativeOrder());
        this.lightBufferHandle = createBuffer((long) MAX_LIGHTS * (LIGHT_FLOATS * Float.BYTES));
        this.sceneBufferHandle = createBuffer(SCENE_SIZE);
    }

    public static synchronized GleamLightEngine getInstance() {
        if (instance == null) instance = new GleamLightEngine();
        return instance;
    }

    private static int createBuffer(long size) {
        int handle = GL30.glGenBuffers();
        GL30.glBindBuffer(GL31.GL_UNIFORM_BUFFER, handle);
        GL30.glBufferData(GL31.GL_UNIFORM_BUFFER, size, GL30.GL_DYNAMIC_DRAW);
        GL30.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
        return handle;
    }

    public void registerShader(ShaderInstance shader) {
        registeredShaders.add(shader);
    }

    public void clearShaders() {
        registeredShaders.clear();
    }

    public void rebindBlocks() {
        RenderSystem.assertOnRenderThread();
        for (ShaderInstance shader : registeredShaders) {
            int program = shader.getId();
            int idx = GL31.glGetUniformBlockIndex(program, "GleamLight");
            if (idx != GL31.GL_INVALID_INDEX) GL31.glUniformBlockBinding(program, idx, LIGHT_BINDING);
            idx = GL31.glGetUniformBlockIndex(program, "GleamScene");
            if (idx != GL31.GL_INVALID_INDEX) GL31.glUniformBlockBinding(program, idx, SCENE_BINDING);
        }
    }

    public void uploadLights(List<GleamLight> lights, double camX, double camY, double camZ) {
        RenderSystem.assertOnRenderThread();
        int count = packLights(lights);
        packScene(count, camX, camY, camZ);
        GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, LIGHT_BINDING, lightBufferHandle);
        GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, SCENE_BINDING, sceneBufferHandle);
    }

    private int packLights(List<GleamLight> lights) {
        lightStagingBuffer.clear();
        int count = 0;
        for (GleamLight light : lights) {
            if (count >= MAX_LIGHTS) break;
            lightStagingBuffer.put(light.r()).put(light.g()).put(light.b()).put(light.intensity());
            lightStagingBuffer.put(light.x()).put(light.y()).put(light.z());
            lightStagingBuffer.put(light.radius());
            count++;
        }
        lightStagingBuffer.flip();
        if (count > 0) {
            GL30.glBindBuffer(GL31.GL_UNIFORM_BUFFER, lightBufferHandle);
            GL30.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 0, lightStagingBuffer);
            GL30.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
        }
        return count;
    }

    private void packScene(int count, double camX, double camY, double camZ) {
        sceneStagingBuffer.clear();
        sceneStagingBuffer.putInt(count);
        sceneStagingBuffer.putInt(0);
        sceneStagingBuffer.putInt(0);
        sceneStagingBuffer.putInt(0);
        sceneStagingBuffer.putFloat((float) camX);
        sceneStagingBuffer.putFloat((float) camY);
        sceneStagingBuffer.putFloat((float) camZ);
        sceneStagingBuffer.putFloat(0f);
        sceneStagingBuffer.flip();
        GL30.glBindBuffer(GL31.GL_UNIFORM_BUFFER, sceneBufferHandle);
        GL30.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 0, sceneStagingBuffer);
        GL30.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
    }

    public void dispose() {
        if (lightBufferHandle != 0) GL30.glDeleteBuffers(lightBufferHandle);
        if (sceneBufferHandle != 0) GL30.glDeleteBuffers(sceneBufferHandle);
        registeredShaders.clear();
    }
}