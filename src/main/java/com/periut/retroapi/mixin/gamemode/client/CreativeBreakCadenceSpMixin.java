package com.periut.retroapi.mixin.gamemode.client;

import com.periut.retroapi.gamemode.RetroGameMode;
import com.periut.retroapi.gamemode.RetroGameModes;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.SingleplayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * One break at a time while the button is held in creative - the singleplayer half.
 *
 * <p>Beta breaks a held-down block through <b>two</b> paths, and a creative player, who breaks
 * everything in one hit, gets hit by both. {@code Minecraft.tick} re-fires {@code handleMouseClick}
 * every {@code tps / 4} ticks - five - which goes to {@code attackBlock} and destroys the block
 * outright. Separately, {@code handleMouseDown} calls {@code processBlockBreakingAction} EVERY tick,
 * and for a one-hit block that path also ends at {@code attackBlock}. Two breakers running at
 * different rates is why the rhythm came out fast and uneven however the second one was paced.
 *
 * <p>So the second one stands down: in creative this method does nothing, and the five-tick click
 * repeat beta already has becomes the whole cadence - within a tick of modern's own
 * {@code destroyDelay = 5} between creative breaks. Survival is untouched, and the breaking particles
 * still come, because {@code handleMouseDown} spawns those itself rather than through this call.
 *
 * <p>Split from its multiplayer twin because each manager declares the method separately.
 */
@Mixin(SingleplayerInteractionManager.class)
public class CreativeBreakCadenceSpMixin {

	@Inject(method = "processBlockBreakingAction", at = @At("HEAD"), cancellable = true)
	private void retroapi$creativeCadence(int x, int y, int z, int side, CallbackInfo ci) {
		final PlayerEntity player = retroapi$player();
		if (player != null && RetroGameModes.get(player) == RetroGameMode.CREATIVE) {
			ci.cancel();
		}
	}

	private static PlayerEntity retroapi$player() {
		final Object game = FabricLoader.getInstance().getGameInstance();
		return game instanceof Minecraft minecraft ? minecraft.player : null;
	}
}
