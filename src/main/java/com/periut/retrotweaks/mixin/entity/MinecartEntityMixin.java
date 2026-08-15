package com.periut.retrotweaks.mixin.entity;

import com.periut.retrotweaks.config.Config;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.util.math.Box;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Dropped items and stray arrows no longer stop a minecart dead. From GameplayEssentials and
 * UniTweaks.
 */
@Mixin(MinecartEntity.class)
public class MinecartEntityMixin {

	@Inject(method = "getCollisionAgainstShape", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$ignoreItemsAndArrows(Entity other, CallbackInfoReturnable<Box> cir) {
		if (!Config.BUGFIXES.minecartStoppingOnItemsFix) return;
		if (other instanceof ItemEntity || other instanceof ArrowEntity) cir.setReturnValue(null);
	}
}
