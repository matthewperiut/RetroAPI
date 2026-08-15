package com.periut.retroapi.client.render;

import com.periut.retroapi.register.block.RetroBlockAccess;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

/**
 * The size a dropped item is drawn at, answered once for every renderer that has to ask.
 *
 * <p>Two of them do - beta's own {@code ItemRenderer} and, under StationAPI, arsenic's replacement -
 * and they must not disagree, so the rule lives here rather than in each mixin. See
 * {@code DroppedItemScaleMixin} for why the two features that want this constant became one answer.
 */
@Environment(EnvType.CLIENT)
public final class DroppedItemScale {

	private DroppedItemScale() {
	}

	/** What every version after b1.7.3 draws a dropped item at, and what the tweak restores. */
	private static final float MODERN = 0.25F;

	/**
	 * @param stack   the dropped stack, which may be null
	 * @param vanilla the constant beta was about to use
	 * @return the block's own declared scale, else the modern size when the tweak is on, else
	 *         {@code vanilla} untouched
	 */
	public static float of(final ItemStack stack, final float vanilla) {
		final float override = override(stack);
		return override > 0.0F ? override : vanilla;
	}

	/**
	 * The size something other than beta wants, or {@code -1} when nothing does - for a caller that
	 * has no single "vanilla" number to fall back to, only a call to leave alone.
	 */
	public static float override(final ItemStack stack) {
		final float declared = declaredScale(stack);
		if (declared > 0.0F) {
			return declared;
		}
		return com.periut.retrotweaks.config.Config.BUGFIXES.droppedItemSizeFix ? MODERN : -1.0F;
	}

	/** What {@link RetroBlockAccess#droppedItemScale(float)} was told, or {@code -1} for anything else. */
	private static float declaredScale(final ItemStack stack) {
		if (stack == null) {
			return -1.0F;
		}
		final int id = stack.itemId;
		if (id <= 0 || id >= Block.BLOCKS.length) {
			return -1.0F;
		}
		final Block block = Block.BLOCKS[id];
		return block == null ? -1.0F : ((RetroBlockAccess) block).getDroppedItemScale();
	}
}
