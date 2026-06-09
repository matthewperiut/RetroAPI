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

	void registerBlock(String namespace, String path, Block block);

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
}
