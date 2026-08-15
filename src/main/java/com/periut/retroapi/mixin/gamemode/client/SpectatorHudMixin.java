package com.periut.retroapi.mixin.gamemode.client;

import com.periut.retroapi.gamemode.RetroGameMode;
import com.periut.retroapi.gamemode.RetroGameModes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A spectator has no hotbar and no crosshair, as in modern.
 *
 * <p>Beta already answers the health / armour / air question through
 * {@code InteractionManager.canBeRendered()} (see {@code StatusBarsMixin}), but the hotbar and the
 * crosshair are drawn before that check and unconditionally. They are the first three
 * {@code drawTexture} calls in {@code render}: the hotbar bar, the selected-slot highlight, and the
 * crosshair. Ordinals are safe here in a way they would not be on a moving target - this is a 2011
 * jar that will not gain a fourth draw.
 *
 * <p>Creative deliberately keeps all of it: modern only takes the hotbar away in spectator.
 */
@Mixin(InGameHud.class)
public class SpectatorHudMixin {
	@Shadow private Minecraft minecraft;

	@Redirect(method = "render", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(IIIIII)V", ordinal = 0))
	private void retroapi$hideHotbar(InGameHud hud, int x, int y, int u, int v, int width, int height) {
		if (!retroapi$isSpectator()) {
			hud.drawTexture(x, y, u, v, width, height);
		}
	}

	@Redirect(method = "render", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(IIIIII)V", ordinal = 1))
	private void retroapi$hideSelectedSlot(InGameHud hud, int x, int y, int u, int v, int width, int height) {
		if (!retroapi$isSpectator()) {
			hud.drawTexture(x, y, u, v, width, height);
		}
	}

	@Redirect(method = "render", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(IIIIII)V", ordinal = 2))
	private void retroapi$hideCrosshair(InGameHud hud, int x, int y, int u, int v, int width, int height) {
		if (!retroapi$isSpectator()) {
			hud.drawTexture(x, y, u, v, width, height);
		}
	}

	/**
	 * The items IN the hotbar, which are not part of those three draws: vanilla loops the nine slots
	 * and renders each through this method, so hiding the bar left the stacks floating over the world.
	 */
	@Inject(method = "renderHotbarItem", at = @At("HEAD"), cancellable = true)
	private void retroapi$hideHotbarItems(int slot, int x, int y, float tickDelta, CallbackInfo ci) {
		if (retroapi$isSpectator()) {
			ci.cancel();
		}
	}

	private boolean retroapi$isSpectator() {
		return minecraft != null && minecraft.player != null
			&& RetroGameModes.get(minecraft.player.name) == RetroGameMode.SPECTATOR;
	}
}
