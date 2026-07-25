package com.periut.retroapi.register.block;

/**
 * Render-side state for one overlay pass of a layered block ({@code RetroBlockAccess.overlay(...)}).
 *
 * <p>The renderer draws a layered block once per layer. For each extra pass it stashes that layer's tint
 * here and forces the sprite through the block renderer's texture override, so the ordinary block draw
 * path renders the overlay - no custom renderer, no model JSON. The block's color hook reads
 * {@link #forcedTint} back, which is why this lives in a common package: {@code Block.getColorMultiplier}
 * is a common method, and a client-only class referenced from it would be a landmine on a dedicated server.
 *
 * <p>Nothing is cached: layers are asked for and drawn fresh per chunk rebuild, so the cost scales with
 * how many layered blocks are visible, not with how many distinct looks exist.
 */
public final class RetroBlockLayerDraw {

	/** True while an overlay pass is being drawn (guards the renderer against recursing into itself). */
	public static boolean drawing;

	/** The {@code 0xRRGGBB} tint of the pass being drawn, or -1 when no pass is active. */
	public static int forcedTint = -1;

	private RetroBlockLayerDraw() {}

	/** True when a tint from an overlay pass should win over the block's own color. */
	public static boolean hasForcedTint() {
		return forcedTint >= 0;
	}

	public static void begin(RetroBlockLayer layer) {
		drawing = true;
		forcedTint = layer.tint();
	}

	public static void end() {
		drawing = false;
		forcedTint = -1;
	}
}
