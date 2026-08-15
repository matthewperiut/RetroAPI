package com.periut.retroapi.mixin.gamemode.client;

import com.periut.retroapi.gamemode.RetroGameModes;
import net.minecraft.client.InteractionManager;
import net.minecraft.client.Minecraft;
import net.fabricmc.loader.api.FabricLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * No hearts, armour or air bubbles in creative or spectator.
 *
 * <p>Beta already asks one question before drawing the whole status-bar block -
 * {@code InteractionManager.canBeRendered()} - which is the same hook modern uses for exactly this
 * ({@code hasStatusBars}). Answering it is therefore the entire fix: one branch, no HUD surgery, and
 * anything else that respects the same question comes along for free.
 */
@Mixin(InteractionManager.class)
public class StatusBarsMixin {

	@Inject(method = "canBeRendered", at = @At("HEAD"), cancellable = true)
	private void retroapi$hideStatusBarsInCreative(CallbackInfoReturnable<Boolean> cir) {
		Object game = FabricLoader.getInstance().getGameInstance();
		if (!(game instanceof Minecraft minecraft) || minecraft.player == null) {
			return;
		}
		if (RetroGameModes.get(minecraft.player.name).isInvulnerable()) {
			cir.setReturnValue(false);
		}
	}
}
