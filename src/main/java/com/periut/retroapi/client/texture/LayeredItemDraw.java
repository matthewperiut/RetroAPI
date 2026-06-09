package com.periut.retroapi.client.texture;

import com.periut.retroapi.component.RetroLayeredTexture;
import com.periut.retroapi.component.RetroTextureLayer;

import net.minecraft.item.ItemStack;

import java.util.List;

/**
 * Render-side state for {@link RetroLayeredTexture} items. The item-render mixins loop a
 * layered item's render once per layer; for each pass they stash the layer's sprite and
 * tint here, and the texture/color hooks ({@code ItemTextureMixin}, {@code ItemColorMixin})
 * read them back so the existing single-sprite draw paths render that one layer. Nothing is
 * cached: the layers are asked for and drawn fresh each frame, so the cost scales with how
 * many layered stacks are ON SCREEN, not with how many distinct looks exist.
 */
public final class LayeredItemDraw {

	private LayeredItemDraw() {
	}

	/** The sprite the current layer pass should draw, or -1 when not drawing a layered item. */
	public static int forcedSprite = -1;

	/** The tint the current layer pass should use ({@code 0xRRGGBB}). */
	public static int forcedTint = RetroTextureLayer.NO_TINT;

	/** Whether a layered draw pass is active (so the hooks know to override). */
	public static boolean active() {
		return forcedSprite >= 0;
	}

	/** The layers of a stack, or null if it is not a (non-empty) layered item. */
	public static List<RetroTextureLayer> layersOf(ItemStack stack) {
		if (stack != null && stack.getItem() instanceof RetroLayeredTexture) {
			List<RetroTextureLayer> layers = ((RetroLayeredTexture) stack.getItem()).getTextureLayers(stack);
			if (layers != null && !layers.isEmpty()) {
				return layers;
			}
		}
		return null;
	}

	public static void begin(RetroTextureLayer layer) {
		// resolvedSpriteId() reads a RetroTexture handle's CURRENT atlas index (or the raw id), so a custom
		// texture captured at registration still draws the resolved sprite - the same render-time resolution
		// the vanilla-sprite layers get. See RetroTextureLayer.
		forcedSprite = layer.resolvedSpriteId();
		forcedTint = layer.tint();
	}

	public static void end() {
		forcedSprite = -1;
		forcedTint = RetroTextureLayer.NO_TINT;
	}
}
