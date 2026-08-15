package com.periut.retroapi.mixin.movement;

import com.periut.retroapi.gamemode.RetroFlight;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * No run of footsteps on touching down after a flight.
 *
 * <p>Beta counts distance walked into {@code horizontalSpeed} and plays a step each time it passes
 * {@code nextStepSoundDistance}, which it then raises by exactly one. In the air there is no block
 * underfoot, so no step plays and the counter is never raised - while the distance keeps piling up.
 * Fly a hundred blocks and land, and the two are a hundred apart: the game then plays one step per
 * tick until it has caught up, which is the burst of footsteps.
 *
 * <p>Modern does not have this, because it re-seats its own counter from the distance rather than
 * stepping it by one. This does that, and only for a player RetroAPI is flying: the counter is pulled
 * up to the distance every tick they are airborne, so landing owes exactly one step.
 */
@Mixin(Entity.class)
public class FlightStepSoundMixin {

	@Shadow public float horizontalSpeed;
	@Shadow private int nextStepSoundDistance;

	@Inject(method = "move", at = @At("RETURN"))
	private void retroapi$keepStepCounterInSync(double dx, double dy, double dz, CallbackInfo ci) {
		if ((Object) this instanceof PlayerEntity player && RetroFlight.isFlying(player)) {
			nextStepSoundDistance = (int) horizontalSpeed + 1;
		}
	}
}
