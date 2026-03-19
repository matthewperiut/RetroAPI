package com.periut.retroapi.register.rendertype;

import net.ornithemc.osl.core.api.util.NamespacedIdentifier;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registry for block render types using flattened identifiers.
 * <p>
 * Vanilla render types are pre-registered with their fixed numeric IDs.
 * Custom render types are assigned IDs starting at 18.
 * <p>
 * Usage:
 * <pre>
 * // Use a vanilla render type
 * block.setRenderType(RenderTypes.STAIRS); // resolves to 10
 *
 * // Register and use a custom render type
 * NamespacedIdentifier SLOPE = RenderType.register(
 *     NamespacedIdentifiers.from("mymod", "slope"),
 *     ctx -> {
 *         // custom rendering logic using ctx helpers
 *         return true;
 *     }
 * );
 * block.setRenderType(SLOPE);
 * </pre>
 *
 * @see RenderTypes for vanilla render type constants
 */
public final class RenderType {
	private static final Map<NamespacedIdentifier, Integer> idMap = new LinkedHashMap<>();
	private static final Map<Integer, CustomBlockRenderer> renderers = new LinkedHashMap<>();
	private static int nextId = 18;

	static {
		idMap.put(RenderTypes.BLOCK, 0);
		idMap.put(RenderTypes.CROSS, 1);
		idMap.put(RenderTypes.TORCH, 2);
		idMap.put(RenderTypes.FIRE, 3);
		idMap.put(RenderTypes.LIQUID, 4);
		idMap.put(RenderTypes.REDSTONE_WIRE, 5);
		idMap.put(RenderTypes.PLANT, 6);
		idMap.put(RenderTypes.DOOR, 7);
		idMap.put(RenderTypes.LADDER, 8);
		idMap.put(RenderTypes.RAIL, 9);
		idMap.put(RenderTypes.STAIRS, 10);
		idMap.put(RenderTypes.FENCE, 11);
		idMap.put(RenderTypes.LEVER, 12);
		idMap.put(RenderTypes.CACTUS, 13);
		idMap.put(RenderTypes.BED, 14);
		idMap.put(RenderTypes.REPEATER, 15);
		idMap.put(RenderTypes.PISTON_BASE, 16);
		idMap.put(RenderTypes.PISTON_HEAD, 17);
	}

	private RenderType() {}

	/**
	 * Register a custom render type with a renderer implementation.
	 *
	 * @param id       unique identifier for the render type
	 * @param renderer the rendering implementation
	 * @return the identifier (for use with {@link RetroBlockAccess#retroapi$setRenderType})
	 */
	public static NamespacedIdentifier register(NamespacedIdentifier id, CustomBlockRenderer renderer) {
		if (idMap.containsKey(id)) {
			throw new IllegalArgumentException("Render type already registered: " + id);
		}
		int numericId = nextId++;
		idMap.put(id, numericId);
		renderers.put(numericId, renderer);
		return id;
	}

	/**
	 * Resolve a render type identifier to its numeric ID.
	 * The result can be returned from {@code Block.getRenderType()}.
	 */
	public static int resolve(NamespacedIdentifier id) {
		Integer numericId = idMap.get(id);
		if (numericId == null) {
			throw new IllegalArgumentException("Unknown render type: " + id);
		}
		return numericId;
	}

	public static CustomBlockRenderer getRenderer(int numericId) {
		return renderers.get(numericId);
	}

	public static boolean isCustom(int numericId) {
		return numericId >= 18;
	}
}
