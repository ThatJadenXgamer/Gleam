package net.thatmaidenjaden.gleam.client.lighting;

import net.minecraft.world.phys.AABB;

public record GleamLight(float x, float y, float z, float r, float g, float b, float intensity, float radius, AABB box) {

    public static GleamLight create(float x, float y, float z, float r, float g, float b, float radius, float intensity) {
        AABB cachedBox = new AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
        return new GleamLight(x, y, z, r, g, b, intensity, radius, cachedBox);
    }
}