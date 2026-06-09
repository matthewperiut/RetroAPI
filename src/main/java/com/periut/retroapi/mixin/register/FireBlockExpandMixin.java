package com.periut.retroapi.mixin.register;

import com.periut.retroapi.registry.RetroIds;
import net.minecraft.block.FireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

/**
 * FireBlock keeps its own per-block-id chance tables ({@code burnChances}/{@code spreadChances})
 * hardcoded to 256 entries and indexes them with NEIGHBOR block ids - so any extended-range
 * (id >= 256) block sitting next to fire crashes with an ArrayIndexOutOfBoundsException in
 * {@code getBurnChance}/{@code isFlammable}/{@code trySpreadingFire}. Expand both tables to the
 * same range as {@link BlockArrayExpandMixin} ({@link RetroIds#BLOCK_ID_CAPACITY}); modded blocks
 * default to chance 0 (non-flammable) unless registered via
 * {@link com.periut.retroapi.register.block.RetroFlammability}.
 */
@Mixin(FireBlock.class)
public class FireBlockExpandMixin {
	private static final int EXPANDED_SIZE = RetroIds.BLOCK_ID_CAPACITY;

	@Shadow private int[] burnChances;
	@Shadow private int[] spreadChances;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void retroapi$expandChanceTables(int id, int textureId, CallbackInfo ci) {
		// FireBlock is constructed during Block.<clinit>, BEFORE BlockArrayExpandMixin's TAIL
		// hook has expanded Block.BLOCKS - so size from our own constant, not BLOCKS.length.
		// Grow-only so a coexisting mod that already expanded these is never truncated.
		if (burnChances.length < EXPANDED_SIZE) {
			burnChances = Arrays.copyOf(burnChances, EXPANDED_SIZE);
		}
		if (spreadChances.length < EXPANDED_SIZE) {
			spreadChances = Arrays.copyOf(spreadChances, EXPANDED_SIZE);
		}
	}
}
