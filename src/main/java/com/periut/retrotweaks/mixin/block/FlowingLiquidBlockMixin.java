package com.periut.retrotweaks.mixin.block;

import com.periut.retrotweaks.config.Config;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.block.Block;
import net.minecraft.block.FlowingLiquidBlock;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

/**
 * Two liquid bugs, from AnnoyanceFix and UniTweaks.
 *
 * <p><b>Lava.</b> Vanilla has a branch that skips the flow update for lava, so lava left behind
 * after its source is removed never disappears. Reporting the material as water for that one check
 * lets the existing update code run.
 *
 * <p><b>Water.</b> Vanilla reads the metadata of the block being checked when deciding whether a
 * source can form, rather than the one below it, so a water source will not form over flowing
 * water. Redirecting the lookup one block down is the fix.
 */
@Mixin(FlowingLiquidBlock.class)
public class FlowingLiquidBlockMixin {

	@WrapOperation(method = "onTick", at = @At(value = "FIELD",
		target = "Lnet/minecraft/block/FlowingLiquidBlock;material:Lnet/minecraft/block/material/Material;",
		opcode = Opcodes.GETFIELD, ordinal = 3))
	private Material retrotweaks$lavaDisappears(FlowingLiquidBlock block, Operation<Material> original) {
		if (Config.BLOCKS.lavaFixes) return Material.WATER;
		return original.call(block);
	}

	@WrapOperation(method = "onTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockMeta(III)I"))
	private int retrotweaks$springPropagation(World world, int x, int y, int z, Operation<Integer> original) {
		if (Config.BLOCKS.waterFixes) return original.call(world, x, y - 1, z);
		return original.call(world, x, y, z);
	}

	/**
	 * Liquid flowing down destroys whatever is below it. Vanilla replaces the block outright, so a
	 * torch or rail is simply deleted; this drops it first. From UniTweaks.
	 */
	@Inject(method = "onTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlock(IIIII)Z"))
	private void retrotweaks$dropWashedAwayBlock(World world, int x, int y, int z, Random random, CallbackInfo ci) {
		if (!Config.BUGFIXES.liquidBlockDropFix) return;
		int id = world.getBlockId(x, y - 1, z);
		if (id == 0) return;
		Block.BLOCKS[id].dropStacks(world, x, y - 1, z, world.getBlockMeta(x, y - 1, z));
	}
}
