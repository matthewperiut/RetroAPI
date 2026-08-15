package com.periut.retroapi.mixin.gamerule;

import com.periut.retroapi.gamerule.RetroGameRules;
import net.minecraft.block.FireBlock;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

/**
 * {@code doFireTick}: fire neither spreads nor burns out.
 *
 * <p>The whole scheduled tick is cancelled, which is what modern does - the flame stays exactly where
 * it was lit until something removes it.
 */
@Mixin(FireBlock.class)
public class FireTickMixin {

	@Inject(method = "onTick", at = @At("HEAD"), cancellable = true)
	private void retroapi$doFireTick(World world, int x, int y, int z, Random random, CallbackInfo ci) {
		if (!RetroGameRules.getBoolean(RetroGameRules.DO_FIRE_TICK)) {
			ci.cancel();
		}
	}
}
