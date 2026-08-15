package com.periut.retrotweaks.mixin.block;

import com.periut.retrotweaks.config.Config;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.block.Block;
import net.minecraft.block.PressurePlateBlock;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Lets a pressure plate sit on a fence, for gate-style contraptions. From GameplayEssentials and
 * UniTweaks.
 *
 * <p>Vanilla asks whether the block below suffocates, which a fence does not. Both the placement
 * check and the neighbour update have to agree, or the plate places and then immediately pops off.
 */
@Mixin(PressurePlateBlock.class)
public class PressurePlateBlockMixin {

	@WrapOperation(method = "canPlaceAt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;shouldSuffocate(III)Z"))
	private boolean retrotweaks$canPlaceOnFence(World world, int x, int y, int z, Operation<Boolean> original) {
		if (!Config.BLOCKS.pressurePlatesOnFences) return original.call(world, x, y, z);
		return original.call(world, x, y, z) || world.getBlockId(x, y, z) == Block.FENCE.id;
	}

	@WrapOperation(method = "neighborUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;shouldSuffocate(III)Z"))
	private boolean retrotweaks$stayOnFence(World world, int x, int y, int z, Operation<Boolean> original) {
		if (!Config.BLOCKS.pressurePlatesOnFences) return original.call(world, x, y, z);
		return original.call(world, x, y, z) || world.getBlockId(x, y, z) == Block.FENCE.id;
	}
}
