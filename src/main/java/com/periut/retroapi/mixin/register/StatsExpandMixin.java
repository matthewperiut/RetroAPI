package com.periut.retroapi.mixin.register;

import com.periut.retroapi.registry.RetroIds;
import net.minecraft.stat.Stat;
import net.minecraft.stat.Stats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

/**
 * Expands the four item/block-indexed stat arrays from 256 to {@link #EXPANDED_SIZE}
 * so modded blocks/items with ids >= 256 never index out of bounds.
 *
 * <p>New slots are left {@code null}; {@code increaseStat} is only ever called on a
 * slot a mod explicitly populated. Mods that want a real block/item stat for an
 * expanded id should go through
 * {@link com.periut.retroapi.achievement.RetroAchievements#itemStat(Stat[], int, String)}
 * which lazily creates + registers the stat on first use, so empty slots never NPE.
 */
@Mixin(Stats.class)
public class StatsExpandMixin {
	@Shadow
	public static Stat[] MINE_BLOCK;
	@Shadow
	public static Stat[] CRAFTED;
	@Shadow
	public static Stat[] USED;
	@Shadow
	public static Stat[] BROKEN;

	private static final int EXPANDED_SIZE = RetroIds.BLOCK_ID_CAPACITY;

	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void retroapi$expandStatsArrays(CallbackInfo ci) {
		MINE_BLOCK = retroapi$grow(MINE_BLOCK);
		CRAFTED = retroapi$grow(CRAFTED);
		USED = retroapi$grow(USED);
		BROKEN = retroapi$grow(BROKEN);
	}

	private static Stat[] retroapi$grow(Stat[] array) {
		if (array != null && array.length < EXPANDED_SIZE) {
			return Arrays.copyOf(array, EXPANDED_SIZE);
		}
		return array;
	}
}
