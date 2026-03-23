package com.periut.retroapi.register.block;

import net.ornithemc.osl.core.api.util.NamespacedIdentifier;
import com.periut.retroapi.compat.StationAPICompat;
import net.minecraft.block.Block;
import net.minecraft.item.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Texture registration API for RetroAPI.
 * Register textures by identifier and get {@link RetroTexture} objects whose .id
 * is updated when the atlas resolves.
 * <p>
 * When StationAPI is present, textures are resolved during TextureRegisterEvent.
 * When absent, textures are composited into the vanilla atlas at the allocated slots.
 * <p>
 * Texture files: assets/{namespace}/textures/block/{path}.png
 * and assets/{namespace}/textures/item/{path}.png
 */
public class RetroTextures {
	private static final List<RetroTexture> terrainTextures = new ArrayList<>();
	private static final List<RetroTexture> itemTextures = new ArrayList<>();
	private static int nextTerrainSlot = 256;
	private static int nextItemSlot = 256;

	// Queued block/item updates for StationAPI resolution
	private static final List<BlockEntry> trackedBlocks = new ArrayList<>();
	private static final List<ItemEntry> trackedItems = new ArrayList<>();

	/**
	 * Register a block/terrain texture.
	 * The returned RetroTexture's .id is updated when the atlas resolves.
	 */
	public static RetroTexture addBlockTexture(NamespacedIdentifier id) {
		int slot = nextTerrainSlot++;
		RetroTexture tex = new RetroTexture(id, slot);
		terrainTextures.add(tex);
		return tex;
	}

	/**
	 * Register an item texture.
	 * The returned RetroTexture's .id is updated when the atlas resolves.
	 */
	public static RetroTexture addItemTexture(NamespacedIdentifier id) {
		int slot = nextItemSlot++;
		RetroTexture tex = new RetroTexture(id, slot);
		itemTextures.add(tex);
		return tex;
	}

	/**
	 * Track a block for sprite update during StationAPI resolution.
	 * After resolution, block.sprite is set to the RetroTexture's resolved id.
	 */
	public static void trackBlock(Block block, RetroTexture texture) {
		trackedBlocks.add(new BlockEntry(block, texture));
	}

	/**
	 * Track an item for texture update during StationAPI resolution.
	 * After resolution, the item's texture is set via StationAPI's setTexture.
	 */
	public static void trackItem(Item item, RetroTexture texture) {
		trackedItems.add(new ItemEntry(item, texture));
	}

	/**
	 * Called during StationAPI's TextureRegisterEvent.
	 * Registers all textures with StationAPI's atlas and updates RetroTexture.id values.
	 */
	public static void resolveStationAPITextures() {
		// Register terrain textures and update ids
		for (RetroTexture tex : terrainTextures) {
			tex.id = StationAPICompat.addTerrainTexture(tex.getIdentifier().namespace(), tex.getIdentifier().identifier());
		}
		// Update tracked block sprites
		for (BlockEntry entry : trackedBlocks) {
			entry.block.sprite = entry.texture.id;
		}

		// Register item textures and update ids
		for (RetroTexture tex : itemTextures) {
			tex.id = StationAPICompat.addItemTexture(tex.getIdentifier().namespace(), tex.getIdentifier().identifier());
		}
		// Use setTexture for tracked items (handles StationAPI renderer internals)
		for (ItemEntry entry : trackedItems) {
			StationAPICompat.setItemTexture(entry.item, entry.texture.getIdentifier().namespace(), entry.texture.getIdentifier().identifier());
		}
	}

	/** Terrain textures for AtlasExpander (without StationAPI). */
	public static List<RetroTexture> getTerrainTextures() {
		return Collections.unmodifiableList(terrainTextures);
	}

	/** Item textures for AtlasExpander (without StationAPI). */
	public static List<RetroTexture> getItemTextures() {
		return Collections.unmodifiableList(itemTextures);
	}

	private record BlockEntry(Block block, RetroTexture texture) {}
	private record ItemEntry(Item item, RetroTexture texture) {}
}
