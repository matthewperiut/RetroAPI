package com.periut.retrotweaks.mixin.world.feature;

import com.periut.retrotweaks.config.Config;

import net.minecraft.world.World;
import net.minecraft.world.gen.feature.DeadBushPatchFeature;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

/**
 * Stops dead bushes generating, for players who want the pre-beta-1.6 desert back. From UniTweaks.
 * Only affects newly generated chunks; existing ones keep what they have.
 */
@Mixin(DeadBushPatchFeature.class)
public class DeadBushPatchFeatureMixin {

	@Inject(method = "generate", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$skipDeadBushes(World world, Random random, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
		if (Config.WORLD.flora.disableDeadBushGeneration) cir.setReturnValue(false);
	}
}
