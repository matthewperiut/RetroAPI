package com.periut.retrotweaks.mixin.client.hud;

import com.periut.retrotweaks.config.Config;

import net.minecraft.client.gui.hud.InGameHud;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * The version string in the corner. From UniTweaks.
 *
 * <p>Low priority so a mod that wants to own the whole line still can. Defaults to off, unlike
 * UniTweaks - see the README.
 */
@Mixin(value = InGameHud.class, priority = 900)
public class VersionTextMixin {

	@ModifyConstant(method = "render", constant = @Constant(stringValue = "Minecraft Beta 1.7.3 ("))
	private String retrotweaks$versionText(String vanilla) {
		if (!Config.INTERFACE.versionText.enableCustomVersionText) return vanilla;
		return Config.INTERFACE.versionText.customVersionText + " (";
	}
}
