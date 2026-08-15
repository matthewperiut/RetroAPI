package com.periut.retrotweaks.mixin.client.gui;

import com.periut.retrotweaks.feature.scoring.achievement.ScoreAchievements;

import net.minecraft.client.gui.screen.AchievementsScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Refreshes the WAYS achievement pages' live descriptions (and grants) every time the achievements
 * screen opens or resizes - the same moment WhatAreYouScoring refreshed them from, just reached
 * through {@code init()} rather than one specific button on the pause menu, so it works no matter
 * how the player got here (a keybind, a different menu layout, ...).
 *
 * <p>A separate mixin from {@link AchievementsScreenMixin} on purpose - that one owns the "Done"
 * button behaviour and this owns none of that, so neither has to know the other exists.
 */
@Mixin(AchievementsScreen.class)
public abstract class AchievementScoreRefreshMixin {

	@Inject(method = "init", at = @At("TAIL"))
	private void retrotweaks$refreshScoreAchievements(CallbackInfo ci) {
		ScoreAchievements.refresh();
	}
}
