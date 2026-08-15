package com.periut.retrotweaks.mixin.entity;

import com.periut.retrotweaks.config.Config;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResultType;
import net.minecraft.world.World;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Snowballs put out the fire they land on. From MiscTweaks.
 *
 * <p>Hooked on the first read of the hit result's entity, which is the point vanilla has a hit
 * result in hand and has not yet branched on what it struck.
 */
@Mixin(SnowballEntity.class)
public abstract class SnowballEntityMixin extends Entity {

	protected SnowballEntityMixin(World world) {
		super(world);
	}

	@WrapOperation(method = "tick", at = @At(value = "FIELD",
		target = "Lnet/minecraft/util/hit/HitResult;entity:Lnet/minecraft/entity/Entity;",
		opcode = Opcodes.GETFIELD, ordinal = 0), require = 0)
	private Entity retrotweaks$extinguishOnLand(HitResult hit, Operation<Entity> original) {
		if (Config.WORLD.fire.snowballsExtinguishFire && hit.type == HitResultType.BLOCK) {
			this.world.extinguishFire(null, hit.blockX, hit.blockY, hit.blockZ, hit.side);
		}
		return original.call(hit);
	}
}
