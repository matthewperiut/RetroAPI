package com.periut.retrotweaks.mixin.client;

import com.periut.retrotweaks.config.Config;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.Minecraft;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.PixelFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Asks for a 24-bit depth buffer instead of the driver default. From UniTweaks and MojangFix.
 *
 * <p>Some drivers - AMD's especially - hand out 16 bits by default, which shows up as z-fighting
 * and banding on distant terrain.
 *
 * <p>{@code require = 0} because RetroDragon replaces the display setup entirely; there the option
 * simply has nothing to patch, which is correct rather than an error.
 */
@Mixin(Minecraft.class)
public class MinecraftBitDepthMixin {

	@WrapOperation(method = "init", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/Display;create()V", remap = false), require = 0)
	private void retrotweaks$requestDepthBits(Operation<Void> original) throws LWJGLException {
		if (Config.BUGFIXES.bitDepthFix) {
			Display.create(new PixelFormat().withDepthBits(24));
			return;
		}
		original.call();
	}
}
