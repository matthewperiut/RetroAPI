package com.periut.retrotweaks.mixin.client;

import com.periut.retrotweaks.feature.options.ModKeyBindings;
import com.periut.retrotweaks.feature.options.ModOptions;

import com.llamalad7.mixinextras.injector.WrapWithCondition;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerInventory;

import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * While the Zoom key is held, hotbar scroll adjusts the zoom level instead of the selected slot.
 * From UniTweaks (mixin.tweaks.fov.MinecraftMixin).
 *
 * <p>Not gated on {@code Config.GRAPHICS.video.fovOverride} - UniTweaks does not gate this one
 * either, so holding the key still swallows hotbar scroll even with the FOV override turned off.
 * Ships exactly that clumsy.
 */
@Environment(EnvType.CLIENT)
@Mixin(Minecraft.class)
public class MinecraftZoomMixin {

	@WrapWithCondition(method = "tick", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/entity/player/PlayerInventory;scrollInHotbar(I)V"))
	private boolean retrotweaks$disableScrollWhenZooming(PlayerInventory instance, int scroll) {
		if (Keyboard.isKeyDown(ModKeyBindings.ZOOM.code)) {
			ModOptions.addZoomFovOffset(-Math.min(1, Math.max(-1, scroll)));
			return false;
		}
		return true;
	}
}
