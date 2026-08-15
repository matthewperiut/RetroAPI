package com.periut.retrotweaks.mixin.entity;

import com.periut.retrotweaks.config.Config;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Being hit by a burning mob can set you alight. From UniTweaks (Better Burning).
 *
 * <p>Skeletons are excluded because their contribution is the burning arrow, which already lights
 * the target; counting the melee hit too would double up.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerBurningSpreadMixin extends Entity {

	protected PlayerBurningSpreadMixin(World world) {
		super(world);
	}

	@Inject(method = "damage(Lnet/minecraft/entity/Entity;I)Z", at = @At("HEAD"))
	private void retrotweaks$catchFireFromAttacker(Entity attacker, int amount, CallbackInfoReturnable<Boolean> cir) {
		Config.BetterBurning burning = Config.MOBS.betterBurning;
		if (!burning.enabled || !burning.burningEntitySpread || this.world.isRemote) return;
		if (attacker == null || attacker instanceof SkeletonEntity || attacker.fireTicks <= 0) return;
		if (this.world.random.nextInt(100) < burning.burningEntitySpreadChance) this.fireTicks = 100;
	}
}
