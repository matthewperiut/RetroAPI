package com.periut.retrotweaks.mixin.item;

import com.periut.retrotweaks.config.Config;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The general-block half of plant replacement: lets any block be placed straight into tall
 * grass/dead bush by redirecting {@code BlockItem.useOnBlock}'s placement-target lookup to treat
 * grass/dead bush as snow (a replaceable block), then forcing the follow-up canPlace check to
 * succeed. The slab-specific half lives in {@link SlabBlockItemMixin}, which overrides
 * {@code useOnBlock} entirely instead of going through this class.
 *
 * <p>From AnnoyanceFix and UniTweaksTelsAddons (identical implementations in both).
 */
@Mixin(BlockItem.class)
public class BlockItemMixin {

	@Unique private boolean retrotweaks$replaceBlock = false;

	@WrapOperation(method = "useOnBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockId(III)I"))
	private int retrotweaks$useOnBlockId(World instance, int x, int y, int z, Operation<Integer> original) {
		int blockId = original.call(instance, x, y, z);

		if (Block.GRASS.id == blockId || Block.DEAD_BUSH.id == blockId) {
			if (Config.BLOCKS.plantReplacementFixes) {
				blockId = Block.SNOW.id;
				retrotweaks$replaceBlock = true;
			}
		}

		return blockId;
	}

	@WrapOperation(method = "useOnBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;canPlace(IIIIZI)Z"))
	private boolean retrotweaks$useOnBlockCanPlace(World instance, int blockId, int x, int y, int z, boolean fallingBlock, int side, Operation<Boolean> original) {
		boolean canPlace = original.call(instance, blockId, x, y, z, fallingBlock, side);

		if (retrotweaks$replaceBlock) {
			canPlace = true;
			retrotweaks$replaceBlock = false;
		}

		return canPlace;
	}
}
