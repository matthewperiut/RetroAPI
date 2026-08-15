package com.periut.retroapi.stationapi.mixin;

import com.periut.retroapi.stationapi.StationAchievementPages;

import net.minecraft.client.gui.screen.AchievementsScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Brings StationAPI's page list up to date every time the achievements screen opens.
 *
 * <p>{@code init} rather than registration time: which RetroAPI pages belong on the screen depends on
 * a setting and on whether the WAYS viewer is what opened it, and both can change between one screen
 * and the next. It is also the last moment before anything is drawn, so the list StationAPI navigates
 * and the list it filters icons against are the same one.
 *
 * <p>HEAD, so the page buttons StationAPI adds in its own {@code init} are decided from the reconciled
 * list rather than the previous screen's.
 */
@Mixin(AchievementsScreen.class)
public class AchievementsScreenPagesMixin {

	@Inject(method = "init", at = @At("HEAD"))
	private void retroapi$syncPages(CallbackInfo ci) {
		StationAchievementPages.sync();
	}
}
