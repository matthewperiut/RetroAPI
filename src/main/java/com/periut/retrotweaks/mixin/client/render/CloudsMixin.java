package com.periut.retrotweaks.mixin.client.render;

import com.periut.retrotweaks.config.Config;
import com.periut.retrotweaks.feature.options.ModOptions;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.WorldRenderer;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The clouds toggle, plus the hotbar rendering fix that shares the same source method. From
 * UniTweaks. Cloud height is handled on the dimension, next door.
 */
@Environment(EnvType.CLIENT)
@Mixin(WorldRenderer.class)
public class CloudsMixin {

	@Shadow
	private Minecraft client;

	@Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$cloudsToggle(float tickDelta, CallbackInfo ci) {
		// If clouds are disabled, cancel their rendering
		if (ModOptions.enabled(ModOptions.cloudsOption) && !ModOptions.clouds) {
			// If hotbar rendering fix is enabled, apply the proper blend function
			if (Config.BUGFIXES.hotbarRenderingFix) {
				GL11.glBlendFunc(770, 771);
			}
			ci.cancel();
		}

		// Clouds are not disabled, apply hotbar rendering fix if enabled and running on Fast graphics
		if (Config.BUGFIXES.hotbarRenderingFix && !this.client.options.fancyGraphics) {
			GL11.glBlendFunc(770, 771);
		}
	}
}
