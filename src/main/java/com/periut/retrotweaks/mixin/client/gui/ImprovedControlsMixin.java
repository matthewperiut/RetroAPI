package com.periut.retrotweaks.mixin.client.gui;

import com.periut.retrotweaks.client.gui.ControlsScreen;
import com.periut.retrotweaks.config.Config;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Sends the Controls button to {@link ControlsScreen} instead of vanilla's un-scrollable one.
 * From UniTweaks.
 *
 * <p>Swapping the screen at the point it is opened, rather than rewriting the vanilla screen, means
 * turning the option off gives the vanilla screen straight back with nothing left behind.
 */
@Environment(EnvType.CLIENT)
@Mixin(OptionsScreen.class)
public class ImprovedControlsMixin {

	@WrapOperation(method = "buttonClicked", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screen/Screen;)V", ordinal = 1), require = 0)
	private void retrotweaks$openImprovedControls(Minecraft minecraft, Screen screen, Operation<Void> original) {
		if (Config.INTERFACE.improvedControlsMenu && screen instanceof KeybindsScreen) {
			original.call(minecraft, new ControlsScreen((Screen) (Object) this, minecraft.options));
			return;
		}
		original.call(minecraft, screen);
	}
}
