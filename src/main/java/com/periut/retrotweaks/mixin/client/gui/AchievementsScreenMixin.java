package com.periut.retrotweaks.mixin.client.gui;

import com.periut.retrotweaks.config.Config;

import net.minecraft.client.gui.screen.AchievementsScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * "Done" on the achievements screen goes back to the game menu rather than straight into the world.
 * From UniTweaks. Opening achievements from the menu and being dropped into the game on close is
 * the sort of thing you only notice by falling off something.
 */
@Mixin(AchievementsScreen.class)
public abstract class AchievementsScreenMixin extends Screen {

	@Inject(method = "buttonClicked", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$backToMenu(ButtonWidget button, CallbackInfo ci) {
		if (!Config.INTERFACE.achievementBackToMenu || button.id != 1) return;
		this.minecraft.setScreen(new GameMenuScreen());
		ci.cancel();
	}
}
