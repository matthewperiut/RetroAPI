package com.periut.retroapi.mixin.gamemode;

import com.periut.retroapi.gamemode.RetroGameModes;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Anyone who may fly takes no fall damage, which is modern's rule rather than a special case for
 * creative.
 *
 * <p>Modern's {@code Player.causeFallDamage} returns early on {@code abilities.mayfly}, and that single
 * line is why a creative player never breaks their legs - not the game mode itself. Backporting it the
 * same way means a survival player handed flight by {@code /fly} is protected on the way down too,
 * including the landing after they switch flight off mid-air, and taking the permission away puts the
 * ground back to being dangerous immediately.
 *
 * <p>Only the damage is skipped. The distance is still counted, so the fall stats a player accrues are
 * the ones they actually fell.
 */
@Mixin(PlayerEntity.class)
public class FlightFallDamageMixin {

	@Inject(method = "onLanding", at = @At("HEAD"), cancellable = true)
	private void retroapi$noFallDamageWhileAllowedToFly(float fallDistance, CallbackInfo ci) {
		if (RetroGameModes.mayFly((PlayerEntity) (Object) this)) {
			ci.cancel();
		}
	}
}
