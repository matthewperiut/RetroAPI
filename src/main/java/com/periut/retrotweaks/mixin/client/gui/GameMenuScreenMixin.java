/*
 * Ported from HudTweaks by Telvarost (GameMenuScreenMixin). Forces a HUD layout recompute whenever
 * the pause menu is (re)initialized, as an extra safety net alongside the config-save listener.
 */
package com.periut.retrotweaks.mixin.client.gui;

import com.periut.retrotweaks.feature.hud.HudLayout;

import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {

	@Inject(method = "init", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$onInit(CallbackInfo ci) {
		HudLayout.configUpdated = true;
	}
}
