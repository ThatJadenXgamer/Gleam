package net.thatmaidenjaden.gleam.client.lighting;

import net.minecraft.world.level.block.Block;
import java.util.HashMap;
import java.util.Map;

public final class GleamEmitterRegistry {

    private record EmitterData(float r, float g, float b, float radius, float intensity) {}

    private static final Map<Block, EmitterData> EMISSION_MAP = new HashMap<>();

    private GleamEmitterRegistry() {}

    public static void clear() {
        EMISSION_MAP.clear();
    }

    public static void registerBlock(Block block, float r, float g, float b, float radius, float intensity) {
        EMISSION_MAP.put(block, new EmitterData(r, g, b, radius, intensity));
    }

    public static boolean isEmitter(Block block) {
        return EMISSION_MAP.containsKey(block);
    }

    public static GleamLight createLight(Block block, int x, int y, int z) {
        EmitterData e = EMISSION_MAP.get(block);
        return e == null ? null : GleamLight.create(x + 0.5f, y + 0.5f, z + 0.5f, e.r, e.g, e.b, e.radius, e.intensity);
    }
}