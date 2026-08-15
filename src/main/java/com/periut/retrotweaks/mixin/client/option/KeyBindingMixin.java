package com.periut.retrotweaks.mixin.client.option;

import com.periut.retrotweaks.feature.options.KeyBindingAccessor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.option.KeyBinding;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Captures the key code a {@link KeyBinding} was constructed with, so a "reset to default" button
 * has something to reset to. From MojangFix.
 */
@Environment(EnvType.CLIENT)
@Mixin(KeyBinding.class)
public class KeyBindingMixin implements KeyBindingAccessor {

	@Shadow
	public int code;

	@Unique
	private int retrotweaks$defaultKeyCode;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void retrotweaks$onInit(String translationKey, int code, CallbackInfo ci) {
		this.retrotweaks$defaultKeyCode = this.code;
	}

	@Override
	public int retrotweaks$getDefaultKeyCode() {
		return retrotweaks$defaultKeyCode;
	}
}
