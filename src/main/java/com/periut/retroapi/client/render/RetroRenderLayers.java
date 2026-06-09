package com.periut.retroapi.client.render;

import net.minecraft.block.Block;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-block render layer assignment, read by the {@code Block.getRenderLayer()} mixin.
 * Code-side: {@code RetroRenderLayers.set(block, RetroRenderLayer.TRANSLUCENT)};
 * data-side: {@code "render_layer": "translucent"} in the blockstate JSON.
 *
 * <p>Environment-neutral on purpose (no client imports), so blockstate JSON parsing can
 * run in common init on a dedicated server too.</p>
 */
public final class RetroRenderLayers {

	private static final Map<Block, RetroRenderLayer> LAYERS = new HashMap<>();

	private RetroRenderLayers() {}

	public static void set(Block block, RetroRenderLayer layer) {
		LAYERS.put(block, layer);
	}

	/** The assigned layer, or null when the block uses vanilla's own getRenderLayer. */
	public static RetroRenderLayer get(Block block) {
		return LAYERS.get(block);
	}
}
