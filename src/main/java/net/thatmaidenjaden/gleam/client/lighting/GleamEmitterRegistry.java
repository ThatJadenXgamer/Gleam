package net.thatmaidenjaden.gleam.client.lighting;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class GleamEmitterRegistry {

    private record EmitterData(float r, float g, float b, boolean blacklight, boolean occludeToBlocklight, float radius, float intensity) {}

    private record ConditionalEmitter(Map<String, String> condition, EmitterData data) {}

    private static final Map<Block, List<ConditionalEmitter>> EMISSION_MAP = new IdentityHashMap<>();

    private GleamEmitterRegistry() {}

    public static void clear() {
        EMISSION_MAP.clear();
    }

    public static void registerEmitter(ResourceLocation blockId, Map<String, String> condition, float r, float g, float b, float radius, float intensity, boolean blacklight, boolean occludeToBlocklight) {
        Optional<Block> optionalBlock = BuiltInRegistries.BLOCK.getOptional(blockId);
        if (optionalBlock.isEmpty()) return;

        Block block = optionalBlock.get();
        EMISSION_MAP.computeIfAbsent(block, k -> new ArrayList<>()).add(new ConditionalEmitter(condition, new EmitterData(r, g, b, blacklight, occludeToBlocklight, radius, intensity)));
    }

    public static boolean isEmitter(BlockState state) {
        return getEmitterData(state) != null;
    }

    public static GleamLight createLight(BlockState state, int x, int y, int z) {
        EmitterData data = getEmitterData(state);
        if (data == null) return null;
        return GleamLight.create(x + 0.5f, y + 0.5f, z + 0.5f, data.r, data.g, data.b, data.radius, data.intensity, data.blacklight, data.occludeToBlocklight);
    }

    private static EmitterData getEmitterData(BlockState state) {
        List<ConditionalEmitter> list = EMISSION_MAP.get(state.getBlock());
        if (list == null) return null;
        for (ConditionalEmitter conditionalEmitter : list) if (matchesCondition(state, conditionalEmitter.condition)) return conditionalEmitter.data;
        return null;
    }

    private static boolean matchesCondition(BlockState state, Map<String, String> condition) {
        if (condition.isEmpty()) return true;
        for (Map.Entry<String, String> entry : condition.entrySet()) {
            String propertyName = entry.getKey();
            String expectedValue = entry.getValue();
            Property property = state.getBlock().getStateDefinition().getProperty(propertyName);
            if (property == null) return false;
            if (!expectedValue.equals(property.getName(state.getValue(property)))) return false;
        }
        return true;
    }
}