package com.periut.retrotweaks.mixin.block;

import com.periut.retrotweaks.config.Config;

import net.minecraft.block.Block;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stops a slab merging into a double slab when it is placed on another slab of the same type.
 *
 * <p>Vanilla merges them, which makes stacking two slabs to build a step impossible. From
 * AnnoyanceFix and UniTweaksTelsAddons.
 */
@Mixin(SlabBlock.class)
public abstract class SlabBlockMixin extends Block {

	protected SlabBlockMixin(int id, Material material) {
		super(id, material);
	}

	@Inject(method = "onPlaced(Lnet/minecraft/world/World;III)V", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$dontMergeSlabs(World world, int x, int y, int z, CallbackInfo ci) {
		if (!Config.BLOCKS.slabPlacementFixes) return;
		super.onPlaced(world, x, y, z);
		ci.cancel();
	}
}
