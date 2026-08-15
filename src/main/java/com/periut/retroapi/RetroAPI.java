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
import com.periut.retroapi.entrypoint.RetroEntrypoints;
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
		LOGGER.debug("RetroAPI initializing ({} block slots)", Block.BLOCKS.length);

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

			// Creative tabs BEFORE the entrypoint: a mod's init is exactly where it would call
			// RetroItemGroups.modifyEntries(VanillaItemGroups.INGREDIENTS, ...), and a lazily-created
			// group is null at that moment, so the call did nothing at all and said nothing about it.
			com.periut.retroapi.itemgroup.VanillaItemGroups.ensureRegistered();

			// The `retroapi` entrypoint: mods register their content here, in an order RetroAPI
			// guarantees (platform ready, before the registration events, before recipes are sorted,
			// before any world assigns ids). See RetroModInitializer for why `init` is not enough.
			RetroEntrypoints.invokeCommon();

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

			// One summary line at the end of init is all an end user needs; the per-registration
			// detail is on the debug logger for when something actually needs diagnosing.
		} else {
			// When StationAPI is present, it handles ID management and textures.
			// Register our lang path so StationAPI scans lang/ directories.
			StationBridges.get().registerLangPath();
			com.periut.retroapi.itemgroup.VanillaItemGroups.ensureRegistered();
			LOGGER.info("RetroAPI: StationAPI detected, delegating registration, ID management and textures to it");

			// The `retroapi` entrypoint fires in BOTH worlds. RetroAPI's own block/item/achievement
			// registration events deliberately do not fire under StationAPI (StationAPI owns registration
			// there), which is exactly why a mod that registers from the entrypoint - rather than from
			// those events - is the one shape that behaves the same with and without StationAPI.
			RetroEntrypoints.invokeCommon();
		}

		// Entities register into vanilla EntityRegistry (string<->class) the same way with or without
		// StationAPI; only the spawn/render backend differs (handled by entity mixins + delegation), so
		// this fires unconditionally. Modded entity ids are stored as id.toString() per the flattening contract.
		EntityRegistrationCallback.EVENT.invoker().run();

		// Dimensions, biomes and entities register into RetroAPI's own registries identically whether or
		// not StationAPI is present (only the spawn/world backend differs, handled by mixins + delegation).
		// Firing dimensions unconditionally keeps the dimension identifier->serial-id map populated even
		// under StationAPI, so the id_map.dat "dimensions" section is written and a datafixer world
		// conversion can read it. Biomes are plain vanilla Biome objects held by a dimension's BiomeSource.
		DimensionRegistrationCallback.EVENT.invoker().run();
		BiomeRegistrationCallback.EVENT.invoker().run();
		// Command blocks are RetroAPI's own vanilla content: registered before commands so the tree can
		// name them, and under minecraft: because that is what they are.
		com.periut.retroapi.commandblock.CommandBlocks.register();
		// Spawn eggs are the same kind of content, and register after the entity events above so an
		// egg can be given to a mob that was only registered a moment ago.
		com.periut.retroapi.entity.spawnegg.SpawnEggs.register();
		// And one for every mob a mod registered, unless it opted out - after the entity event above,
		// so every mob is known, and after the vanilla eggs so a mod cannot be beaten to a name.
		com.periut.retroapi.entity.spawnegg.RetroSpawnEggs.registerAll();
		// A creative tab for any mod that added content and never said where to find it. Last of the
		// automatic passes, so a mod's own tab - registered from its init or its entrypoint - has
		// already been seen and this leaves it alone.
		com.periut.retroapi.itemgroup.AutoItemGroups.registerAll();

		// Commands come last: the tree a dedicated server builds here describes the entities, items and
		// dimensions registered above, so every registration event has to have fired first.
		com.periut.retroapi.commands.RetroCommands.init();

		LOGGER.info("RetroAPI ready: {} blocks, {} items, {} entities, {} dimensions",
			RetroRegistry.getBlocks().size(), RetroRegistry.getItems().size(),
			RetroRegistry.getEntities().size(),
			com.periut.retroapi.dimension.RetroDimensionRegistry.getAll().size());
	}
}
