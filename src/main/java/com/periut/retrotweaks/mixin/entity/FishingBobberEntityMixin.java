package com.periut.retrotweaks.mixin.entity;

import com.periut.retrotweaks.config.Config;
import com.periut.retrotweaks.feature.fishing.Fishing;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Caught fish arc towards the player instead of flying over their head.
 *
 * <p>Vanilla launches the fish with a fixed upward velocity that ignores where the player is, so
 * reeling in from close range throws it behind you. From GameplayEssentials and UniTweaks.
 */
@Mixin(FishingBobberEntity.class)
public abstract class FishingBobberEntityMixin extends Entity {

	protected FishingBobberEntityMixin(World world) {
		super(world);
	}

	@WrapOperation(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z"))
	private boolean retrotweaks$aimFishAtPlayer(World world, Entity fish, Operation<Boolean> original) {
		if (fish instanceof ItemEntity caught) {
			// Size (and, with RetroAPI, species) is rolled from the water around the bobber, so it
			// has to happen here rather than when the fish is eaten.
			Fishing.rollCatch(caught, world, this.x, this.y, this.z);
		}
		if (Config.BUGFIXES.fishVelocityFix && fish != null) {
			FishingBobberEntity bobber = (FishingBobberEntity) (Object) this;
			double dx = bobber.owner.x - bobber.x;
			double dy = bobber.owner.y - bobber.y;
			double dz = bobber.owner.z - bobber.z;
			fish.velocityX = dx * 0.1;
			fish.velocityY = dy * 0.1 + Math.sqrt(Math.sqrt(dx * dx + dy * dy + dz * dz)) * 0.05;
			fish.velocityZ = dz * 0.1;
		}
		return original.call(world, fish);
	}
}
