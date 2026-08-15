package com.periut.retroapi.mixin.client.achievement;

import com.periut.retroapi.achievement.AchievementPage;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.AchievementsScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Every new achievements screen starts on the ordinary pages.
 *
 * <p>A viewer (see {@link AchievementPage#setViewingHidden}) is this same screen with navigation
 * pointed at the hidden pages, so something has to end that mode or the next ordinary visit stays
 * stuck in it. The constructor is the one point every route in passes exactly once - {@code removed()}
 * is inherited from {@code Screen} and not declared here, so there is nothing on this class to inject
 * into. Whoever opens a viewer therefore builds its screen FIRST and arms the mode SECOND.
 *
 * <p>Deliberately its own file, on RetroAPI's side, and in neither the StationAPI nor the UniTweaks
 * skip lists. It lived in the tweaks half's {@code AchievementsScreenMixin} first, which stands down
 * whenever UniTweaks is installed - so on exactly those installs the mode never ended and the ordinary
 * achievements screen was stuck showing the viewer's pages. A reset that must always run cannot live
 * in a file that is allowed not to.
 */
@Mixin(AchievementsScreen.class)
@Environment(EnvType.CLIENT)
public class AchievementsScreenViewerResetMixin {

	@Inject(method = "<init>", at = @At("RETURN"))
	private void retroapi$startOnOrdinaryPages(CallbackInfo ci) {
		AchievementPage.setViewingHidden(false);
		// And bring every page's visibility up to date before anything reads it. The constructor is
		// the only moment early enough for both readers: StationAPI's mirror reconciles at init HEAD
		// and RetroAPI's own page navigation draws from init TAIL, and the order two mixin configs
		// apply in is not ours to assume.
		AchievementPage.prepare();
	}
}
