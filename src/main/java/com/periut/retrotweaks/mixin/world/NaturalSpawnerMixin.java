package com.periut.retrotweaks.mixin.world;

import com.periut.retrotweaks.config.Config;
import com.periut.retrotweaks.config.Enums;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.NaturalSpawner;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Fixes the distance check that decides whether a "nightmare" mob can reach a sleeping player.
 *
 * <p>Vanilla compares the path node's raw coordinate against the player's, forgetting to centre the
 * node in its block, so the check is off by half a block on both axes. From UniTweaks.
 */
@Mixin(NaturalSpawner.class)
public class NaturalSpawnerMixin {

	/**
	 * Stops the ambush that spawns around a sleeping player. From GameplayEssentials (Disable
	 * Nightmares) and UniTweaks (disable sleep spawning). Sleeping still works and morning still
	 * comes; only the spawn attempt is skipped.
	 */
	@Inject(method = "spawnMonstersAndWakePlayers", at = @At("HEAD"), cancellable = true)
	private static void retrotweaks$noSleepSpawning(World world, List<PlayerEntity> players, CallbackInfoReturnable<Boolean> cir) {
		if (Config.GAMEPLAY.disableSleepSpawning
			|| Config.GAMEPLAY.bedBehavior == Enums.BedBehavior.DISABLE_NIGHTMARES) {
			cir.setReturnValue(false);
		}
	}

	@WrapOperation(method = "spawnMonstersAndWakePlayers", at = @At(value = "INVOKE", target = "Ljava/lang/Math;abs(D)D", ordinal = 0))
	private static double retrotweaks$centreNodeX(double value, Operation<Double> original,
			@Local(ordinal = 0) PathNode node, @Local(ordinal = 0) PlayerEntity player) {
		if (!Config.BUGFIXES.nightmarePathfindingFix) return original.call(value);
		return Math.abs(node.x + 0.5 - player.x);
	}

	@WrapOperation(method = "spawnMonstersAndWakePlayers", at = @At(value = "INVOKE", target = "Ljava/lang/Math;abs(D)D", ordinal = 1))
	private static double retrotweaks$centreNodeZ(double value, Operation<Double> original,
			@Local(ordinal = 0) PathNode node, @Local(ordinal = 0) PlayerEntity player) {
		if (!Config.BUGFIXES.nightmarePathfindingFix) return original.call(value);
		return Math.abs(node.z + 0.5 - player.z);
	}
}
