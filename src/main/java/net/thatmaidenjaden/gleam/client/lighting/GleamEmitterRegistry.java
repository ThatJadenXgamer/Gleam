package net.thatmaidenjaden.gleam.client.lighting;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import java.util.Map;

public final class GleamEmitterRegistry {

    private record EmitterData(float r, float g, float b, float radius) {}

    private static final Map<Block, EmitterData> EMISSION_MAP = Map.of(
            Blocks.SOUL_FIRE, new EmitterData(0.2f, 0.6f, 1.0f, 8.0f),
            Blocks.REDSTONE_TORCH, new EmitterData(1.0f, 0.2f, 0.2f, 4.0f)
    );

    private GleamEmitterRegistry() {}

    public static boolean isEmitter(Block block) {
        return EMISSION_MAP.containsKey(block);
    }

    public static GleamLight createLight(Block block, int x, int y, int z) {
        EmitterData e = EMISSION_MAP.get(block);
        return e == null ? null : GleamLight.create(x + 0.5f, y + 0.5f, z + 0.5f, e.r, e.g, e.b, e.radius);
    }
}