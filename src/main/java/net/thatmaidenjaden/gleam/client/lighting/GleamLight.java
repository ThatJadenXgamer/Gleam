package net.thatmaidenjaden.gleam.client.lighting;

public record GleamLight(float x, float y, float z, float r, float g, float b, float intensity, float radius) {

    public static GleamLight create(float x, float y, float z, float r, float g, float b, float radius, float intensity) {
        return new GleamLight(x, y, z, r, g, b, intensity, radius);
    }

    public double distanceSquaredTo(double x, double y, double z) {
        double dx = this.x - x;
        double dy = this.y - y;
        double dz = this.z - z;
        return dx * dx + dy * dy + dz * dz;
    }
}