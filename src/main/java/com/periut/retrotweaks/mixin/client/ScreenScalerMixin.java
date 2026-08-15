package com.periut.retrotweaks.mixin.client;

import com.periut.retrotweaks.feature.options.ModOptions;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.util.ScreenScaler;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The GUI scale slider, replacing vanilla's four fixed sizes. From UniTweaks.
 *
 * <p>Hooked on the read of {@code options.guiScale} rather than on the local it lands in. The local
 * is not stable: StationAPI rewrites this constructor, so a {@code @ModifyVariable} keyed on a local
 * ordinal finds nothing and brings the game down. A field read is the same instruction whoever else
 * has been here.
 *
 * <p>{@code require = 1}: verified live against b1.7.3's {@code ScreenScaler.<init>} (single
 * {@code GETFIELD} of {@code GameOptions.guiScale}, no other mod rewrites this constructor), so a
 * miss here means the target broke and should fail loudly rather than leave the slider silently
 * inert - see finding #50.
 */
@Environment(EnvType.CLIENT)
@Mixin(ScreenScaler.class)
public class ScreenScalerMixin {

	/**
	 * The framebuffer size is only handed to this constructor, and it is rebuilt every frame, so this
	 * is the cheapest place to keep the slider's ceiling current - including across a window resize,
	 * which fires no event of its own here.
	 *
	 * <p>{@code static} is not a style choice: an {@code @At("HEAD")} inject into a constructor runs
	 * before {@code super()}, where {@code this} is not yet usable, and Mixin rejects a non-static
	 * handler there. It needs no instance state anyway.
	 */
	@Inject(method = "<init>", at = @At("HEAD"))
	private static void retrotweaks$trackScaleCeiling(GameOptions options, int framebufferWidth, int framebufferHeight, CallbackInfo ci) {
		ModOptions.updateGuiScaleCeiling(framebufferWidth, framebufferHeight);
	}

	@ModifyExpressionValue(method = "<init>",
		at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;guiScale:I"), require = 1)
	private int retrotweaks$guiScale(int vanilla) {
		if (!ModOptions.enabled(ModOptions.guiScaleOption)) return vanilla;
		// 0 keeps vanilla's "as large as fits" behaviour, which the slider's leftmost step means too.
		return ModOptions.guiScaleSteps();
	}
}
