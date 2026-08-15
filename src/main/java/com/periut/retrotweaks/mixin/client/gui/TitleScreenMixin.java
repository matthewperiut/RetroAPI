package com.periut.retrotweaks.mixin.client.gui;

import com.periut.retrotweaks.client.gui.multiplayer.MultiplayerScreen;
import com.periut.retrotweaks.compat.UniTweaksCompat;
import com.periut.retrotweaks.feature.hud.HudLayout;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

	@Inject(method = "init", at = @At("TAIL"))
	private void retrotweaks$onInit(CallbackInfo ci) {
		// The first title screen is the earliest point every mod is guaranteed to have finished
		// initialising, which is what the UniTweaks config adjustment has to wait for.
		UniTweaksCompat.patchIfNeeded();
	}

	/**
	 * Sends the "Multiplayer" button to {@link MultiplayerScreen} instead of vanilla's single-field
	 * one. From MojangFix's "Enable Multiplayer Server Changes"
	 * ({@code mixin/client/multiplayer/TitleScreenMixin}, a {@code @Redirect} there; MixinExtras'
	 * {@code @WrapOperation} does the same job here, matching {@code ImprovedControlsMixin} next
	 * door). Button 2 in {@code TitleScreen.buttonClicked} is the only one that opens
	 * {@code MultiplayerScreen}, i.e. the third {@code setScreen} call in that method - ordinal 2.
	 */
	@WrapOperation(method = "buttonClicked", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screen/Screen;)V", ordinal = 2))
	private void retrotweaks$openMultiplayerScreen(Minecraft minecraft, Screen screen, Operation<Void> original) {
		original.call(minecraft, new MultiplayerScreen(this));
	}

	// Ported from HudTweaks by Telvarost (TitleScreenMixin). Extra HUD-layout-recompute safety net,
	// fired every time the title screen is (re)initialized, alongside the config-save listener.
	@Inject(method = "init", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$onInitHudLayout(CallbackInfo ci) {
		HudLayout.configUpdated = true;
	}
}
