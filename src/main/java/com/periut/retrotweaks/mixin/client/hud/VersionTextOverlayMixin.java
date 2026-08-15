package com.periut.retrotweaks.mixin.client.hud;

import com.periut.retrotweaks.config.Config;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.toast.AchievementToast;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.option.VideoOptionsScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.Color;

/**
 * Draws the version string in the corner while playing, and applies the custom text to the
 * unlicensed-copy notice. Ported from UniTweaks.
 *
 * <p>Hung off the achievement toast because that is the one HUD element vanilla already draws over
 * everything, including while a menu is open - which is exactly where this text needs to appear.
 * An odd host, but it is UniTweaks' host, and the alternative is a second overlay hook that has to
 * reproduce the same "on top of the pause menu too" behaviour.
 *
 * <p>Skipped while the debug screen is up, since that draws the version itself.
 */
@Environment(EnvType.CLIENT)
@Mixin(AchievementToast.class)
public abstract class VersionTextOverlayMixin extends DrawContext {

	@Shadow private Minecraft client;

	@Unique
	private String retrotweaks$versionText() {
		return Config.INTERFACE.versionText.enableCustomVersionText
			? Config.INTERFACE.versionText.customVersionText
			: "Minecraft Beta 1.7.3";
	}

	@ModifyConstant(method = "tick", constant = @Constant(stringValue = "Minecraft Beta 1.7.3   Unlicensed Copy :("))
	private String retrotweaks$customUnlicensedText(String vanilla) {
		return retrotweaks$versionText() + "   Unlicensed Copy :(";
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void retrotweaks$drawVersionText(CallbackInfo ci) {
		if (!Config.INTERFACE.versionText.showVersionTextIngame) return;
		if (this.client == null || this.client.options == null || this.client.options.debugHud) return;

		int colour = 0xFFFFFF;
		if (this.client.currentScreen != null) {
			// Over the pause and options screens it is drawn dark so it stays readable against
			// their lighter background. Anywhere else it is left off entirely.
			boolean menu = this.client.currentScreen instanceof GameMenuScreen
				|| this.client.currentScreen instanceof OptionsScreen
				|| this.client.currentScreen instanceof VideoOptionsScreen;
			if (!menu) return;
			colour = Color.darkGray.getRGB();
		}

		if (Config.INTERFACE.versionText.unlicensedCopy) {
			this.drawTextWithShadow(this.client.textRenderer, retrotweaks$versionText() + "   Unlicensed Copy :(", 2, 2, colour);
			this.drawTextWithShadow(this.client.textRenderer, "(Or logged in from another location)", 2, 11, colour);
			this.drawTextWithShadow(this.client.textRenderer, "Purchase at minecraft.net", 2, 20, colour);
			return;
		}
		this.drawTextWithShadow(this.client.textRenderer, retrotweaks$versionText(), 2, 2, colour);
	}
}
