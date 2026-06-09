package com.periut.retroapi.component;

import net.minecraft.item.ItemStack;

/**
 * Implemented by an Item whose RENDERED TEXTURE depends on the stack's components, so the
 * item's look changes live as its data changes, the beta port of modern's component-driven
 * item models (dyed/tinted/layered appearance).
 *
 * <p>How it differs from modern, and why: modern Minecraft composites and tints item
 * layers at RENDER time from the components. Beta draws items as a single flat sprite out
 * of one shared atlas, with no per-layer concept at draw time. The equivalent that fits
 * beta is to bake the layered/tinted variants you want as ordinary item textures (each its
 * own atlas slot), then return the slot that matches the current components here. The
 * result is the same: the item's appearance tracks its data. RetroAPI calls this from
 * {@code ItemStack.getTextureId()}, so the chosen sprite shows everywhere, the hotbar, the
 * inventory, the hand, and dropped on the ground.</p>
 *
 * <pre>{@code
 * public class MoodGemItem extends Item implements RetroDynamicTexture {
 *     public int getDynamicTextureId(ItemStack stack) {
 *         // GEM_SPRITES[i] are pre-registered, differently tinted gem textures.
 *         return GEM_SPRITES[RetroComponents.get(stack, MOOD)];
 *     }
 * }
 * }</pre>
 */
public interface RetroDynamicTexture {

	/** The atlas sprite id to draw for this stack, from its components. */
	int getDynamicTextureId(ItemStack stack);
}
