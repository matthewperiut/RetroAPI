package com.periut.retrotweaks.mixin.item;

import com.periut.retrotweaks.config.Config;

import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.LogBlockItem;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;

/**
 * Records the axis a log was placed along. See {@link com.periut.retrotweaks.mixin.block.LogBlockMixin}.
 *
 * <p>An override rather than an inject: {@code LogBlockItem} does not declare {@code useOnBlock},
 * it inherits it from {@code BlockItem}. Vanilla places the block first, then this sets the axis
 * bits on it - which is one step back along the face that was clicked.
 */
@Mixin(LogBlockItem.class)
public abstract class LogBlockItemMixin extends BlockItem {

	protected LogBlockItemMixin(int id) {
		super(id);
	}

	@Override
	public boolean useOnBlock(ItemStack stack, PlayerEntity user, World world, int x, int y, int z, int side) {
		boolean placed = super.useOnBlock(stack, user, world, x, y, z, side);
		if (!placed || !Config.BLOCKS.logRotation) return placed;
		// Snow is replaced in place rather than built against, so it keeps the upright default.
		if (world.getBlockId(x, y, z) == Block.SNOW.id) return placed;

		int axis;
		switch (side) {
			case 2 -> { z--; axis = 0x4; }
			case 3 -> { z++; axis = 0x4; }
			case 4 -> { x--; axis = 0x8; }
			case 5 -> { x++; axis = 0x8; }
			default -> { return placed; } // top or bottom: upright, which is metadata 0
		}
		world.setBlockMeta(x, y, z, world.getBlockMeta(x, y, z) | axis);
		return placed;
	}
}
