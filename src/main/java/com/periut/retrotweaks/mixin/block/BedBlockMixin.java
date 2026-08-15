package com.periut.retrotweaks.mixin.block;

import com.periut.retrotweaks.config.Config;
import com.periut.retrotweaks.config.Enums;
import com.periut.retrotweaks.feature.scoring.Score;
import com.periut.retrotweaks.mixin.entity.PlayerSpawnPosAccessor;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.block.BedBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.SleepAttemptResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * What a bed does when you use it. From GameplayEssentials and UniTweaks.
 *
 * <ul>
 *   <li><b>Vanilla</b> - sleep as normal.</li>
 *   <li><b>Disable Nightmares</b> - sleep, but nothing spawns around you (handled in
 *       {@code NaturalSpawnerMixin}).</li>
 *   <li><b>Set Spawn Point Only</b> - the bed becomes a respawn anchor and the night does not
 *       pass. For servers and for anyone who wants nights to stay dangerous.</li>
 *   <li><b>Disable Entirely</b> - the bed does nothing at all.</li>
 * </ul>
 */
@Mixin(BedBlock.class)
public class BedBlockMixin {

	@Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$bedDisabled(World world, int x, int y, int z, PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
		if (Config.GAMEPLAY.bedBehavior == Enums.BedBehavior.DISABLE_ENTIRELY) cir.setReturnValue(false);
	}

	@WrapOperation(method = "onUse", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/entity/player/PlayerEntity;trySleep(III)Lnet/minecraft/entity/player/SleepAttemptResult;"))
	private SleepAttemptResult retrotweaks$setSpawnOnly(PlayerEntity player, int x, int y, int z, Operation<SleepAttemptResult> original) {
		if (Config.GAMEPLAY.bedBehavior != Enums.BedBehavior.SET_SPAWN_POINT_ONLY) {
			return original.call(player, x, y, z);
		}
		// Server side only: the client would otherwise print the message twice and set a spawn
		// point the server does not know about.
		if (player.world.isRemote) return SleepAttemptResult.OK;
		((PlayerSpawnPosAccessor) player).retrotweaks$setSpawnPos(
			new Vec3i(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z)));
		player.sendMessage("Respawn point set");
		return SleepAttemptResult.OK;
	}

	/**
	 * From WhatAreYouScoring's BedBlockMixin: mark the 404 Challenge Score "never slept" bit the
	 * moment a bed successfully puts the player to sleep. The achievement it also granted is not
	 * carried over, as it needs an achievement registry API RetroTweaks does not depend on.
	 */
	@Inject(
		method = "onUse",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/block/BedBlock;updateState(Lnet/minecraft/world/World;IIIZ)V",
			ordinal = 1
		)
	)
	private void retrotweaks$scoreOnSleep(World world, int x, int y, int z, PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
		if (Config.SCORING.challenge404.enabled) {
			if (null != player) {
				Score.Fields retrotweaks$s = Score.of(player);
				retrotweaks$s.OTHER_BITFIELD |= Score.Fields.HAS_PLAYER_SLEPT;
				retrotweaks$s.Current404Score = Score.calculate404ChallengeScore(retrotweaks$s, world);
			}
		}
	}
}
