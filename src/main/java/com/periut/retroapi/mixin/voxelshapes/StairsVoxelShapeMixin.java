package com.periut.retroapi.mixin.voxelshapes;

import com.periut.retroapi.voxelshapes.HasVoxelShape;
import com.periut.retroapi.voxelshapes.VoxelData;
import com.periut.retroapi.voxelshapes.VoxelShape;
import com.periut.retroapi.voxelshapes.VoxelBox;
import net.minecraft.block.StairsBlock;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(StairsBlock.class)
public class StairsVoxelShapeMixin implements HasVoxelShape {
    @Unique
    private static final VoxelData retroapi$BASE_VOXEL_DATA = new VoxelData(new VoxelBox(0, 0, 0, 1, 0.5, 1));
    @Unique
    private static final VoxelData[] retroapi$VOXEL_DATUM = new VoxelData[4];

    static {
        retroapi$VOXEL_DATUM[0] = retroapi$BASE_VOXEL_DATA.withBox(new VoxelBox(.5, .5, 0, 1, 1, 1)).preCache();
        retroapi$VOXEL_DATUM[1] = retroapi$BASE_VOXEL_DATA.withBox(new VoxelBox(0, .5, 0, .5, 1, 1)).preCache();
        retroapi$VOXEL_DATUM[2] = retroapi$BASE_VOXEL_DATA.withBox(new VoxelBox(0, .5, .5, 1, 1, 1)).preCache();
        retroapi$VOXEL_DATUM[3] = retroapi$BASE_VOXEL_DATA.withBox(new VoxelBox(0, .5, 0, 1, 1, .5)).preCache();
    }

    @Override
    public VoxelShape getVoxelShape(World world, int x, int y, int z) {
        int meta = world.getBlockMeta(x, y, z);
        if (meta < 0 || meta > 3) return null;
        return retroapi$VOXEL_DATUM[meta].withOffset(x, y, z);
    }
}
