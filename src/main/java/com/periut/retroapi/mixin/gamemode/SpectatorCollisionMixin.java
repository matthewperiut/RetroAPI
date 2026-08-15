package com.periut.retroapi.mixin.gamemode;

import com.periut.retroapi.gamemode.RetroGameMode;
import com.periut.retroapi.gamemode.RetroGameModes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A spectator is not there as far as everything else is concerned: nothing collides with it, nothing
 * pushes it, and it pushes nothing.
 *
 * <p>Terrain is handled by {@code noClip} in the flight mixin; this is the other half - entities.
 * Beta asks both questions on {@code Entity}, and a spectator has to answer no to each, or mobs
 * shove the invisible player around and boats can be stood on from inside a wall.
 */
@Mixin(Entity.class)
public class SpectatorCollisionMixin {

	@Inject(method = "isCollidable", at = @At("HEAD"), cancellable = true)
	private void retroapi$spectatorHasNoCollision(CallbackInfoReturnable<Boolean> cir) {
		if (retroapi$isSpectator()) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
	private void retroapi$spectatorIsNotPushed(CallbackInfoReturnable<Boolean> cir) {
		if (retroapi$isSpectator()) {
			cir.setReturnValue(false);
		}
	}

	private boolean retroapi$isSpectator() {
		return (Object) this instanceof PlayerEntity player
			&& RetroGameModes.get(player.name) == RetroGameMode.SPECTATOR;
	}
}
