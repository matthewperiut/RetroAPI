package com.periut.retrotweaks.mixin.client.render;

import com.periut.retrotweaks.config.Config;
import com.periut.retrotweaks.feature.options.ModOptions;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.GameRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies the render distance, fog density and frame limit sliders. From UniTweaks.
 *
 * <p>Vanilla derives its view distance from four fixed steps ({@code 256 >> viewDistance}) and uses
 * it for both the fog and the far clip plane, so overwriting that one field after it is set moves
 * everything together and nothing else has to be touched.
 *
 * <p>"Vanilla far fog values" exists because raising the render distance also pushes the fog back,
 * and some players want the extra draw distance while keeping the beta look.
 */
@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public abstract class VideoSettingsRendererMixin {

	@Shadow @Mutable private float viewDistance;

	@Inject(method = "renderWorld", at = @At(value = "FIELD", opcode = org.objectweb.asm.Opcodes.PUTFIELD,
		target = "Lnet/minecraft/client/render/GameRenderer;viewDistance:F", shift = At.Shift.AFTER), require = 1)
	private void retrotweaks$applyRenderDistance(float tickDelta, int eye, CallbackInfo ci) {
		if (ModOptions.enabled(ModOptions.renderDistanceOption)) {
			this.viewDistance = ModOptions.renderDistanceChunks() * 16.0F;
		}
		if (ModOptions.enabled(ModOptions.fogDensityOption)) {
			// Fog rides on the same field, so it is scaled rather than set - the slider thickens or
			// thins the fog relative to whatever draw distance is in force.
			this.viewDistance *= ModOptions.fogMultiplier();
		}
		if (Config.GRAPHICS.video.vanillaFarValues) {
			// Clamp back to what beta would have used, so extra draw distance does not also mean
			// fog pushed off into the distance.
			this.viewDistance = Math.min(this.viewDistance, 256.0F);
		}
	}
}
