package com.periut.retroapi.stationapi;

import com.periut.retroapi.compat.StationBridge;
import com.periut.retroapi.dimension.TeleportationManager;
import net.minecraft.achievement.Achievement;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.modificationstation.stationapi.api.client.item.StationRendererItem;
import net.modificationstation.stationapi.api.client.texture.atlas.Atlas;
import net.modificationstation.stationapi.api.client.texture.atlas.Atlases;
import net.modificationstation.stationapi.api.recipe.CraftingRegistry;
import net.modificationstation.stationapi.api.recipe.SmeltingRegistry;
import net.modificationstation.stationapi.api.registry.BlockRegistry;
import net.modificationstation.stationapi.api.registry.ItemRegistry;
import net.modificationstation.stationapi.api.registry.Registry;
import net.modificationstation.stationapi.api.resource.language.LanguageManager;
import net.modificationstation.stationapi.api.template.achievement.AchievementTemplate;
import net.modificationstation.stationapi.api.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The real {@link StationBridge}, living in the optional {@code retroapi-stationapi} mod. This is the single
 * place that holds StationAPI class references for RetroAPI's imperative registration/recipe/achievement/
 * texture/portal delegation - loaded reflectively by {@code StationBridges} only when StationAPI is present, so
 * RetroAPI core never links against StationAPI.
 */
public final class StationBridgeImpl implements StationBridge {
	private static final Logger LOGGER = LogManager.getLogger("RetroAPI/StationAPI");

	@Override
	public void registerBlock(String namespace, String path, Block block) {
		Identifier id = Identifier.of(namespace + ":" + path);
		Registry.register(BlockRegistry.INSTANCE, id, block);

		// Also create and register a BlockItem so ItemStack(Block) works.
		// StationAPI looks up block items via its BLOCK_ITEMS map, which gets
		// populated by BlockItemTracker when a BlockItem is registered in ItemRegistry.
		BlockItem blockItem = new BlockItem(block.id - 256);
		Registry.register(ItemRegistry.INSTANCE, id, blockItem);

		LOGGER.info("Registered block {} with StationAPI", id);
	}

	@Override
	public void registerItem(String namespace, String path, Item item) {
		Identifier id = Identifier.of(namespace + ":" + path);
		Registry.register(ItemRegistry.INSTANCE, id, item);
		LOGGER.info("Registered item {} with StationAPI", id);
	}

	@Override
	public void registerLangPath() {
		LanguageManager.addPath("lang");
		LOGGER.info("Registered lang path with StationAPI LanguageManager");
	}

	/**
	 * Binds an already-registered RetroAPI achievement into StationAPI's StatRegistry.
	 * StationAPI alpha.6+ requires every achievement to be bound, otherwise the registry
	 * fails to freeze with "Trying to access unbound value".
	 */
	@Override
	public void bindAchievement(Achievement achievement, String namespace, String path) {
		Identifier id = Identifier.of(namespace + ":" + path);
		AchievementTemplate.onConstructor(achievement, id);
		LOGGER.info("Bound achievement {} into StationAPI StatRegistry", id);
	}

	@Override
	public int addTerrainTexture(String namespace, String path) {
		Identifier texId = Identifier.of(namespace + ":block/" + path);
		Atlas.Sprite sprite = Atlases.getTerrain().addTexture(texId);
		LOGGER.debug("Added terrain texture {} -> sprite {}", texId, sprite.index);
		return sprite.index;
	}

	@Override
	public int addItemTexture(String namespace, String path) {
		Identifier texId = Identifier.of(namespace + ":item/" + path);
		Atlas.Sprite sprite = Atlases.getGuiItems().addTexture(texId);
		LOGGER.debug("Added item texture {} -> sprite {}", texId, sprite.index);
		return sprite.index;
	}

	@Override
	public void setItemTexture(Item item, String namespace, String path) {
		Identifier texId = Identifier.of(namespace + ":item/" + path);
		((StationRendererItem) item).setTexture(texId);
		LOGGER.debug("Set item texture via StationRendererItem: {}", texId);
	}

	@Override
	public void addShapedRecipe(ItemStack output, Object... recipe) {
		CraftingRegistry.addShapedRecipe(output, recipe);
	}

	@Override
	public void addShapelessRecipe(ItemStack output, Object... ingredients) {
		CraftingRegistry.addShapelessRecipe(output, ingredients);
	}

	@Override
	public void addSmeltingRecipe(int inputId, ItemStack output) {
		SmeltingRegistry.addSmeltingRecipe(inputId, output);
	}

	@Override
	public void attachPortal(PlayerEntity player, TeleportationManager retroManager) {
		StationPortalBridge.attach(player, retroManager);
	}
}
