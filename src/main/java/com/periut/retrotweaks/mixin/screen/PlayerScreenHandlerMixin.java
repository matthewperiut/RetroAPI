package com.periut.retrotweaks.mixin.screen;

import com.periut.retrotweaks.config.Config;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cancels the vanilla drop of the 2x2 crafting grid's contents when the inventory screen closes,
 * so {@code PlayerCraftingGridMixin}'s persisted slots survive a close instead of being emptied
 * onto the ground. From BetaTweaks ({@code PlayerScreenHandlerMixin}) and UniTweaksTelsAddons
 * ({@code keepplayercraftinggrid.PlayerContainerMixin}), which ship the identical fix - one config
 * option covers both.
 *
 * <p>Extends {@code ScreenHandler} (PlayerScreenHandler's real superclass) so the injected method
 * can call {@code super.onClosed(arg)} directly - the base implementation only drops the cursor
 * stack, not the crafting grid, which is exactly the behaviour vanilla's
 * {@code PlayerScreenHandler.onClosed} override adds on top and that this cancels.
 */
@Mixin(PlayerScreenHandler.class)
public abstract class PlayerScreenHandlerMixin extends ScreenHandler {

	@Inject(method = "onClosed", at = @At("HEAD"), cancellable = true)
	public void retrotweaks$onClosed(PlayerEntity player, CallbackInfo ci) {
		if (Config.INVENTORY.craftingGridAsInventory) {
			super.onClosed(player);
			ci.cancel();
		}
	}
}
