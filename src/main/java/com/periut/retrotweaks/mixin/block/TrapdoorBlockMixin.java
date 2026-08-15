package com.periut.retrotweaks.mixin.block;

import com.periut.retrotweaks.config.Config;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.block.Block;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Trapdoor placement without support. From GameplayEssentials and UniTweaks - and ONLY that: this
 * class is skipped wholesale when UniTweaks is installed, so nothing that must survive UniTweaks may
 * live here. The glued-trapdoor enforcement used to, and died with the skip while ItemUseMixin kept
 * consuming the slimeball; it now lives in {@link GluedTrapdoorMixin}.
 */
@Mixin(TrapdoorBlock.class)
public abstract class TrapdoorBlockMixin extends Block {

	protected TrapdoorBlockMixin(int id, Material material) {
		super(id, material);
	}

	@WrapOperation(method = "neighborUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;shouldSuffocate(III)Z"))
	private boolean retrotweaks$neverPopOff(World world, int x, int y, int z, Operation<Boolean> original) {
		return Config.BLOCKS.trapdoorsWithoutSupport || original.call(world, x, y, z);
	}

	@Inject(method = "canPlaceAt", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$placeWithoutSupport(World world, int x, int y, int z, int side, CallbackInfoReturnable<Boolean> cir) {
		// Still refuses to replace a solid block; it only drops the "must lean on something" rule.
		if (Config.BLOCKS.trapdoorsWithoutSupport) cir.setReturnValue(!world.getMaterial(x, y, z).isSolid());
	}
}
