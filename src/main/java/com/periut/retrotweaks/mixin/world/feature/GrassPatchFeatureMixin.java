package com.periut.retrotweaks.mixin.world.feature;

import com.periut.retrotweaks.config.Config;

import net.minecraft.world.World;
import net.minecraft.world.gen.feature.GrassPatchFeature;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

/** Stops tall grass generating. From UniTweaks. Only affects newly generated chunks. */
@Mixin(GrassPatchFeature.class)
public class GrassPatchFeatureMixin {

	@Inject(method = "generate", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$skipTallGrass(World world, Random random, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
		if (Config.WORLD.flora.disableTallGrassGeneration) cir.setReturnValue(false);
	}
}
