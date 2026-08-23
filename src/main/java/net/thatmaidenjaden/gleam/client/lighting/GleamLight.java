package net.thatmaidenjaden.gleam.client.lighting;

public record GleamLight(float x, float y, float z, float r, float g, float b, float a, float radius) {

    public static GleamLight create(float x, float y, float z, float r, float g, float b, float radius) {
        return new GleamLight(x, y, z, r, g, b, 1.0f, radius);
    }

    public double distanceSquaredTo(double x, double y, double z) {
        double dx = this.x - x;
        double dy = this.y - y;
        double dz = this.z - z;
        return dx * dx + dy * dy + dz * dz;
    }
}