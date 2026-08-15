package com.periut.retrotweaks.mixin.block;

import com.periut.retrotweaks.config.Config;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.block.Block;
import net.minecraft.block.SugarCaneBlock;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Sugar cane grows on sand as well as dirt and grass. From GameplayEssentials and UniTweaks.
 *
 * <p>Vanilla's check reads the block below and compares it to dirt and grass, so telling it that
 * sand is dirt is enough - no need to duplicate the rest of the placement rules (still needs water
 * beside it) and risk them drifting apart.
 */
@Mixin(SugarCaneBlock.class)
public class SugarCaneBlockMixin {

	@WrapOperation(method = "canPlaceAt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockId(III)I"))
	private int retrotweaks$sandCountsAsDirt(World world, int x, int y, int z, Operation<Integer> original) {
		int blockId = original.call(world, x, y, z);
		if (Config.BLOCKS.sugarCaneOnSand && blockId == Block.SAND.id) return Block.DIRT.id;
		return blockId;
	}
}
