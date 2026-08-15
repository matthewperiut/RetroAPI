package com.periut.retroapi.mixin.gamemode.client;

import com.periut.retroapi.gamemode.RetroGameMode;
import com.periut.retroapi.gamemode.RetroGameModes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.item.HeldItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A spectator holds nothing, so nothing is drawn in front of the camera - modern's
 * {@code ItemInHandRenderer} leaves on the same test.
 *
 * <p>The whole first-person pass goes, hand included, because that is what "you are not there" looks
 * like: a floating arm is as wrong as a floating pickaxe. The screen overlays this class also draws
 * (fire, water) are left alone; they answer to whether the camera is in fire or water, not to what the
 * player is carrying.
 */
@Mixin(HeldItemRenderer.class)
public class SpectatorHeldItemMixin {
	@Shadow private Minecraft minecraft;

	@Inject(method = "render(F)V", at = @At("HEAD"), cancellable = true)
	private void retroapi$hideHeldItem(float tickDelta, CallbackInfo ci) {
		if (minecraft != null && minecraft.player != null
			&& RetroGameModes.get(minecraft.player.name) == RetroGameMode.SPECTATOR) {
			ci.cancel();
		}
	}
}
