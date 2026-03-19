package com.periut.retroapi.mixin.register;

import net.minecraft.stat.Stat;
import net.minecraft.stat.Stats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

@Mixin(Stats.class)
public class StatsExpandMixin {
	@Shadow
	public static Stat[] BLOCKS_MINED;

	private static final int EXPANDED_SIZE = 4096;

	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void retroapi$expandStatsArrays(CallbackInfo ci) {
		if (BLOCKS_MINED != null && BLOCKS_MINED.length < EXPANDED_SIZE) {
			BLOCKS_MINED = Arrays.copyOf(BLOCKS_MINED, EXPANDED_SIZE);
		}
	}
}
