package com.periut.retrotweaks.mixin.client.gui;

import com.periut.retrotweaks.config.Config;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.gui.screen.ingame.HandledScreen;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws container labels before the items rather than after, so item stacks stop being painted
 * underneath the "Inventory" text. From UniTweaks.
 *
 * <p>"Before" is a depth-buffer claim, not a call-order one: the early call lands after the slot loop
 * but while {@code GL_DEPTH_TEST} is still on, so the items have already written depth and the text is
 * rejected wherever one covers it.
 *
 * <p>It also lands while the item LIGHTING is still on, which vanilla always turns off before calling
 * drawForeground - so foreground text comes out shaded. Nothing is done about that here, because this
 * mixin is skipped whenever UniTweaks is installed and UniTweaks' own copy of this fix has the same
 * behaviour: a screen that cares has to be robust to being drawn lit however it was reached. See
 * {@code RetroCreativeScreen.drawForeground}.
 */
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

	@Shadow
	protected abstract void drawForeground();

	@WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawForeground()V"))
	private void retrotweaks$skipLateForeground(HandledScreen screen, Operation<Void> original) {
		if (!Config.BUGFIXES.itemstackRenderingFix) original.call(screen);
	}

	@Inject(method = "render", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
		target = "Lnet/minecraft/entity/player/ClientPlayerEntity;inventory:Lnet/minecraft/entity/player/PlayerInventory;"))
	private void retrotweaks$drawForegroundEarly(int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (Config.BUGFIXES.itemstackRenderingFix) drawForeground();
	}
}
