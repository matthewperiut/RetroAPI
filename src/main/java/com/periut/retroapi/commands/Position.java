package com.periut.retroapi.commands;

/**
 * A position in world space.
 *
 * <p>Beta's own vector class is tied to a per-tick pool that is cleared out from under you, so
 * command code carries this instead.
 */
public record Position(double x, double y, double z) {
    public static final Position ORIGIN = new Position(0.0, 0.0, 0.0);

    public int blockX() {
        return (int) Math.floor(x);
    }

    public int blockY() {
        return (int) Math.floor(y);
    }

    public int blockZ() {
        return (int) Math.floor(z);
    }

    public double distanceTo(final Position other) {
        final double dx = x - other.x;
        final double dy = y - other.y;
        final double dz = z - other.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public double squaredDistanceTo(final double ox, final double oy, final double oz) {
        final double dx = x - ox;
        final double dy = y - oy;
        final double dz = z - oz;
        return dx * dx + dy * dy + dz * dz;
    }

    @Override
    public String toString() {
        return String.format("%.2f, %.2f, %.2f", x, y, z);
    }
}
