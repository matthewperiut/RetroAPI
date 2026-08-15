package com.periut.retrotweaks.mixin.client;

import com.periut.retrotweaks.config.Config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MinecraftApplet;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Brings back the quit button. From UniTweaks and MojangFix.
 *
 * <p>b1.7.3 hides "Quit Game" whenever it thinks it is running as a browser applet, and the launcher
 * makes it think exactly that. Clearing the applet flag is enough - the button and its handler are
 * already there.
 */
@Mixin(MinecraftApplet.class)
public class MinecraftAppletQuitMixin {

	@Shadow private Minecraft minecraft;

	@Inject(method = "init", at = @At("TAIL"), remap = false)
	private void retrotweaks$showQuitButton(CallbackInfo ci) {
		if (Config.INTERFACE.showQuitButton) this.minecraft.isApplet = false;
	}
}
