package com.periut.retroapi.mixin.gamemode.client;

import com.periut.retroapi.gamemode.GameModeNetworking;
import com.periut.retroapi.gamemode.RetroGameMode;
import com.periut.retroapi.gamemode.RetroGameModes;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Double-tap jump toggles creative flight, as it has since 1.0.
 *
 * <p>The tap is only detectable on the client - the server sees a stream of positions, not key
 * presses - so the decision is made here and the result is sent on, which is also why flight is not
 * something a client may simply assert: the server checks the mode before believing it.
 */
@Mixin(ClientPlayerEntity.class)
public abstract class CreativeFlightToggleMixin {

	/** Modern's window: a second tap within this many ticks counts as a double-tap. */
	private static final int DOUBLE_TAP_TICKS = 7;

	@Unique private boolean retroapi$wasJumping;
	@Unique private int retroapi$ticksSinceJumpTap = DOUBLE_TAP_TICKS + 1;

	@Inject(method = "tickMovement", at = @At("HEAD"))
	private void retroapi$flightToggle(CallbackInfo ci) {
		ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
		// May-fly, not creative: /fly hands the same double-tap to a survival or adventure player.
		if (!RetroGameModes.mayFly(player.name)) {
			retroapi$wasJumping = player.input != null && player.input.jumping;
			return;
		}

		boolean jumping = player.input != null && player.input.jumping;
		if (retroapi$ticksSinceJumpTap <= DOUBLE_TAP_TICKS) {
			retroapi$ticksSinceJumpTap++;
		}

		if (jumping && !retroapi$wasJumping) {
			if (retroapi$ticksSinceJumpTap <= DOUBLE_TAP_TICKS) {
				final boolean wasFlying = RetroGameModes.isFlying(player.name);
				GameModeNetworking.requestFlightToggle();

				// Modern's kick off the ground: "abilities.flying = !abilities.flying; if (flying &&
				// onGround()) jumpFromGround();". Taking off from a standstill is why this felt limp -
				// flight began with no vertical momentum at all and the only lift was the throttle. A
				// real jump gives it the same shove out of the ground, and flight's own 0.6 vertical
				// friction bleeds it away over the next few ticks exactly as it does there.
				if (!wasFlying && player.onGround && RetroGameModes.isFlying(player.name)) {
					((com.periut.retroapi.mixin.gamemode.LivingEntityJumpInvoker) player).retroapi$jump();
				}

				retroapi$ticksSinceJumpTap = DOUBLE_TAP_TICKS + 1;
			} else {
				retroapi$ticksSinceJumpTap = 0;
			}
		}

		retroapi$wasJumping = jumping;
	}
}
