package com.periut.retroapi.component;

import net.minecraft.item.ItemStack;

import java.util.List;

/**
 * Implemented by an Item that draws as a STACK OF TINTED LAYERS instead of one flat sprite,
 * the beta port of modern's component-driven, dyed/layered item models. Each render the item
 * is asked for its layers (sprite id + tint each), and RetroAPI draws them back to front
 * everywhere the item appears: hotbar, inventory, hand, and dropped on the ground.
 *
 * <p>The list IS the whole appearance, the base is just layer 0, so a stack with no special
 * components can return its one normal sprite and render exactly as usual, while a stack
 * whose components call for an override returns a different list (a recolored base, an extra
 * glow layer, whatever). Return {@code null} or an empty list to opt out entirely and fall
 * back to the ordinary single-texture path (including {@link RetroDynamicTexture}).</p>
 *
 * <p>This is drawn at RENDER time, not baked into the atlas, so the number of distinct looks
 * is unbounded: a tool with thousands of component-driven color combinations (or a smooth
 * gradient) costs nothing to store, only the few layers of the few stacks actually on screen
 * are ever drawn.</p>
 *
 * <pre>{@code
 * public List<RetroTextureLayer> getTextureLayers(ItemStack stack) {
 *     int tint = TINTS[RetroComponents.get(stack, MOOD)];
 *     return List.of(
 *         RetroTextureLayer.tinted(GEM_BASE_SPRITE, tint), // recolored base
 *         RetroTextureLayer.plain(GEM_SPARKLE_SPRITE));    // untinted overlay
 * }
 * }</pre>
 */
public interface RetroLayeredTexture {

	/** The layers to draw for this stack, back to front, or null/empty for the normal path. */
	List<RetroTextureLayer> getTextureLayers(ItemStack stack);
}
