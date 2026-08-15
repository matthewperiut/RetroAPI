package com.periut.retrotweaks.feature.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

/**
 * The colour a block drawn as a flat item sprite should be multiplied by.
 *
 * <p>Vanilla asks {@link Block#getColor(int)} in exactly one place - {@code BlockRenderManager
 * .render(Block, int, float)}, the path a full cube takes. Anything whose render type is not "side lit"
 * (crossed sprites like tall grass and ferns, render type 1) is drawn as a flat texture by a different
 * code path that never asks at all, which is why a greyscale grass sprite stays grey as an item however
 * correct {@code getColor} is.
 *
 * <p>Three separate renderers have that flaw - the inventory icon, the item in your hand and the
 * dropped entity - so the question "what colour does this stack want" lives here rather than three
 * times over. Blocks that answer white are reported as "no tint" so a caller can skip the work
 * entirely instead of multiplying by nothing.
 */
@Environment(EnvType.CLIENT)
public final class ItemTint {

	private ItemTint() {}

	/** Sentinel for "this needs no tint". Chosen because a colour is always a positive 24-bit value. */
	public static final int NONE = -1;

	public static int of(ItemStack stack) {
		return stack == null ? NONE : of(stack.itemId, stack.getDamage());
	}

	public static int of(int itemId, int meta) {
		// The player's CHOSEN value, not the effective Config field. The option is suppressed when
		// UniTweaks is installed ("UniTweaks provides this"), but UniTweaks' grassblockitemfix only
		// covers the 3D inventory block render - none of the three flat-sprite renderers asking this
		// class exist on its side, so for THESE paths there is nothing to double-apply with and the
		// suppression just turned dropped ferns and tall-grass icons grey. Same reasoning, and the
		// same accessor, as StationApiItemColors.
		if (!com.periut.retrotweaks.config.ConfigManager.chosenBoolean("bugfixes.grassBlockItemFix", true)) return NONE;
		if (itemId < 0 || itemId >= Block.BLOCKS.length) return NONE;
		Block block = Block.BLOCKS[itemId];
		if (block == null) return NONE;
		int color = block.getColor(meta);
		return color == 0xFFFFFF ? NONE : color;
	}

	public static float red(int color) {
		return (color >> 16 & 255) / 255.0F;
	}

	public static float green(int color) {
		return (color >> 8 & 255) / 255.0F;
	}

	public static float blue(int color) {
		return (color & 255) / 255.0F;
	}
}
