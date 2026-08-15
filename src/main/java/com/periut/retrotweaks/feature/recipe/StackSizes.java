package com.periut.retrotweaks.feature.recipe;

import com.periut.retrotweaks.config.Config;

import net.minecraft.block.Block;
import net.minecraft.item.Item;

/**
 * Modern stack sizes for doors and signs. From UniTweaksTelsAddons.
 *
 * <p>b1.7.3 stacks all three to 1, which is a real nuisance when building. Applied by writing the
 * item's max count, which can be changed at any time - so unlike the original this needs no restart,
 * and turning it back off restores the vanilla value rather than leaving 64 behind.
 */
public final class StackSizes {

	private StackSizes() {}

	/**
	 * Set once the item table exists. Touching {@code Item} before that would force its class
	 * initialiser to run at mod-init time, which is early enough to change the order blocks and
	 * items register in.
	 */
	private static boolean ready;

	public static void markReady() {
		ready = true;
		apply();
	}

	public static void apply() {
		if (!ready) return;
		Config.StackSizes sizes = Config.RECIPES.stackSizes;
		Item.WOODEN_DOOR.setMaxCount(sizes.woodDoor ? 64 : 1);
		Item.IRON_DOOR.setMaxCount(sizes.ironDoor ? 64 : 1);
		// Signs stack to 16 in modern Minecraft, not 64.
		Item.SIGN.setMaxCount(sizes.sign ? 16 : 1);
	}
}
