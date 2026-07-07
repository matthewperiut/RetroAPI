package com.periut.retroapi;

import com.periut.retroapi.register.block.event.BlockRegistrationCallback;
import com.periut.retroapi.register.item.event.ItemRegistrationCallback;
import com.periut.retroapi.entity.event.EntityRegistrationCallback;
import com.periut.retroapi.dimension.event.DimensionRegistrationCallback;
import com.periut.retroapi.biome.event.BiomeRegistrationCallback;
import com.periut.retroapi.register.recipe.RetroRecipes;
import com.periut.retroapi.register.recipe.event.RecipeRegistrationCallback;
import com.periut.retroapi.achievement.event.AchievementRegistrationCallback;
import com.periut.retroapi.compat.StationBridges;
import com.periut.retroapi.lang.LangLoader;
import com.periut.retroapi.registry.RetroRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.ornithemc.osl.entrypoints.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RetroAPI implements ModInitializer {
	public static final Logger LOGGER = LogManager.getLogger("RetroAPI");

	public static boolean isBlock(int id) {
		return id >= 0 && id < Block.BLOCKS.length && Block.BLOCKS[id] != null;
	}

	@Override
	public void init() {
		LOGGER.info("RetroAPI initializing");
		boolean hasStationAPI = FabricLoader.getInstance().isModLoaded("stationapi");

		// Make Block the entry point of vanilla's order-sensitive Block<->Item<->Stats<->CraftingRecipeManager
		// static-init cycle by forcing Block.<clinit> now (referencing Block.BLOCKS), before achievement and
		// recipe registration set the stat-init flags. Vanilla's own guards then safely defer the
		// crafted-item stat build until items + CraftingRecipeManager are all ready. The crash this avoids:
		// if recipe/achievement registration makes CraftingRecipeManager the cycle's entry point while
		// Block/Item are still uninitialized, the deferral guards are bypassed and getInstance() is read
		// mid-<clinit> (null) -> NPE. Because RetroAPI's init runs before any dependent mod's, this also
		// fixes the order for consuming mods. NOTE: do NOT add a "defensive" mixin that touches
		// CraftingRecipeManager.getInstance() here - merely calling it forces its <clinit> at the wrong
		// time and reintroduces the crash from the other direction.
		LOGGER.info("RetroAPI initializing ({} block slots)", Block.BLOCKS.length);

		if (!hasStationAPI) {
			// NOTE: the beta-accurate default vanilla mineable/needs_<tier>_tool membership is registered
			// LAZILY on first tag query (RetroTags.ensureVanillaDefaults), not here, so it can't lose a
			// race with a consumer mod that queries tags from its own init before RetroAPI's runs.

			// Load mod lang files BEFORE any registration callbacks fire: vanilla Achievement
			// translates its name/description EAGERLY in its constructor (I18n.getTranslation),
			// so any achievement constructed before the lang is in TranslationStorage freezes the
			// raw "achievement.<key>" string forever. TranslationStorage/I18n are common classes
			// (Achievements.<clinit> uses them on the dedicated server too), so this is env-safe.
			// Default-name injection for unnamed blocks/items stays in the client entrypoint
			// (it needs the registries populated and is display-only).
			LangLoader.loadModLangFiles();

			// Fire registration events so mods can register their blocks/items
			BlockRegistrationCallback.EVENT.invoker().run();
			ItemRegistrationCallback.EVENT.invoker().run();
			// Achievements register AFTER blocks/items so icons can reference registered content.
			AchievementRegistrationCallback.EVENT.invoker().run();

			// Fire recipe registration. Calling RetroRecipes lazily constructs (and registers
			// all vanilla recipes for) CraftingRecipeManager, so our recipes land after vanilla.
			// Re-sort afterwards so shaped recipes correctly precede shapeless ones.
			RecipeRegistrationCallback.EVENT.invoker().run();
			RetroRecipes.sortCraftingRecipes();

			LOGGER.info("Registered {} blocks and {} items",
				RetroRegistry.getBlocks().size(), RetroRegistry.getItems().size());
		} else {
			// When StationAPI is present, it handles ID management and textures.
			// Register our lang path so StationAPI scans lang/ directories.
			StationBridges.get().registerLangPath();
			LOGGER.info("StationAPI detected - delegating registration, ID management, and textures to StationAPI");
		}

		// Entities register into vanilla EntityRegistry (string<->class) the same way with or without
		// StationAPI; only the spawn/render backend differs (handled by entity mixins + delegation), so
		// this fires unconditionally. Modded entity ids are stored as id.toString() per the flattening contract.
		EntityRegistrationCallback.EVENT.invoker().run();
		LOGGER.info("Registered {} entities", RetroRegistry.getEntities().size());

		// Dimensions, biomes and entities register into RetroAPI's own registries identically whether or
		// not StationAPI is present (only the spawn/world backend differs, handled by mixins + delegation).
		// Firing dimensions unconditionally keeps the dimension identifier->serial-id map populated even
		// under StationAPI, so the id_map.dat "dimensions" section is written and a datafixer world
		// conversion can read it. Biomes are plain vanilla Biome objects held by a dimension's BiomeSource.
		DimensionRegistrationCallback.EVENT.invoker().run();
		BiomeRegistrationCallback.EVENT.invoker().run();
		LOGGER.info("Registered {} dimensions", com.periut.retroapi.dimension.RetroDimensionRegistry.getAll().size());
	}
}
