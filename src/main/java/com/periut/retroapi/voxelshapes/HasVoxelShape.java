package com.periut.retroapi.voxelshapes;

import net.minecraft.world.World;

/**
 * Implement on a Block to give it a multi-box SELECTION shape: the mouse-over outline,
 * raytracing (you can mine through the gaps), and, unless {@link HasCollisionVoxelShape}
 * is also implemented, entity collision. Return null to fall back to vanilla handling
 * for that position. Ported from matthewperiut/voxelshapes.
 */
public interface HasVoxelShape {
    VoxelShape getVoxelShape(World world, int x, int y, int z);
}
