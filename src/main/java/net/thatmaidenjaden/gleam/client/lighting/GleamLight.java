package net.thatmaidenjaden.gleam.client.lighting;

import net.minecraft.world.phys.AABB;

public record GleamLight(float x, float y, float z, float r, float g, float b, boolean blacklight, boolean occludeToBlocklight, float intensity, float radius, AABB box) {

    public static GleamLight create(float x, float y, float z, float r, float g, float b, float radius, float intensity, boolean blacklight, boolean occludeToBlocklight) {
        AABB cachedBox = new AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
        return new GleamLight(x, y, z, r, g, b, blacklight, occludeToBlocklight, intensity, radius, cachedBox);
    }
}