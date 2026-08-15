package com.periut.retrotweaks.mixin.entity;

import com.periut.retrotweaks.config.Config;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A burning arrow sets what it hits alight. From UniTweaks (Better Burning). */
@Mixin(ArrowEntity.class)
public abstract class ArrowEntityBurningMixin extends Entity {

	protected ArrowEntityBurningMixin(World world) {
		super(world);
	}

	@WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/Entity;I)Z"))
	private boolean retrotweaks$igniteOnHit(Entity target, Entity source, int amount, Operation<Boolean> original) {
		Config.BetterBurning burning = Config.MOBS.betterBurning;
		if (burning.enabled && burning.burningArrowsSetOnFire && this.fireTicks > 0) {
			target.fireTicks = 100;
		}
		return original.call(target, source, amount);
	}
}
