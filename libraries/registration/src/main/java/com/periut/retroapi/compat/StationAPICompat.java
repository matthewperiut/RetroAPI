package com.periut.retroapi.compat;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.modificationstation.stationapi.api.client.item.StationRendererItem;
import net.modificationstation.stationapi.api.client.texture.atlas.Atlas;
import net.modificationstation.stationapi.api.client.texture.atlas.Atlases;
import net.modificationstation.stationapi.api.registry.BlockRegistry;
import net.modificationstation.stationapi.api.registry.ItemRegistry;
import net.modificationstation.stationapi.api.registry.Registry;
import net.modificationstation.stationapi.api.resource.language.LanguageManager;
import net.modificationstation.stationapi.api.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Isolates StationAPI class references so they are only loaded when StationAPI is present.
 */
public class StationAPICompat {
	private static final Logger LOGGER = LogManager.getLogger("RetroAPI/StationAPI");

	public static void registerBlock(String namespace, String path, Block block) {
		Identifier id = Identifier.of(namespace + ":" + path);
		Registry.register(BlockRegistry.INSTANCE, id, block);

		// Also create and register a BlockItem so ItemStack(Block) works.
		// StationAPI looks up block items via its BLOCK_ITEMS map, which gets
		// populated by BlockItemTracker when a BlockItem is registered in ItemRegistry.
		BlockItem blockItem = new BlockItem(block.id - 256);
		Registry.register(ItemRegistry.INSTANCE, id, blockItem);

		LOGGER.info("Registered block {} with StationAPI", id);
	}

	public static void registerItem(String namespace, String path, Item item) {
		Identifier id = Identifier.of(namespace + ":" + path);
		Registry.register(ItemRegistry.INSTANCE, id, item);
		LOGGER.info("Registered item {} with StationAPI", id);
	}

	public static void registerLangPath() {
		LanguageManager.addPath("lang");
		LOGGER.info("Registered lang path with StationAPI LanguageManager");
	}

	/**
	 * Register a block texture with StationAPI's terrain atlas.
	 * @return sprite index allocated by StationAPI
	 */
	public static int addTerrainTexture(String namespace, String path) {
		Identifier texId = Identifier.of(namespace + ":block/" + path);
		Atlas.Sprite sprite = Atlases.getTerrain().addTexture(texId);
		LOGGER.debug("Added terrain texture {} -> sprite {}", texId, sprite.index);
		return sprite.index;
	}

	/**
	 * Register an item texture with StationAPI's GUI items atlas.
	 * @return sprite index allocated by StationAPI
	 */
	public static int addItemTexture(String namespace, String path) {
		Identifier texId = Identifier.of(namespace + ":item/" + path);
		Atlas.Sprite sprite = Atlases.getGuiItems().addTexture(texId);
		LOGGER.debug("Added item texture {} -> sprite {}", texId, sprite.index);
		return sprite.index;
	}

	/**
	 * Set an item's texture using StationAPI's renderer interface.
	 * This properly registers the texture with the atlas AND sets StationAPI's internal texture ID.
	 */
	public static void setItemTexture(Item item, String namespace, String path) {
		Identifier texId = Identifier.of(namespace + ":item/" + path);
		((StationRendererItem) item).setTexture(texId);
		LOGGER.debug("Set item texture via StationRendererItem: {}", texId);
	}
}
