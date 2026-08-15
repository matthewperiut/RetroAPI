package com.periut.retrotweaks.mixin.block;

import com.periut.retrotweaks.config.Config;

import net.minecraft.block.Block;
import net.minecraft.block.FenceBlock;
import net.minecraft.util.math.Box;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fence placement and shape, from AnnoyanceFix and UniTweaks.
 *
 * <p>Vanilla only lets a fence be placed on a solid block or another fence, and gives every fence a
 * full-block collision box regardless of what it is connected to. Both are fixed here, separately,
 * so either can be turned off on its own.
 */
@Mixin(FenceBlock.class)
public abstract class FenceBlockMixin extends Block {

	protected FenceBlockMixin(int id, net.minecraft.block.material.Material material) {
		super(id, material);
	}

	@Inject(method = "canPlaceAt", at = @At("RETURN"), cancellable = true)
	private void retrotweaks$placeAnywhere(World world, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
		if (!Config.BLOCKS.fencePlacementFixes) return;
		int blockId = world.getBlockId(x, y, z);
		cir.setReturnValue(blockId == 0 || Block.BLOCKS[blockId].material.isReplaceable());
	}

	@Inject(method = "getCollisionShape", at = @At("RETURN"), cancellable = true)
	private void retrotweaks$connectedCollisionShape(World world, int x, int y, int z, CallbackInfoReturnable<Box> cir) {
		if (!Config.BLOCKS.fenceShapeFixes) return;
		cir.setReturnValue(retrotweaks$fenceBox(world, x, y, z, true));
	}

	@Override
	public Box getBoundingBox(World world, int x, int y, int z) {
		if (!Config.BLOCKS.fenceShapeFixes) return super.getBoundingBox(world, x, y, z);
		// Remembering the world here is what lets updateBoundingBox below see the neighbours; the
		// vanilla signature only hands it a BlockView, which cannot answer "is that a fence?".
		retrotweaks$lastWorld = world;
		return retrotweaks$fenceBox(world, x, y, z, false);
	}

	@Override
	public void updateBoundingBox(BlockView blockView, int x, int y, int z) {
		if (!Config.BLOCKS.fenceShapeFixes || retrotweaks$lastWorld == null) {
			super.updateBoundingBox(blockView, x, y, z);
			return;
		}
		Box box = retrotweaks$rawFenceBox(retrotweaks$lastWorld, x, y, z);
		setBoundingBox((float) box.minX, (float) box.minY, (float) box.minZ,
			(float) box.maxX, (float) box.maxY, (float) box.maxZ);
	}

	@Unique
	private static World retrotweaks$lastWorld;

	/** The fence's shape in block-local coordinates, narrowed on any side with no neighbour. */
	@Unique
	private Box retrotweaks$rawFenceBox(World world, int x, int y, int z) {
		int fenceId = Block.FENCE.id;
		boolean posX = retrotweaks$connects(world, x + 1, y, z, fenceId);
		boolean negX = retrotweaks$connects(world, x - 1, y, z, fenceId);
		boolean posZ = retrotweaks$connects(world, x, y, z + 1, fenceId);
		boolean negZ = retrotweaks$connects(world, x, y, z - 1, fenceId);
		return Box.create(
			negX ? 0.0F : 0.375F, 0.0F, negZ ? 0.0F : 0.375F,
			posX ? 1.0F : 0.625F, 1.0F, posZ ? 1.0F : 0.625F);
	}

	@Unique
	private boolean retrotweaks$connects(World world, int x, int y, int z, int fenceId) {
		int id = world.getBlockId(x, y, z);
		if (id == fenceId) return true;
		// "Fences connect to blocks" also joins them to anything solid, the way modern fences do.
		return Config.BLOCKS.fencesConnectBlocks && id != 0 && Block.BLOCKS[id] != null && Block.BLOCKS[id].isFullCube();
	}

	@Unique
	private Box retrotweaks$fenceBox(World world, int x, int y, int z, boolean collision) {
		Box box = retrotweaks$rawFenceBox(world, x, y, z);
		box.minX += x;
		box.minY += y;
		box.minZ += z;
		box.maxX += x;
		box.maxY += y;
		box.maxZ += z;
		// Vanilla makes fences a block and a half tall so mobs cannot jump them.
		if (collision) box.maxY += 0.5F;
		return box;
	}
}
