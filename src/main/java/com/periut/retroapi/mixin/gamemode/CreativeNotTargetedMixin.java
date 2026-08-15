package com.periut.retroapi.mixin.gamemode;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.periut.retroapi.gamemode.RetroGameMode;
import com.periut.retroapi.gamemode.RetroGameModes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mobs lose interest in a creative or spectating player, as they do in modern.
 *
 * <p>Two halves of the same tick, because beta's AI does both things in a row. {@code tickLiving}
 * opens by asking {@code getTargetInRange} for a target whenever it holds none, so clearing the field
 * on its own only bought a single instruction: the very next line handed the same player straight
 * back, a path was laid to them, and the mob walked over while merely declining to swing. Rejecting
 * the answer as it is returned is what actually stops the pathing.
 *
 * <p>Both halves sit on {@link MobEntity} rather than at the many places a target is chosen. Every mob
 * picks its own way - a monster asks the nearest player and checks line of sight, a spider only in the
 * dark, a wolf only when angry, a mod does whatever it likes - but all of those are overrides of the
 * one method called from the one place, and every mob then runs this same tick with the answer in the
 * field. Catching it here needs to know none of that, so a modded mob is covered without having
 * followed any particular pattern.
 *
 * <p>Slimes and ghasts are their own AI and are not covered; neither extends {@link MobEntity}.
 *
 * <p>The target is re-acquired the moment the player leaves creative, since nothing here is remembered
 * between ticks.
 */
@Mixin(MobEntity.class)
public class CreativeNotTargetedMixin {

	@Shadow protected Entity target;

	@Inject(method = "tickLiving", at = @At("HEAD"))
	private void retroapi$forgetCreativePlayers(CallbackInfo ci) {
		if (retroapi$isOffLimits(target)) {
			target = null;
		}
	}

	@ModifyExpressionValue(
		method = "tickLiving",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/MobEntity;getTargetInRange()Lnet/minecraft/entity/Entity;"))
	private Entity retroapi$dontAcquireCreativePlayers(Entity found) {
		return retroapi$isOffLimits(found) ? null : found;
	}

	@Unique
	private static boolean retroapi$isOffLimits(Entity entity) {
		if (!(entity instanceof PlayerEntity player)) {
			return false;
		}

		final RetroGameMode mode = RetroGameModes.get(player);
		return mode == RetroGameMode.CREATIVE || mode == RetroGameMode.SPECTATOR;
	}
}
