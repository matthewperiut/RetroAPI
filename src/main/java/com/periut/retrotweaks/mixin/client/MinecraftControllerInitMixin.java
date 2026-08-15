package com.periut.retrotweaks.mixin.client;

import com.periut.retrotweaks.config.Config;

import com.llamalad7.mixinextras.injector.WrapWithCondition;

import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Skips LWJGL's controller setup. From UniTweaks.
 *
 * <p>b1.7.3 initialises JInput on startup and does nothing with the result. When the native library
 * is missing - which it is on most modern Linux setups - it throws an UnsatisfiedLinkError and
 * prints a stack trace on every launch.
 */
@Mixin(Minecraft.class)
public class MinecraftControllerInitMixin {

	@WrapWithCondition(method = "init", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Controllers;create()V"), require = 0)
	private boolean retrotweaks$skipControllerInit() {
		return !Config.SYSTEM.disableControllerInit;
	}
}
