package com.periut.retroapi.compat;

import com.periut.retroapi.dimension.TeleportationManager;
import net.minecraft.achievement.Achievement;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Seam between RetroAPI core and the optional StationAPI compatibility module. Core code calls these methods
 * (always behind a {@code FabricLoader.isModLoaded("stationapi")} gate) through {@link StationBridges#get()};
 * the real implementation lives in the separate {@code retroapi-stationapi} mod and is loaded reflectively, so
 * core itself carries <b>no compile-time or runtime dependency on StationAPI</b>.
 *
 * <p>Every method takes only vanilla / primitive types - no StationAPI type ever crosses this boundary - which
 * is what lets the interface live in core without pulling StationAPI onto the classpath.
 */
public interface StationBridge {

	/**
	 * @param itemFactory the block's own item form, built from the item id. A block that asked for a
	 *                    subclass of {@link net.minecraft.item.BlockItem} needs that subclass here too:
	 *                    placement rules, custom use behaviour and inventory rendering all live on it, and
	 *                    substituting a plain BlockItem silently drops every one of them.
	 */
	void registerBlock(String namespace, String path, Block block,
		java.util.function.IntFunction<net.minecraft.item.BlockItem> itemFactory);

	void registerItem(String namespace, String path, Item item);

	void registerLangPath();

	void bindAchievement(Achievement achievement, String namespace, String path);

	/** @return sprite index allocated by StationAPI's terrain atlas. */
	int addTerrainTexture(String namespace, String path);

	/** @return sprite index allocated by StationAPI's GUI-items atlas. */
	int addItemTexture(String namespace, String path);

	void setItemTexture(Item item, String namespace, String path);

	void addShapedRecipe(ItemStack output, Object... recipe);

	void addShapelessRecipe(ItemStack output, Object... ingredients);

	void addSmeltingRecipe(int inputId, ItemStack output);

	/**
	 * Mirror a RetroAPI teleport manager onto the player's StationAPI {@code TeleportationManager} slot so a
	 * RetroAPI portal teleports through StationAPI's dimension system.
	 */
	void attachPortal(PlayerEntity player, TeleportationManager retroManager);

	/**
	 * Dimension identifiers ({@code "namespace:path"}) StationAPI's own registry knows about, or an empty
	 * list when its dimension module is absent. RetroAPI's command layer merges these with its own registry
	 * so a StationAPI dimension is suggested and reachable exactly like a RetroAPI one.
	 */
	java.util.List<String> dimensionIds();

	/**
	 * Move a player into a StationAPI-registered dimension.
	 *
	 * @return false when nothing is registered under that identifier
	 */
	boolean switchDimension(PlayerEntity player, String identifier);

	/** The StationAPI identifier for a dimension serial id, or null when it does not own that id. */
	String dimensionIdentifier(int serialId);

	/** Every item identifier ({@code "namespace:path"}) in StationAPI's item registry. */
	java.util.List<String> itemIdentifiers();

	/** Numeric item id StationAPI resolves an identifier to, or {@code -1}. */
	int itemId(String identifier);

	/** The identifier StationAPI registered an item under, or null. */
	String itemIdentifier(Item item);

	/**
	 * The key code of the sprint keybind StationAPI registered for RetroAPI, or {@code -1} before it
	 * exists. StationAPI has a controls screen, so under it the sprint key is a real rebindable
	 * binding rather than RetroAPI's fixed default.
	 */
	int sprintKeyCode();
}
