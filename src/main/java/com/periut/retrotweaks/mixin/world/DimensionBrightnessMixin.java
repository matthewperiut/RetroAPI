package com.periut.retrotweaks.mixin.world;

import com.periut.retrotweaks.feature.options.ModOptions;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.dimension.Dimension;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The brightness slider, which b1.7.3 has no equivalent of. From UniTweaks.
 *
 * <p>Rebuilds the light-level-to-luminance table with a flattened curve: raising the brightness
 * lifts the dark end without touching full daylight, which is what makes caves readable rather than
 * washing the whole world out. The Nether gets a higher floor because its ambient light already is
 * one.
 */
@Environment(EnvType.CLIENT)
@Mixin(Dimension.class)
public class DimensionBrightnessMixin {

	@Shadow public float[] lightLevelToLuminance;
	@Shadow public boolean isNether;

	@Inject(method = "initBrightnessTable", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$brightnessTable(CallbackInfo ci) {
		if (!ModOptions.enabled(ModOptions.brightnessOption)) return;

		float brightness = ModOptions.brightnessBoost();
		float floor = this.isNether ? 0.1F + brightness * 0.15F : 0.05F;
		// The 3.0 is vanilla's falloff; scaling it down flattens the curve towards linear.
		float falloff = 3.0F * (1.0F - brightness);

		float[] table = new float[16];
		for (int level = 0; level <= 15; level++) {
			float inverse = 1.0F - level / 15.0F;
			table[level] = (1.0F - inverse) / (inverse * falloff + 1.0F) * (1.0F - floor) + floor;
		}
		this.lightLevelToLuminance = table;
		ci.cancel();
	}
}
