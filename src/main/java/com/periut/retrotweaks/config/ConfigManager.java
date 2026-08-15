package com.periut.retrotweaks.config;

import com.periut.retroapi.config.ConfigTree;
import com.periut.retroapi.config.RetroConfig;
import com.periut.retroapi.config.RetroConfigs;
import com.periut.retrotweaks.RetroTweaks;

/**
 * The tweaks half's own config, registered through the same API any other mod uses.
 *
 * <p>Everything that used to live here - reflecting the tree, reading and writing the JSON, backing
 * up an unreadable file, standing options down for another mod - is now
 * {@link com.periut.retroapi.config.RetroConfig}, and this is one call to
 * {@link RetroConfigs#register} plus the handful of lookups the rest of the mod already asks for by
 * name. That is deliberate: the tweaks config is the API's own first consumer, so anything it needs
 * and the API cannot do is a gap every other mod would hit too.
 */
public final class ConfigManager {

	private ConfigManager() {}

	private static RetroConfig config;

	/** The registered config, or null before {@link #load()}. */
	public static RetroConfig config() {
		return config;
	}

	public static ConfigTree.Category tree() {
		if (config == null) throw new IllegalStateException("Config accessed before RetroTweaks initialised");
		return config.tree();
	}

	public static boolean isFreshInstall() {
		return config != null && config.isFreshInstall();
	}

	/**
	 * The value the PLAYER chose for a boolean, ignoring any stand-down suppression - see
	 * {@link RetroConfig#chosenBoolean}. Used by the two paths a UniTweaks stand-down was never aimed
	 * at (the StationAPI item-colour provider and the flat-sprite tints), which have nothing to
	 * double-apply with and so must keep working while the plain field reads false.
	 */
	public static boolean chosenBoolean(String dottedPath, boolean fallback) {
		return config == null ? fallback : config.chosenBoolean(dottedPath, fallback);
	}

	/**
	 * The "HUD Positions" section ({@link Config.HudPositions}), found by its stable key path rather
	 * than kept as a reference from {@link #load()} - the tree is built purely by reflection, so
	 * nothing else names its nodes directly.
	 * {@link com.periut.retrotweaks.client.gui.ConfigScreen#isHudPositionsScreen()} uses this
	 * to recognise the page (and its six element subsections) the live HUD preview belongs on.
	 */
	public static ConfigTree.Category hudPositionsNode() {
		return ConfigTree.find(tree(), "hud", "positions");
	}

	/**
	 * The top-level "Graphics" section ({@link Config.Graphics}), found the same way and for the same
	 * reason as {@link #hudPositionsNode()}.
	 */
	public static ConfigTree.Category graphicsNode() {
		return ConfigTree.find(tree(), "graphics");
	}

	/**
	 * The top-level "Scoring" section ({@link Config.Scoring}), found the same way and for the same
	 * reason as {@link #hudPositionsNode()}.
	 * {@link com.periut.retrotweaks.client.gui.ConfigScreen#isScoringScreen()} uses it to recognise
	 * the page that gets the WAYS viewer row.
	 */
	public static ConfigTree.Category scoringNode() {
		return ConfigTree.find(tree(), "scoring");
	}

	public static void load() {
		// The delegation hook the API asks for: an option UniTweaks owns is still editable here,
		// because the bridge hands the player's value on to it. See ConfigTree.install.
		ConfigTree.install(com.periut.retrotweaks.compat.UniTweaksBridge::isDelegated);

		config = RetroConfigs.register(RetroTweaks.MOD_ID, "RetroTweaks", Config.ROOT)
			// Four things used to wait for the screen to close: the JSON on disk, the recipe and
			// stack-size rebuild, the HUD layout recompute, and handing values to UniTweaks. Hanging
			// them off the save is what makes a recipe toggle take effect when it is clicked.
			.onSaved(() -> {
				com.periut.retrotweaks.feature.hud.HudLayout.configUpdated = true;
				com.periut.retrotweaks.feature.recipe.RecipeTweaks.apply();
				com.periut.retrotweaks.feature.recipe.StackSizes.apply();
				com.periut.retrotweaks.compat.UniTweaksBridge.push(ConfigManager.tree());
			});

		// Once more now that the hook is attached. Registration already saved the file - that is how a
		// config from an older version gains the options it was missing - but it did so before there was
		// anything to run afterwards, and the recipes and stack sizes have to be built from the values
		// just loaded rather than waiting for the first time somebody opens the screen.
		config.save();
	}

	public static void save() {
		if (config != null) config.save();
	}
}
