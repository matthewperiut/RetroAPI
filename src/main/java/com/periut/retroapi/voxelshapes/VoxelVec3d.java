package com.periut.retroapi.voxelshapes;

import net.minecraft.util.math.Vec3d;

import java.util.Arrays;

public record VoxelVec3d(double x, double y, double z) {
    public static VoxelVec3d ZERO = new VoxelVec3d(0, 0, 0);

    public static VoxelVec3d voxelify(Vec3d vec) {
        return new VoxelVec3d(vec.x, vec.y, vec.z);
    }

    public static Vec3d devoxelify(VoxelVec3d voxelVec) {
        return Vec3d.create(voxelVec.x, voxelVec.y, voxelVec.z);
    }

    public static VoxelVec3d[] voxelify(Vec3d[] vecs) {
        return Arrays.stream(vecs).map(VoxelVec3d::voxelify).toArray(VoxelVec3d[]::new);
    }

    public static Vec3d[] devoxelify(VoxelVec3d[] voxelVecs) {
        return Arrays.stream(voxelVecs).map(VoxelVec3d::devoxelify).toArray(Vec3d[]::new);
    }

    public VoxelVec3d add(double x, double y, double z) {
        return new VoxelVec3d(this.x + x, this.y + y, this.z + z);
    }

    public VoxelVec3d add(VoxelVec3d other) {
        return add(other.x, other.y, other.z);
    }

    public VoxelVec3d add(Vec3d other) {
        return add(other.x, other.y, other.z);
    }

    public VoxelVec3d subtract(double x, double y, double z) {
        return new VoxelVec3d(this.x - x, this.y - y, this.z - z);
    }

    public VoxelVec3d subtract(VoxelVec3d other) {
        return subtract(other.x, other.y, other.z);
    }

    public VoxelVec3d subtract(Vec3d other) {
        return subtract(other.x, other.y, other.z);
    }

    public VoxelVec3d multiply(double scalar) {
        return new VoxelVec3d(x * scalar, y * scalar, z * scalar);
    }

    public double distanceTo(VoxelVec3d other) {
        return Math.sqrt(
                Math.pow(x - other.x, 2) +
                Math.pow(y - other.y, 2) +
                Math.pow(z - other.z, 2)
        );
    }

    public double distanceTo(Vec3d other) {
        return Math.sqrt(
                Math.pow(x - other.x, 2) +
                Math.pow(y - other.y, 2) +
                Math.pow(z - other.z, 2)
        );
    }

    // Yes, this was ripped directly from StationAPI.
    // Why do you ask?
    public VoxelVec3d normalize() {
        double d = Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
        if (d < 1.0E-4) {
            return ZERO;
        }
        return new VoxelVec3d(this.x / d, this.y / d, this.z / d);
    }
}
