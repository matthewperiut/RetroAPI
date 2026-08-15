package com.periut.retrotweaks.mixin.client;

import com.periut.retrotweaks.config.Config;

import com.llamalad7.mixinextras.injector.WrapWithCondition;

import net.minecraft.client.Minecraft;
import net.minecraft.client.render.GameRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Suppresses the vanilla auto-pause when the window loses focus. From UniTweaks.
 *
 * <p>{@code GameRenderer.onFrameUpdate} calls {@code Minecraft.pauseGame()} once the display has
 * been inactive for more than 500ms. Wrapping that call lets {@code pauseOnLostFocus} turn it off.
 */
@Mixin(GameRenderer.class)
public class GameRendererPauseOnLostFocusMixin {

	@WrapWithCondition(method = "onFrameUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;pauseGame()V"))
	private boolean retrotweaks$preventPauseOnLostFocus(Minecraft instance) {
		return Config.SYSTEM.pauseOnLostFocus;
	}
}
