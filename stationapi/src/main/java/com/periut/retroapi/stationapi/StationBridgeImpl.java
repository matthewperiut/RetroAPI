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
	public void registerBlock(String namespace, String path, Block block,
			java.util.function.IntFunction<BlockItem> itemFactory) {
		Identifier id = Identifier.of(namespace + ":" + path);
		Registry.register(BlockRegistry.INSTANCE, id, block);

		// Also create and register a BlockItem so ItemStack(Block) works.
		// StationAPI looks up block items via its BLOCK_ITEMS map, which gets
		// populated by BlockItemTracker when a BlockItem is registered in ItemRegistry.
		//
		// Built by the block's own factory, not `new BlockItem(...)`: a block that registered a BlockItem
		// subclass keeps its placement rules under StationAPI, which is where they matter most, since
		// StationAPI has no equivalent of them to fall back on.
		BlockItem blockItem = itemFactory.apply(block.id - 256);
		Registry.register(ItemRegistry.INSTANCE, id, blockItem);

		// Per-registration lines are debug: a modpack forwards hundreds of these at boot, and a
		// hundred identical INFO lines are not information. RetroAPI already logs one summary line
		// with the counts once registration is done.
		LOGGER.debug("Registered block {} with StationAPI", id);
	}

	@Override
	public void registerItem(String namespace, String path, Item item) {
		Identifier id = Identifier.of(namespace + ":" + path);
		Registry.register(ItemRegistry.INSTANCE, id, item);
		LOGGER.debug("Registered item {} with StationAPI", id);
	}

	@Override
	public void registerLangPath() {
		LanguageManager.addPath("lang");
		LOGGER.debug("Registered lang path with StationAPI LanguageManager");
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
		LOGGER.debug("Bound achievement {} into StationAPI StatRegistry", id);
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

	@Override
	public java.util.List<String> dimensionIds() {
		return StationDimensionBridge.ids();
	}

	@Override
	public boolean switchDimension(PlayerEntity player, String identifier) {
		return StationDimensionBridge.switchTo(player, identifier);
	}

	@Override
	public String dimensionIdentifier(int serialId) {
		return StationDimensionBridge.identifierOf(serialId);
	}

	@Override
	public java.util.List<String> itemIdentifiers() {
		java.util.List<String> ids = new java.util.ArrayList<>();
		for (Identifier id : ItemRegistry.INSTANCE.getIds()) {
			ids.add(id.namespace.toString() + ":" + id.path);
		}
		return ids;
	}

	@Override
	public int itemId(String identifier) {
		Identifier id = Identifier.tryParse(identifier);
		return id == null ? -1 : ItemRegistry.INSTANCE.getOrEmpty(id).map(item -> item.id).orElse(-1);
	}

	@Override
	public int blockId(String identifier) {
		Identifier id = Identifier.tryParse(identifier);
		return id == null ? -1 : BlockRegistry.INSTANCE.getOrEmpty(id).map(block -> block.id).orElse(-1);
	}

	@Override
	public int sprintKeyCode() {
		return SprintKeybindListener.keyCode();
	}

	@Override
	public String itemIdentifier(Item item) {
		Identifier id = ItemRegistry.INSTANCE.getId(item);
		return id == null ? null : id.namespace.toString() + ":" + id.path;
	}
}
