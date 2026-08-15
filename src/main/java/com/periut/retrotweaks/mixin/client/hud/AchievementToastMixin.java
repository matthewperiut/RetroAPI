package com.periut.retrotweaks.mixin.client.hud;

import com.periut.retrotweaks.config.Config;

import net.minecraft.achievement.Achievement;
import net.minecraft.client.gui.hud.toast.AchievementToast;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides the achievement popup. From BetaTweaks and UniTweaks.
 *
 * <p>Only the toast is suppressed - the achievement is still earned and still shows in the
 * achievements screen.
 */
@Mixin(AchievementToast.class)
public class AchievementToastMixin {

	@Inject(method = "set", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$hideToast(Achievement achievement, CallbackInfo ci) {
		if (Config.INTERFACE.hideAchievementToast) ci.cancel();
	}
}
