package com.periut.retrotweaks.mixin.block;

import com.periut.retrotweaks.config.Config;

import net.minecraft.block.Block;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

/**
 * Stairs drop stairs rather than the block they were made from. From AnnoyanceFix and UniTweaks.
 *
 * <p>Vanilla {@code StairsBlock} forwards breaking to its base block, which is why a broken
 * staircase gives back cobblestone or planks. All four of those forwards are stopped together
 * because leaving any one of them would still leak the base block on that particular path
 * (mined, exploded, or dropped with a luck roll).
 */
@Mixin(StairsBlock.class)
public abstract class StairsBlockMixin extends Block {

	protected StairsBlockMixin(int id, Material material) {
		super(id, material);
	}

	@Inject(method = "onBreak", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$dontForwardBreak(World world, int x, int y, int z, CallbackInfo ci) {
		if (Config.BLOCKS.stairFixes) ci.cancel();
	}

	@Inject(method = "onDestroyedByExplosion", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$dontForwardExplosion(World world, int x, int y, int z, CallbackInfo ci) {
		if (Config.BLOCKS.stairFixes) ci.cancel();
	}

	@Inject(method = "getDroppedItemId", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$dropStairs(int blockMeta, Random random, CallbackInfoReturnable<Integer> cir) {
		if (Config.BLOCKS.stairFixes) cir.setReturnValue(this.id);
	}

	@Inject(method = "dropStacks", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$dropStairStacks(World world, int x, int y, int z, int meta, float luck, CallbackInfo ci) {
		if (!Config.BLOCKS.stairFixes) return;
		super.dropStacks(world, x, y, z, meta, luck);
		ci.cancel();
	}
}
