package com.periut.retrotweaks.mixin.world;

import com.periut.retrotweaks.feature.options.ModOptions;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.dimension.Dimension;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The cloud height slider. From UniTweaks.
 *
 * <p>Set on the dimension rather than in the renderer because both the fast and fancy cloud paths
 * ask it the same question, and so does anything else that cares where the clouds are.
 */
@Environment(EnvType.CLIENT)
@Mixin(Dimension.class)
public class DimensionCloudHeightMixin {

	@ModifyReturnValue(method = "getCloudHeight", at = @At("RETURN"))
	private float retrotweaks$cloudHeight(float vanilla) {
		return ModOptions.enabled(ModOptions.cloudHeightOption) ? ModOptions.cloudHeightBlocks() : vanilla;
	}
}
