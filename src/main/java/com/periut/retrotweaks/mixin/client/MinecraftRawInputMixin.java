package com.periut.retrotweaks.mixin.client;

import com.periut.retrotweaks.feature.input.RawInput;
import com.periut.retrotweaks.feature.input.RawMouse;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Mouse;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.Canvas;

/** Swaps in {@link RawMouse} once vanilla has built its own. From UniTweaks. */
@Mixin(Minecraft.class)
public abstract class MinecraftRawInputMixin {

	@Shadow public Mouse mouse;
	@Shadow public Canvas canvas;

	@Inject(method = "init", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
		target = "Lnet/minecraft/client/Minecraft;mouse:Lnet/minecraft/client/Mouse;", ordinal = 0, shift = At.Shift.AFTER), require = 0)
	private void retrotweaks$useRawMouse(CallbackInfo ci) {
		// Only replaced when a device was actually found, so a machine without JInput natives
		// keeps the cursor-based handler and behaves exactly as it did before.
		if (RawInput.start()) this.mouse = new RawMouse(this.canvas);
	}
}
