package com.periut.retrotweaks.mixin.entity;

import com.periut.retrotweaks.config.Config;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A burning skeleton shoots burning arrows. From UniTweaks (Better Burning). */
@Mixin(SkeletonEntity.class)
public abstract class BetterBurningMixin extends Entity {

	protected BetterBurningMixin(World world) {
		super(world);
	}

	@WrapOperation(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z"))
	private boolean retrotweaks$lightTheArrow(World world, Entity arrow, Operation<Boolean> original) {
		Config.BetterBurning burning = Config.MOBS.betterBurning;
		if (burning.enabled && burning.skeletonsBurningArrows && this.fireTicks > 0
			&& this.world.random.nextInt(100) < burning.skeletonBurningArrowChance) {
			// 400 ticks outlasts any realistic flight time, so the arrow is still alight on impact.
			arrow.fireTicks = 400;
		}
		return original.call(world, arrow);
	}
}
