package com.periut.retrotweaks;

import com.periut.retrotweaks.compat.Mods;
import com.periut.retrotweaks.compat.UniTweaksCompat;
import com.periut.retrotweaks.config.ConfigManager;

import net.fabricmc.api.ModInitializer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * RetroTweaks entry point.
 *
 * <p>This runs on the loader's plain {@code main} entrypoint rather than an API's init hook, because
 * RetroTweaks must run on a bare Babric or Ornithe install with no other mod present. Anything that
 * needs a later moment (recipes, item stack sizes) hooks the vanilla class that owns it instead of
 * waiting on an API's lifecycle event - except content that only exists when RetroAPI supplies a
 * registry for it, which is registered from RetroAPI's own {@code retroapi} entrypoint instead (see
 * {@link com.periut.retrotweaks.compat.retroapi.RetroTweaksRetroInitializer}), because that is
 * the only hook RetroAPI itself orders relative to its own readiness.
 */
public class RetroTweaks implements ModInitializer {

	public static final String MOD_ID = "retrotweaks";
	public static final Logger LOGGER = LogManager.getLogger("RetroTweaks");

	@Override
	public void onInitialize() {
		// Second attempt (the first is in RetroTweaksMixinPlugin.onLoad) in case the mixin
		// transformer was not live that early; register() is idempotent.
		com.periut.retrotweaks.compat.MixinVeto.register();
		ConfigManager.load();
		UniTweaksCompat.onInitialize();
		// FishinFoodTweaks' four non-vanilla fish items and WhatAreYouScoring's achievement pages both
		// need RetroAPI's registries, and both are registered from
		// com.periut.retrotweaks.compat.retroapi.RetroTweaksRetroInitializer instead of from
		// here - not from this bare `main` entrypoint, which the loader runs in unspecified order
		// relative to RetroAPI's own init, but from RetroAPI's dedicated `retroapi` entrypoint (see
		// fabric.mod.json), which RetroAPI itself orders after its registries are ready and before its
		// registration events fire. That class is the only place in RetroTweaks allowed to import a
		// RetroAPI type, and it is reached only through that entrypoint key, so a bare loader with no
		// RetroAPI installed never loads it - see that class's own doc for the full reasoning.

		if (Mods.HAS_UNITWEAKS) {
			LOGGER.info("UniTweaks detected - the features RetroTweaks shares with it are disabled.");
		}
		if (Mods.HAS_RETROAUTH) {
			LOGGER.info("RetroAuth detected - RetroTweaks' session and skin features are disabled.");
		}
	}
}
