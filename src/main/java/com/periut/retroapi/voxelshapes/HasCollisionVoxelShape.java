package com.periut.retroapi.voxelshapes;

import net.minecraft.world.World;

/**
 * Implement alongside {@link HasVoxelShape} when the COLLISION shape differs from the
 * selection shape (fences and walls collide 1.5 blocks tall but select at their visual
 * height). Return null to fall back. Ported from matthewperiut/voxelshapes.
 */
public interface HasCollisionVoxelShape {
    VoxelShape getCollisionVoxelShape(World world, int x, int y, int z);
}
