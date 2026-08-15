package com.periut.retrotweaks.mixin.block;

import com.periut.retrotweaks.config.Config;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.block.Block;
import net.minecraft.block.ChestBlock;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Chests open with something on top of them.
 *
 * <p>Two options share this hook because they are the same vanilla check. "Stackable chests"
 * (UniTweaks) only ignores another chest above, which is what lets chests be stacked into a wall.
 * "Chests open when blocked" (MiscTweaks) ignores anything at all.
 */
@Mixin(ChestBlock.class)
public class ChestBlockMixin {

	@WrapOperation(method = "onUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;shouldSuffocate(III)Z"))
	private boolean retrotweaks$openWhenBlocked(World world, int x, int y, int z, Operation<Boolean> original) {
		if (Config.BLOCKS.chestsOpenWhenBlocked) return false;
		if (Config.BLOCKS.stackableChests && retrotweaks$isChest(world, x, y, z)) return false;
		return original.call(world, x, y, z);
	}

	@Unique
	private static boolean retrotweaks$isChest(World world, int x, int y, int z) {
		int blockId = world.getBlockId(x, y, z);
		return blockId > 0 && Block.BLOCKS[blockId] instanceof ChestBlock;
	}
}
