package com.periut.retroapi.register.block;

import com.periut.retroapi.state.RetroBlockState;
import net.minecraft.world.BlockView;

/**
 * One overlay pass of a layered block ({@code RetroBlockAccess.overlay(...)}): a sprite plus an optional
 * tint, drawn over the block's own faces.
 *
 * <p>This is the grass-side-overlay technique generalised. Vanilla draws grass' green edge as a second
 * pass over the same cube; RetroAPI lets any block declare as many such passes as it likes, from code,
 * with no model JSON and no custom renderer. Because each pass carries its own tint, a "letter on a
 * coloured backdrop" block is one base sprite plus one glyph sprite - not one baked sprite per
 * (colour, letter) combination.
 */
public final class RetroBlockLayer {

	/** Tint value meaning "draw at full brightness, no colour multiply". */
	public static final int NO_TINT = 0xFFFFFF;

	/**
	 * Chooses a layer per position, for overlays that depend on the block's state or its neighbours.
	 * Return null to draw nothing there. The world is null when the block is drawn as an inventory item.
	 */
	@FunctionalInterface
	public interface Provider {
		RetroBlockLayer layerFor(RetroBlockState state, BlockView world, int x, int y, int z);
	}

	private final RetroTexture texture;
	private final int spriteId;
	private final int tint;

	private RetroBlockLayer(RetroTexture texture, int spriteId, int tint) {
		this.texture = texture;
		this.spriteId = spriteId;
		this.tint = tint;
	}

	/** A layer drawing a registered RetroAPI texture, untinted. */
	public static RetroBlockLayer of(RetroTexture texture) {
		return new RetroBlockLayer(texture, -1, NO_TINT);
	}

	/** A layer drawing a registered RetroAPI texture, multiplied by {@code 0xRRGGBB}. */
	public static RetroBlockLayer of(RetroTexture texture, int tint) {
		return new RetroBlockLayer(texture, -1, tint);
	}

	/** A layer drawing a raw atlas sprite index (e.g. a vanilla terrain slot), optionally tinted. */
	public static RetroBlockLayer ofSprite(int spriteId, int tint) {
		return new RetroBlockLayer(null, spriteId, tint);
	}

	/** The atlas sprite to draw, resolved at render time so atlas rebuilds are picked up. */
	public int spriteId() {
		return texture != null ? texture.id : spriteId;
	}

	/** The {@code 0xRRGGBB} multiply for this pass, or {@link #NO_TINT}. */
	public int tint() {
		return tint;
	}

	/** The texture handle this layer was declared with, or null for a raw sprite index. */
	public RetroTexture texture() {
		return texture;
	}
}
