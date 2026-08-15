package com.periut.retrotweaks.mixin.client.option;

import com.periut.retrotweaks.feature.options.ModKeyBindings;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Registers RetroTweaks' dedicated keybindings into vanilla's list, so they show up in the controls
 * screen and can be rebound like any other key. From GameplayEssentials.
 *
 * <p>Which keys those are is {@link ModKeyBindings#registered()}'s call, not this mixin's: a key whose
 * feature has stood down for another mod would otherwise sit in the controls list doing nothing, next
 * to that mod's working version of it.
 */
@Environment(EnvType.CLIENT)
@Mixin(GameOptions.class)
public class KeyBindingRegistryMixin {

	@Shadow
	public KeyBinding[] allKeys;

	@Inject(method = {"<init>()V", "<init>(Lnet/minecraft/client/Minecraft;Ljava/io/File;)V"}, at = @At("RETURN"))
	private void retrotweaks$addKeyBindings(CallbackInfo ci) {
		ArrayList<KeyBinding> newKeys = new ArrayList<>(Arrays.asList(allKeys));
		newKeys.addAll(ModKeyBindings.registered());
		allKeys = newKeys.toArray(new KeyBinding[0]);
	}
}
