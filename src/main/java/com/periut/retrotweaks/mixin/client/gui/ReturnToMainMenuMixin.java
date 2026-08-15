package com.periut.retrotweaks.mixin.client.gui;

import com.periut.retrotweaks.client.gui.multiplayer.MultiplayerScreen;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * "Back to title screen" (from a disconnect) and "Cancel" (while connecting) both go straight back
 * to {@link MultiplayerScreen} instead of the title screen, so backing out of a failed connection
 * lands you back on the server list rather than the main menu. From MojangFix's "Enable Multiplayer
 * Server Changes" ({@code mixin/client/multiplayer/ReturnToMainMenuMixin}, ported unchanged - the
 * name is the reference's, not vanilla behaviour it is restoring).
 */
@Mixin({DisconnectedScreen.class, ConnectScreen.class})
public class ReturnToMainMenuMixin {

	@WrapOperation(method = "buttonClicked", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screen/Screen;)V"))
	private void retrotweaks$returnToMultiplayerScreen(Minecraft minecraft, Screen screen, Operation<Void> original) {
		original.call(minecraft, new MultiplayerScreen());
	}
}
