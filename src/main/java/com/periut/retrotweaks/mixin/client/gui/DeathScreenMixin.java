package com.periut.retrotweaks.mixin.client.gui;

import com.periut.retrotweaks.config.Config;

import net.minecraft.client.gui.screen.DeathScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * The death screen prints a literal "&amp;e" instead of colouring the score yellow, because the
 * string uses an ampersand where the game wants a section sign. From UniTweaks.
 */
@Mixin(DeathScreen.class)
public class DeathScreenMixin {

	@ModifyConstant(method = "render", constant = @Constant(stringValue = "Score: &e"))
	private String retrotweaks$fixScoreColourCode(String vanilla) {
		return Config.BUGFIXES.deathScreenFormattingFix ? vanilla.replace('&', '§') : vanilla;
	}
}
