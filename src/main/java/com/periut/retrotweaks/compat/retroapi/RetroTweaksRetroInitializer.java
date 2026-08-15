package com.periut.retrotweaks.compat.retroapi;

import com.periut.retrotweaks.feature.fishing.Fishing;
import com.periut.retrotweaks.feature.scoring.achievement.ScoreAchievements;

import com.periut.retroapi.entrypoint.RetroModInitializer;

/**
 * RetroTweaks' half of RetroAPI's {@code retroapi} entrypoint.
 *
 * <p>Reached through the {@code "retroapi"} key in {@code fabric.mod.json}. The two halves ship as one
 * mod now, so the old rule this class was written under - "the ONLY class allowed to import a RetroAPI
 * type", because RetroAPI was an optional dependency reached by reflection - no longer applies, and
 * the config, text and registry APIs are used directly wherever they fit. What this entrypoint is
 * still for is ORDER, which is the half that was never about the dependency.
 *
 * <p>Registering here instead of from the plain {@code main} entrypoint ({@link
 * com.periut.retrotweaks.RetroTweaks#onInitialize()}) is the actual fix for the bug this class
 * exists to close: the loader's {@code main} stage runs every mod in an unspecified order relative to
 * RetroAPI's own (OSL) {@code init} stage, so registering from {@code main} could - and, once
 * measured, reliably did - run after RetroAPI had already logged "RetroAPI ready" with zero content
 * and fired its registration events. {@link RetroModInitializer#initRetro()} is ordered by RetroAPI
 * itself: after its registries are ready, before its registration events fire, identically with or
 * without StationAPI.
 */
public final class RetroTweaksRetroInitializer implements RetroModInitializer {

	@Override
	public void initRetro() {
		// New content, not a reference to anything else - exactly what initRetro() itself is for, per
		// RetroModInitializer's javadoc ("registering your own blocks and items here... is right").
		// FISHINFOODTWEAKS DISABLED - re-enable by uncommenting. Without this the four extra species are never
		// registered, so Fishing.nonVanillaFishAvailable() stays false and every downstream user
		// (smelting recipes, catch rolls) short-circuits on its own.
		// Fishing.registerNonVanillaFish();
		// Achievement/stat ids must agree between client and server, so this runs on both sides even
		// though the achievements screen itself is client-only. ScoreAchievements.init() does not
		// register achievements directly - it hooks RetroAPI's AchievementRegistrationCallback (via
		// ApiBridge#onAchievementsRegister), which the doc calls out as where anything building an
		// icon from an item belongs. That hook only fires if it is installed before RetroAPI raises
		// the event, which is exactly what calling it from here, rather than from `main`, guarantees.
		ScoreAchievements.init();
		// Block 31's three metas need no registration at all any more: minecraft:dead_shrub,
		// :short_grass and :fern are entries in RetroAPI's own name table, which carries a meta
		// alongside the id. See feature.flora.TallPlantVariants.
	}
}
