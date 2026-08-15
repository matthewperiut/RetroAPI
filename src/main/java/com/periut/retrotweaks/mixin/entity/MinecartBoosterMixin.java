package com.periut.retrotweaks.mixin.entity;

import com.periut.retrotweaks.config.Config;

import net.minecraft.entity.vehicle.MinecartEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Booster minecarts: two carts pushing each other accelerate without limit. From BetaTweaks and
 * UniTweaks.
 *
 * <p>Vanilla damps the transferred momentum when carts collide, which is exactly what killed the
 * classic booster. Zeroing that damping term brings it back.
 */
@Mixin(MinecartEntity.class)
public class MinecartBoosterMixin {

	@ModifyVariable(method = "onCollision", at = @At("STORE"), ordinal = 6)
	private double retrotweaks$minecartBoosters(double damping) {
		return Config.GAMEPLAY.oldFeatures.minecartBoosters ? 0.0 : damping;
	}
}
