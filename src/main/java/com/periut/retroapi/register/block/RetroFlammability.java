package com.periut.retroapi.register.block;

import com.periut.retroapi.mixin.register.FireBlockAccessor;
import net.minecraft.block.Block;

/**
 * Registers burn/spread chances for modded blocks with vanilla fire - the minimal equivalent of
 * StationAPI's {@code FireBurnableRegisterEvent}. FireBlock's chance tables are expanded to the
 * full extended block-id range by {@code FireBlockExpandMixin}; unregistered modded blocks are
 * simply non-flammable (chance 0), matching how vanilla treats stone.
 *
 * <p>For reference, vanilla values: planks 5/20, logs 5/5, leaves 30/60, wool 30/60, TNT 15/100.
 * The first number ({@code burnChance}) is how readily fire ignites/consumes the block, the
 * second ({@code spreadChance}) how readily fire spreads onto it.
 */
public final class RetroFlammability {
	private RetroFlammability() {
	}

	public static void register(Block block, int burnChance, int spreadChance) {
		register(block.id, burnChance, spreadChance);
	}

	public static void register(int blockId, int burnChance, int spreadChance) {
		((FireBlockAccessor) Block.FIRE).retroapi$registerFlammable(blockId, burnChance, spreadChance);
	}
}
