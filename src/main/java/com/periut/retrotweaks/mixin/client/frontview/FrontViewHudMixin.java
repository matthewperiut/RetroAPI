package com.periut.retrotweaks.mixin.client.frontview;

import com.periut.retrotweaks.feature.options.ModOptions;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.hud.InGameHud;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Hides the crosshair in front view. Ported from UniTweaks. Pointing a crosshair at your own face
 * is not useful, and vanilla draws it in third person because it never had a front view.
 */
@Environment(EnvType.CLIENT)
@Mixin(InGameHud.class)
public class FrontViewHudMixin {

	@Shadow private Minecraft minecraft;

	@WrapOperation(method = "render", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(IIIIII)V", ordinal = 2))
	private void retrotweaks$hideCrosshair(InGameHud hud, int x, int y, int u, int v, int width, int height,
			Operation<Void> original) {
		if (ModOptions.isFrontView(this.minecraft)) return;
		original.call(hud, x, y, u, v, width, height);
	}
}
