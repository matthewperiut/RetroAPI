package com.periut.retrotweaks.mixin.entity;

import com.periut.retrotweaks.config.Config;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.block.FireBlock;
import net.minecraft.entity.LightningEntity;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Lightning strikes without setting anything alight. From MiscTweaks.
 *
 * <p>Vanilla places fire both when the bolt spawns and again while it lingers, so both have to be
 * stopped or the second one starts the fire the first one skipped.
 */
@Mixin(LightningEntity.class)
public class LightningEntityMixin {

	@WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/FireBlock;canPlaceAt(Lnet/minecraft/world/World;III)Z"))
	private boolean retrotweaks$noFireOnStrike(FireBlock fire, World world, int x, int y, int z, Operation<Boolean> original) {
		if (Config.WORLD.fire.lightningCausesFire) return original.call(fire, world, x, y, z);
		return false;
	}

	@WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/FireBlock;canPlaceAt(Lnet/minecraft/world/World;III)Z"))
	private boolean retrotweaks$noFireWhileLingering(FireBlock fire, World world, int x, int y, int z, Operation<Boolean> original) {
		if (Config.WORLD.fire.lightningCausesFire) return original.call(fire, world, x, y, z);
		return false;
	}
}
