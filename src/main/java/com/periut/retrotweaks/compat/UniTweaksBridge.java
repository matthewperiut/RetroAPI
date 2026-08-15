package com.periut.retrotweaks.compat;

import com.periut.retrotweaks.RetroTweaks;
import com.periut.retroapi.config.ConfigTree;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Drives a co-installed UniTweaks from RetroTweaks' own config.
 *
 * <p>RetroTweaks used to simply stand down: an {@code @Opt(source = Mods.UNITWEAKS)} option was forced
 * off and greyed out, and the player had to go and find the same setting again in UniTweaks' own
 * screen. The two mods then had two settings for one feature, and the one you could reach from the
 * RetroTweaks screen was the one that did nothing. This makes the RetroTweaks option the control
 * surface instead: the value the player picks is written straight into UniTweaks' live config object,
 * and UniTweaks' own implementation - the one actually running - reads it from there.
 *
 * <p><b>Which options - all of them that pair up.</b> There is no need to restrict this to features
 * whose mixin is skipped. {@code ConfigTree.Option} already keeps the player's choice separate from the
 * effective value written into the backing field, and {@link ConfigTree#isStoodDownFor} still forces
 * that field off for everything UniTweaks owns - so RetroTweaks' own mixin reads false and cannot
 * double-apply a thing, whether or not it was loaded. The player's value goes to UniTweaks instead, and
 * is still saved to RetroTweaks' own config, so removing UniTweaks later brings it straight back into
 * force with nothing to re-enter. {@link #BINDINGS} therefore covers every option with a real
 * counterpart; each entry is verified by name and type against UniTweaks 0.28.0 and 0.29.0.
 *
 * <p>What is left out is only what cannot be expressed: the per-block axe/pickaxe effectivity toggles
 * (RetroTweaks has one switch per block, UniTweaks a single {@code blockEffectivenessFix} for all of
 * them, and folding 27 into 1 would make each of them lie about what it does) and the shift-placing
 * blacklist (ours is block ids, theirs is strings). Those keep the old greyed-out row, which is the
 * honest answer when a value genuinely has nowhere to go.
 *
 * <p><b>How it reaches UniTweaks.</b> Entirely by reflection, resolved once and cached; nothing here
 * imports a UniTweaks or GCAPI type, so a bare install with neither mod present loads this class,
 * finds nothing, and does nothing. UniTweaks' config roots are {@code public static final} fields on
 * {@code net.danygames2014.unitweaks.UniTweaks}, and the values inside them are plain {@code public}
 * fields, so no API is needed to write them.
 *
 * <p><b>Persistence.</b> GCAPI3 publishes no save entry point ({@code GCAPI} offers only
 * {@code reloadConfig} and {@code getRootConfigScreen}), so writing UniTweaks' YAML means calling
 * {@code GCCore.saveConfig(ModContainer, ConfigCategoryHandler, 32)} - the exact call GCAPI's own
 * config screen makes when you press Back, found via {@code GCCore.MOD_CONFIGS}, which is keyed by the
 * {@code @ConfigRoot} name ("general", "userinterface", ...). That is an implementation package and
 * could be restructured by a future GCAPI, so every step of it is failure-tolerant: if the handles do
 * not resolve, the values are still pushed into the live objects and simply are not written to disk,
 * which costs the player nothing but a re-push on the next launch.
 */
public final class UniTweaksBridge {

	private UniTweaksBridge() {}

	/**
	 * One RetroTweaks option wired to one UniTweaks field.
	 *
	 * @param optionPath dotted path through RetroTweaks' own config tree, e.g. {@code graphics.video.fovOverride}
	 * @param root       UniTweaks' {@code @ConfigRoot} name, which is also its key in {@code GCCore.MOD_CONFIGS}
	 * @param fieldPath  field names to walk from that root's object, last one being the value itself
	 */
	private record Binding(String optionPath, String root, String... fieldPath) {}

	/** Config root name -> the {@code UniTweaks} static field holding it. */
	private static final Map<String, String> ROOT_FIELDS = Map.of(
		"general", "GENERAL_CONFIG",
		"userinterface", "USER_INTERFACE_CONFIG",
		"gameplay", "GAMEPLAY_CONFIG",
		"features", "FEATURES_CONFIG",
		"tweaks", "TWEAKS_CONFIG",
		"oldfeatures", "OLD_FEATURES_CONFIG",
		"bugfixes", "BUGFIXES_CONFIG"
	);

	/**
	 * Every option RetroTweaks takes over. Verified twice: the RetroTweaks side appears in
	 * {@code UNITWEAKS_SUPERSEDED_MIXINS} (so our own code is not running), and the UniTweaks side is a
	 * real field of UniTweaks 0.28.0 with a type this can assign.
	 *
	 * <p>Deliberately absent, though the features pair up: "Shift Placing Blacklist" (ours is
	 * {@code Integer[]}, UniTweaks' is {@code String[]}), "Boat Collision Behavior" (ours is a
	 * three-way enum, UniTweaks' {@code boatsDontBreak} is a boolean) and "Chest Sounds" (two unrelated
	 * enum types). Each needs a real converter rather than an assignment, and guessing at the mapping
	 * would be worse than leaving them where they already work.
	 */
	private static final Binding[] BINDINGS = {
		// -- System: pause on lost focus, autosave, raw input, controller init, dimensions -----------
		new Binding("system.pauseOnLostFocus", "general", "pauseOnLostFocus"),
		new Binding("system.autosaveInterval", "general", "autosaveInterval"),
		new Binding("system.rawInput", "general", "rawInput"),
		new Binding("system.disableControllerInit", "general", "disableControllerInit"),
		new Binding("system.disabledDimensions", "general", "disabledDimensions"),

		// -- Interface -------------------------------------------------------------------------------
		new Binding("ui.showQuitButton", "userinterface", "showQuitButton"),
		new Binding("ui.improvedControlsMenu", "userinterface", "improvedControlsMenu"),
		new Binding("ui.hideAchievementToast", "userinterface", "hideAchievementToast"),
		new Binding("ui.achievementBackToMenu", "userinterface", "achievementBackToMenu"),
		new Binding("hud.debug.disableEntityIdTags", "userinterface", "disableDebugEntityIdTags"),

		// -- Version text ----------------------------------------------------------------------------
		new Binding("ui.versionText.showVersionTextIngame", "userinterface", "versionTextConfig", "showVersionTextIngame"),
		new Binding("ui.versionText.unlicensedCopy", "userinterface", "versionTextConfig", "unlicensedCopy"),
		new Binding("ui.versionText.enableCustomVersionText", "userinterface", "versionTextConfig", "enableCustomVersionText"),
		new Binding("ui.versionText.customVersionText", "userinterface", "versionTextConfig", "customVersionText"),

		// -- Video settings: the whole stack is skipped under UniTweaks, OptionMixin included ---------
		new Binding("graphics.video.fovOverride", "userinterface", "fovSlider"),
		new Binding("graphics.video.brightnessOverride", "userinterface", "videoSettingsConfig", "brightnessSlider"),
		new Binding("graphics.video.cloudHeightOverride", "userinterface", "videoSettingsConfig", "cloudHeightSlider"),
		new Binding("graphics.video.cloudsToggle", "userinterface", "videoSettingsConfig", "cloudsToggle"),
		new Binding("graphics.video.fogDensityOverride", "userinterface", "videoSettingsConfig", "fogDensitySlider"),
		new Binding("graphics.video.guiScaleOverride", "userinterface", "videoSettingsConfig", "guiScaleSlider"),
		new Binding("graphics.video.fpsLimitOverride", "userinterface", "videoSettingsConfig", "fpsLimitSlider"),
		new Binding("graphics.video.renderDistanceOverride", "userinterface", "videoSettingsConfig", "renderDistanceSlider"),
		new Binding("graphics.video.vanillaFarValues", "userinterface", "videoSettingsConfig", "vanillaFarValues"),

		// -- Gameplay ---------------------------------------------------------------------------------
		new Binding("gameplay.shiftPlacing", "gameplay", "shiftPlacing"),
		new Binding("gameplay.rightClickEquipArmor", "gameplay", "rightClickEquipArmor"),
		new Binding("gameplay.disableEatingAtMaxHealth", "gameplay", "noFoodWastage"),
		new Binding("gameplay.pickBlockFromInventory", "gameplay", "pickBlockFromInventory"),
		new Binding("gameplay.pickBlockFixes", "bugfixes", "pickBlockFix"),
		new Binding("gameplay.fenceJumping", "gameplay", "fenceJumping"),
		new Binding("gameplay.stepAssist", "features", "stepAssist"),

		// -- Old features ------------------------------------------------------------------------------
		new Binding("gameplay.oldFeatures.allowGapsInLadders", "oldfeatures", "ladderGaps"),
		new Binding("gameplay.oldFeatures.elevatorBoats", "oldfeatures", "boatElevators"),
		new Binding("gameplay.oldFeatures.minecartBoosters", "oldfeatures", "minecartBoosters"),
		new Binding("gameplay.oldFeatures.hoeGrassForSeeds", "oldfeatures", "hoeGrassForSeeds"),
		new Binding("gameplay.oldFeatures.punchTntToIgnite", "oldfeatures", "punchTntToIgnite"),
		new Binding("gameplay.oldFeatures.punchTntToDefuse", "oldfeatures", "punchTntToDefuse"),

		// -- Blocks and world --------------------------------------------------------------------------
		new Binding("blocks.sugarCaneOnSand", "tweaks", "sugarCaneOnSand"),
		new Binding("blocks.pumpkinPlacementFixes", "tweaks", "pumpkinsPlaceableLikeNormal"),
		new Binding("blocks.fencePlacementFixes", "tweaks", "fencesPlaceableLikeNormal"),
		new Binding("blocks.fencesConnectBlocks", "tweaks", "fencesConnectBlocks"),
		new Binding("blocks.bookshelvesDropBooks", "tweaks", "bookshelvesDropBooks"),
		new Binding("blocks.pressurePlatesOnFences", "tweaks", "pressurePlatesOnFences"),
		new Binding("blocks.stackableChests", "tweaks", "stackableChests"),
		new Binding("blocks.trapdoorsWithoutSupport", "tweaks", "allowTrapdoorsWithoutSupport"),
		new Binding("blocks.flintAndSteelFixes", "tweaks", "modernFlintAndSteel"),
		new Binding("mobs.betterBurning.igniteEntitiesWithFlintAndSteel", "tweaks", "allowIgnitingEntities"),
		new Binding("mobs.boatDropFixes", "tweaks", "boatsDropThemselves"),
		new Binding("gameplay.disableSleepSpawning", "tweaks", "disableSleepSpawning"),

		// -- Bugfixes: UniTweaks names these identically, one for one --------------------------------
		new Binding("bugfixes.bitDepthFix", "bugfixes", "bitDepthFix"),
		new Binding("bugfixes.boatDismountFix", "bugfixes", "boatDismountFix"),
		new Binding("bugfixes.bowHeldFix", "bugfixes", "bowHeldFix"),
		new Binding("bugfixes.breakingAnimationFix", "bugfixes", "breakingAnimationFix"),
		new Binding("bugfixes.deathScreenFormattingFix", "bugfixes", "deathScreenFormattingFix"),
		new Binding("bugfixes.droppedItemSizeFix", "bugfixes", "droppedItemSizeFix"),
		new Binding("bugfixes.farLandsJitterFix", "bugfixes", "farLandsJitterFix"),
		new Binding("bugfixes.fenceLightingFix", "bugfixes", "fenceLightingFix"),
		new Binding("bugfixes.grassBlockItemFix", "bugfixes", "grassBlockItemFix"),
		new Binding("bugfixes.hotbarRenderingFix", "bugfixes", "hotbarRenderingFix"),
		new Binding("bugfixes.itemstackRenderingFix", "bugfixes", "itemstackRenderingFix"),
		new Binding("bugfixes.lastDurabilityFix", "bugfixes", "lastDurabilityFix"),
		new Binding("bugfixes.leggingsWhenRidingFix", "bugfixes", "leggingsWhenRidingFix"),
		new Binding("bugfixes.liquidBlockDropFix", "bugfixes", "liquidBlockDropFix"),
		new Binding("bugfixes.miningDelayFix", "bugfixes", "miningDelayFix"),
		new Binding("bugfixes.multiplayerEntityJitterFix", "bugfixes", "multiplayerEntityJitterFix"),
		new Binding("bugfixes.nightmarePathfindingFix", "bugfixes", "nightmarePathfindingFix"),
		new Binding("bugfixes.sleepingCameraRotationFix", "bugfixes", "sleepingCameraRotationFix"),
		new Binding("bugfixes.torchBottomFaceFix", "bugfixes", "torchBottomFaceFix"),
		new Binding("bugfixes.woodenSlabMiningFix", "bugfixes", "woodenSlabMiningFix"),
		new Binding("mobs.pigSaddleDropFix", "bugfixes", "pigSaddleDropFix"),

		// -- Bugfixes UniTweaks spells differently ----------------------------------------------------
		new Binding("bugfixes.slimeSplitFix", "bugfixes", "enableSlimeSplitFix"),
		new Binding("blocks.fenceShapeFixes", "bugfixes", "fenceBoundingBoxFix"),
		new Binding("blocks.stairFixes", "bugfixes", "stairsDropFix"),
		new Binding("blocks.waterFixes", "bugfixes", "springPropagationFix"),
		new Binding("blocks.lavaFixes", "bugfixes", "lavaWithoutSourceFix"),

		// -- Better burning, leaf decay, sounds --------------------------------------------------------
		new Binding("mobs.betterBurning.enabled", "features", "betterBurning", "enableBetterBurning"),
		new Binding("mobs.betterBurning.skeletonsBurningArrows", "features", "betterBurning", "skeletonsBurningArrows"),
		new Binding("mobs.betterBurning.skeletonBurningArrowChance", "features", "betterBurning", "skeletonBurningArrowChance"),
		new Binding("mobs.betterBurning.burningArrowsSetOnFire", "features", "betterBurning", "burningArrowsSetOnFire"),
		new Binding("mobs.betterBurning.burningEntitySpread", "features", "betterBurning", "burningEntitySpread"),
		new Binding("mobs.betterBurning.burningEntitySpreadChance", "features", "betterBurning", "burningEntitySpreadChance"),
		new Binding("world.flora.fastLeafDecay", "features", "fastLeafDecay", "enableFastLeafDecay"),
		new Binding("world.flora.minimumDecayTime", "features", "fastLeafDecay", "minimumDecayTime"),
		new Binding("world.flora.maximumDecayTime", "features", "fastLeafDecay", "maximumDecayTime"),
		new Binding("sounds.moreSounds", "features", "moreSounds"),

		// -- Remaining tweaks, flora and multiplayer ---------------------------------------------------
		new Binding("mobs.expandChickenHitbox", "tweaks", "expandChickenHitbox"),
		new Binding("world.flora.shearsCollectTallGrass", "tweaks", "shearHarvesting"),
		new Binding("world.flora.beta18LeavesRendering", "tweaks", "beta18LeavesRendering"),
		new Binding("world.flora.disableTallGrassGeneration", "oldfeatures", "disableTallGrassGeneration"),
		new Binding("world.flora.disableDeadBushGeneration", "oldfeatures", "disableDeadBushGeneration"),
		new Binding("gameplay.oldFeatures.punchSheepForWool", "oldfeatures", "punchSheepForWool"),
		new Binding("multiplayer.tcpNoDelay", "general", "tcpNoDelay"),

		// -- Enums, matched by constant name; see convert() ----------------------------------------------
		new Binding("ui.frontViewThirdPerson", "userinterface", "frontViewThirdPerson"),
		new Binding("sounds.chestSounds", "features", "moreSoundsChests"),
	};

	/** Option key path -> binding, for the options this bridge has taken over. Empty without UniTweaks. */
	private static final Map<String, Binding> BY_PATH = index();

	private static Map<String, Binding> index() {
		Map<String, Binding> out = new LinkedHashMap<>();
		if (!Mods.HAS_UNITWEAKS) return out;
		for (Binding binding : BINDINGS) out.put(binding.optionPath(), binding);
		return out;
	}

	/**
	 * True when this option is driven by RetroTweaks and read by UniTweaks - so the screen should leave
	 * it editable rather than greying it out, and {@link ConfigTree#unavailableReasonOf} should say
	 * nothing about it.
	 */
	public static boolean isDelegated(String optionPath) {
		return BY_PATH.containsKey(optionPath);
	}

	// ---------------------------------------------------------------- pushing

	/**
	 * Copies every bound option into UniTweaks' live config and writes its YAML. Called after the
	 * RetroTweaks config is loaded and after every save, which together cover "at startup" and "the
	 * moment the player changed something".
	 */
	public static void push(ConfigTree.Category root) {
		if (BY_PATH.isEmpty() || root == null) return;
		Set<String> changed = new LinkedHashSet<>();
		for (Map.Entry<String, Binding> entry : BY_PATH.entrySet()) {
			ConfigTree.Option option = findOption(root, entry.getKey());
			if (option == null) {
				// A binding naming an option that no longer exists is a bug in the table above, not
				// something the player can fix - say so rather than failing silently.
				RetroTweaks.LOGGER.warn("UniTweaks bridge: no such RetroTweaks option '{}'", entry.getKey());
				continue;
			}
			Object value = option.get();
			if (!write(entry.getValue(), value)) continue;
			// Only a value that actually MOVED earns a YAML write. This runs on every ConfigManager.save,
			// which includes simply closing the screen, and rewriting six config files each time a player
			// looked at the options would be a lot of disk for nothing.
			if (!java.util.Objects.deepEquals(LAST_PUSHED.put(entry.getKey(), value), value)) {
				changed.add(entry.getValue().root());
			}
		}
		pushResourceUrl(root, changed);
		pushSleepSpawning(root, changed);
		for (String rootName : changed) save(rootName);
	}

	/** Synthetic {@link #LAST_PUSHED} key for the computed sleep-spawning value below. */
	private static final String SLEEP_SPAWNING_KEY = "gameplay.disableSleepSpawning:effective";

	/**
	 * Folds "Bed Behavior When Used = Disable Nightmares" into UniTweaks' {@code disableSleepSpawning}.
	 *
	 * <p>Two RetroTweaks options ask for the same thing: the plain "Disable Sleep Mob Spawning" toggle
	 * (bound one-for-one above) and the DISABLE_NIGHTMARES mode of the bed-behavior enum. Both were
	 * implemented by {@code NaturalSpawnerMixin}, which stands down when UniTweaks is installed - the
	 * toggle's value still arrived through its binding, but the enum's half went nowhere and a player
	 * who chose Disable Nightmares silently got the ambush back. Running after the plain binding, this
	 * overwrites UniTweaks' field with the OR of the two, so either way of asking works.
	 */
	private static void pushSleepSpawning(ConfigTree.Category root, Set<String> changed) {
		if (BY_PATH.isEmpty() || root == null) return;
		ConfigTree.Option toggle = findOption(root, "gameplay.disableSleepSpawning");
		ConfigTree.Option bedBehavior = findOption(root, "gameplay.bedBehavior");
		if (toggle == null || bedBehavior == null) {
			RetroTweaks.LOGGER.warn("UniTweaks bridge: sleep-spawning options moved - fix pushSleepSpawning");
			return;
		}
		boolean value = Boolean.TRUE.equals(toggle.get())
			|| bedBehavior.get() == com.periut.retrotweaks.config.Enums.BedBehavior.DISABLE_NIGHTMARES;
		if (!write(new Binding(SLEEP_SPAWNING_KEY, "tweaks", "disableSleepSpawning"), value)) return;
		if (!java.util.Objects.deepEquals(LAST_PUSHED.put(SLEEP_SPAWNING_KEY, value), value)) {
			changed.add("tweaks");
		}
	}

	/**
	 * Synthetic {@link #LAST_PUSHED} key for the computed resource URL below - not a real option path,
	 * so it can never collide with a {@link #BINDINGS} entry.
	 */
	private static final String RESOURCE_URL_KEY = "multiplayer.resourcesDownloadUrl:effective";

	/**
	 * Pushes the sound/music download URL - the one binding that cannot be a {@link Binding} row,
	 * because RetroTweaks' side is FOUR options (enable, primary URL, use-alternate, alternate URL)
	 * folding into UniTweaks' single {@code general.resourceDownloadUrl} string.
	 *
	 * <p>Why it must be pushed at all: both mods {@code @ModifyConstant} the same dead vanilla URL in
	 * {@code ResourceDownloadThread.run}, and the handlers stack - RetroTweaks' (priority 1500,
	 * applied later, therefore inserted closer to the constant) runs FIRST, and UniTweaks' runs last
	 * and ignores its input entirely unless its own config string is empty. So with UniTweaks
	 * installed, whatever the player set on RetroTweaks' screen silently lost to UniTweaks' YAML -
	 * which can hold a stale mirror, or the truncated URL UniTweaks 0.17 wrote (its issue #26). A
	 * player whose sound worked on RetroTweaks alone then adds UniTweaks and loses every sound: the
	 * download thread aborts before registering even the locally cached files, and b1.7.3 goes fully
	 * silent. Pushing the effective RetroTweaks URL here makes the RetroTweaks option the control
	 * surface again, exactly like every other bound option.
	 *
	 * <p>"Download Resources" off pushes the empty string: UniTweaks treats empty as pass-through, and
	 * what reaches it is RetroTweaks' handler output, which for a disabled option is the vanilla URL -
	 * so "off" still honestly means vanilla behaviour.
	 */
	private static void pushResourceUrl(ConfigTree.Category root, Set<String> changed) {
		if (BY_PATH.isEmpty() || root == null) return;
		ConfigTree.Option enabled = findOption(root, "multiplayer.useResourcesDownloadUrl");
		ConfigTree.Option useAlternate = findOption(root, "multiplayer.useAlternateResourcesUrl");
		ConfigTree.Option primary = findOption(root, "multiplayer.resourcesDownloadUrl");
		ConfigTree.Option alternate = findOption(root, "multiplayer.alternateResourcesDownloadUrl");
		if (enabled == null || useAlternate == null || primary == null || alternate == null) {
			RetroTweaks.LOGGER.warn("UniTweaks bridge: resource URL options moved - fix pushResourceUrl");
			return;
		}
		String url = "";
		if (Boolean.TRUE.equals(enabled.get())) {
			Object chosen = Boolean.TRUE.equals(useAlternate.get()) ? alternate.get() : primary.get();
			if (chosen instanceof String s) url = s;
		}
		if (!write(new Binding(RESOURCE_URL_KEY, "general", "resourceDownloadUrl"), url)) return;
		if (!java.util.Objects.deepEquals(LAST_PUSHED.put(RESOURCE_URL_KEY, url), url)) {
			changed.add("general");
		}
	}

	/** What each bound option was last seen holding, so {@link #push} can tell a change from a re-push. */
	private static final Map<String, Object> LAST_PUSHED = new LinkedHashMap<>();

	private static ConfigTree.Option findOption(ConfigTree.Category root, String path) {
		String[] parts = path.split("\\.");
		ConfigTree.Category category = root;
		for (int i = 0; i < parts.length - 1; i++) {
			category = ConfigTree.find(category, parts[i]);
			if (category == null) return null;
		}
		return ConfigTree.findOption(category, parts[parts.length - 1]);
	}

	/** @return true when the value actually reached UniTweaks. */
	private static boolean write(Binding binding, Object value) {
		Object target = rootObject(binding.root());
		if (target == null) return false;
		try {
			String[] path = binding.fieldPath();
			for (int i = 0; i < path.length - 1; i++) {
				Field step = target.getClass().getField(path[i]);
				target = step.get(target);
				if (target == null) return false;
			}
			Field field = target.getClass().getField(path[path.length - 1]);
			Object assignable = value;
			if (value != null && !field.getType().isInstance(value)) {
				assignable = convert(value, field.getType());
				if (assignable == null) {
					RetroTweaks.LOGGER.warn("UniTweaks bridge: {} is {} but {} holds {}", binding.optionPath(),
						value.getClass().getSimpleName(), String.join(".", path), field.getType().getSimpleName());
					return false;
				}
			}
			field.set(target, assignable);
			return true;
		} catch (ReflectiveOperationException | RuntimeException e) {
			RetroTweaks.LOGGER.warn("UniTweaks bridge: could not write {}", binding.optionPath(), e);
			return false;
		}
	}

	/**
	 * Bridges the two enum types that describe the same choice under different class names, by CONSTANT
	 * NAME - {@code Enums.FrontView} to UniTweaks' {@code FrontViewMode} (DISABLED/NORMAL/HIDE_HUD, an
	 * exact match) and {@code Enums.ChestSounds} to its {@code ChestSoundsEnum} (ours is NONE/DOOR, a
	 * subset of its NONE/MODERN/DOOR - RetroTweaks ships no sound files, so it never asks for MODERN).
	 *
	 * <p>Matching on the name rather than the ordinal on purpose: an ordinal silently maps to whatever
	 * happens to sit at that index if either mod ever inserts a constant, whereas a name that stops
	 * existing returns null here and is reported instead of quietly setting the wrong thing.
	 *
	 * @return the converted value, or null when there is no honest conversion
	 */
	private static Object convert(Object value, Class<?> target) {
		if (value instanceof Enum<?> source && target.isEnum()) {
			for (Object constant : target.getEnumConstants()) {
				if (((Enum<?>) constant).name().equals(source.name())) return constant;
			}
		}
		return null;
	}

	// ---------------------------------------------------------------- reflection onto UniTweaks/GCAPI

	private static final Map<String, Object> ROOT_CACHE = new LinkedHashMap<>();

	private static Object rootObject(String rootName) {
		if (ROOT_CACHE.containsKey(rootName)) return ROOT_CACHE.get(rootName);
		Object value = null;
		try {
			Class<?> uniTweaks = Class.forName("net.danygames2014.unitweaks.UniTweaks");
			value = uniTweaks.getField(ROOT_FIELDS.get(rootName)).get(null);
		} catch (ReflectiveOperationException | RuntimeException e) {
			RetroTweaks.LOGGER.warn("UniTweaks bridge: could not reach its '{}' config", rootName, e);
		}
		ROOT_CACHE.put(rootName, value);
		return value;
	}

	/**
	 * Writes one UniTweaks config root to disk through GCAPI3's own saver, so its screen shows what
	 * RetroTweaks set rather than what the file used to say. Best effort by design - see the class doc.
	 */
	private static void save(String rootName) {
		try {
			Class<?> core = Class.forName("net.glasslauncher.mods.gcapi3.impl.GCCore");
			Map<?, ?> configs = (Map<?, ?>) core.getField("MOD_CONFIGS").get(null);
			Object entry = configs.get(rootName);
			if (entry == null) return;
			Object modContainer = entry.getClass().getMethod("modContainer").invoke(entry);
			Object handler = entry.getClass().getMethod("configCategoryHandler").invoke(entry);
			for (java.lang.reflect.Method method : core.getMethods()) {
				if (!method.getName().equals("saveConfig") || method.getParameterCount() != 3) continue;
				// 32 is the width GCAPI's own Back button passes; matching it keeps the file's comment
				// wrapping identical to one saved from its screen.
				method.invoke(null, modContainer, handler, 32);
				return;
			}
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			RetroTweaks.LOGGER.warn("UniTweaks bridge: values applied, but GCAPI would not save '{}'"
				+ " - they will be re-applied on the next launch", rootName, e);
		}
	}

	/** Every option path this bridge owns, for diagnostics. */
	public static List<String> delegatedPaths() {
		return new ArrayList<>(BY_PATH.keySet());
	}
}
