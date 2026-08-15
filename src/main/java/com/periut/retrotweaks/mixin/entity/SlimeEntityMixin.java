package com.periut.retrotweaks.mixin.entity;

import com.periut.retrotweaks.config.Config;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Slimes split even when the killing blow took them below zero health.
 *
 * <p>Vanilla's split code runs {@code while (health > 0)}, so overkill damage silently skips it.
 * Clamping the health back to zero first is the smallest change that makes the existing split code
 * run. From UniTweaks by way of GameplayEssentials.
 */
@Mixin(SlimeEntity.class)
public abstract class SlimeEntityMixin extends LivingEntity {

	protected SlimeEntityMixin(World world) {
		super(world);
	}

	@Inject(method = "markDead", at = @At("HEAD"))
	private void retrotweaks$splitOnOverkill(CallbackInfo ci) {
		if (!Config.BUGFIXES.slimeSplitFix) return;
		if (this.health < 0) this.health = 0;
	}
}
