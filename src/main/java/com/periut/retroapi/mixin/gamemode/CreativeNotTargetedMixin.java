package com.periut.retroapi.mixin.gamemode;

import com.periut.retroapi.gamemode.RetroGameMode;
import com.periut.retroapi.gamemode.RetroGameModes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mobs lose interest in a creative or spectating player, as they do in modern.
 *
 * <p>Deliberately at the START of the AI tick, looking at the target a mob already holds, rather than
 * at the many places a target is chosen. Every mob picks its own way - a monster asks
 * {@code getTargetInRange}, a ghast assigns the field straight from a player lookup, a wolf remembers
 * who hit it, a mod does whatever it likes - and all of them then run this one method with the answer
 * in the field. Catching it here needs to know none of that, so a modded mob is covered without having
 * followed any particular pattern.
 *
 * <p>Retaliation is untouched, which is also modern's behaviour: hit a mob in creative and it will come
 * for you, because the hit is what set the target and this only clears a player it is not entitled to
 * be chasing. The target is re-acquired the moment the player leaves creative.
 */
@Mixin(MobEntity.class)
public class CreativeNotTargetedMixin {

	@Shadow protected Entity target;

	@Inject(method = "tickLiving", at = @At("HEAD"))
	private void retroapi$forgetCreativePlayers(CallbackInfo ci) {
		if (!(target instanceof PlayerEntity player)) {
			return;
		}

		final RetroGameMode mode = RetroGameModes.get(player);
		if (mode == RetroGameMode.CREATIVE || mode == RetroGameMode.SPECTATOR) {
			target = null;
		}
	}
}
