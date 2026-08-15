package com.periut.retrotweaks.mixin.entity;

import com.periut.retrotweaks.config.Config;

import net.minecraft.entity.passive.ChickenEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Chickens get their modern hitbox, which is noticeably easier to hit. From AnnoyanceFix and
 * UniTweaks. Applied at construction, so existing chickens pick it up on chunk reload.
 */
@Mixin(ChickenEntity.class)
public class ChickenEntityMixin {

	@ModifyConstant(method = "<init>", constant = @Constant(floatValue = 0.3F))
	private float retrotweaks$widerChicken(float vanilla) {
		return Config.MOBS.expandChickenHitbox ? 0.4F : vanilla;
	}

	@ModifyConstant(method = "<init>", constant = @Constant(floatValue = 0.4F))
	private float retrotweaks$tallerChicken(float vanilla) {
		return Config.MOBS.expandChickenHitbox ? 0.7F : vanilla;
	}
}
