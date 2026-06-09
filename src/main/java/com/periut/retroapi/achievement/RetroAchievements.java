package com.periut.retroapi.achievement;

import com.periut.retroapi.lang.LangLoader;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.achievement.Achievement;
import net.minecraft.achievement.Achievements;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stat.Stat;
import net.minecraft.stat.Stats;
import net.ornithemc.osl.core.api.util.NamespacedIdentifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Public registration + grant API for modded achievements.
 *
 * <p><b>Phase-1 scope.</b> Achievements use explicit numeric ids in a high band that clears
 * vanilla stat ids. The band copies StationAPI's StatRegistry base {@code 0x1040000}. NOTE: the
 * vanilla {@code Achievement} constructor itself adds the achievement shift {@code 0x500000} to the
 * id you pass, so the achievement's final {@code Stat.id} is {@code (base + n) + 0x500000} - well
 * clear of vanilla item/block stat ids (which run around {@code 0x1000000 + id}). Because the same
 * id is allocated for the same string id on both client and server, the vanilla
 * {@code IncreaseStatS2CPacket(int,int)} sync path works with zero extra networking or persistence.
 *
 * <p>The {@code name} passed to {@code register} is the SHORT key; the vanilla constructor prepends
 * {@code "achievement."}, so the title lang key is {@code achievement.<name>} and the description key
 * is {@code achievement.<name>.desc}.
 *
 * <p>When StationAPI is present every achievement is additionally bound into StationAPI's
 * {@code StatRegistry} (via {@link com.periut.retroapi.compat.StationBridge#bindAchievement}),
 * otherwise that registry refuses to freeze ("Trying to access unbound value").
 */
public final class RetroAchievements {
	private static final Logger LOGGER = LogManager.getLogger("RetroAPI/Achievements");

	/** StationAPI's StatRegistry base ({@code 0x1040000}). The vanilla ctor adds {@link #ACHIEVEMENT_ID_SHIFT}. */
	public static final int ID_BASE = 0x1040000;
	/** Achievement shift ({@code 0x500000}) - applied by the vanilla {@code Achievement} constructor. */
	public static final int ACHIEVEMENT_ID_SHIFT = 0x500000;

	// We pass (ID_BASE + n) to the ctor, which then adds ACHIEVEMENT_ID_SHIFT internally.
	private static int nextId = ID_BASE;

	/** string id -> allocated numeric id, for stable lookups / debugging. */
	private static final Map<String, Integer> ID_BY_NAME = new HashMap<>();

	private static Boolean hasStationAPI;

	private RetroAchievements() {}

	private static boolean stationAPI() {
		if (hasStationAPI == null) {
			hasStationAPI = FabricLoader.getInstance().isModLoaded("stationapi");
		}
		return hasStationAPI;
	}

	// ---------------------------------------------------------------------------------------------
	// Registration - auto-allocated id
	// ---------------------------------------------------------------------------------------------

	public static Achievement register(NamespacedIdentifier id, String name,
	                                   int column, int row, Block icon, Achievement parent) {
		return register(allocateId(id), id, name, column, row, icon, parent);
	}

	public static Achievement register(NamespacedIdentifier id, String name,
	                                   int column, int row, Item icon, Achievement parent) {
		return register(allocateId(id), id, name, column, row, icon, parent);
	}

	public static Achievement register(NamespacedIdentifier id, String name,
	                                   int column, int row, ItemStack icon, Achievement parent) {
		return register(allocateId(id), id, name, column, row, icon, parent);
	}

	// ---------------------------------------------------------------------------------------------
	// Registration - explicit fixed id (Phase-1 path; mirrors Aether's acOff=800 scheme).
	// The vanilla ctor adds ACHIEVEMENT_ID_SHIFT (0x500000) to fixedId internally.
	// ---------------------------------------------------------------------------------------------

	public static Achievement register(int fixedId, NamespacedIdentifier id, String name,
	                                   int column, int row, Block icon, Achievement parent) {
		ensureLangLoaded();
		Achievement a = new Achievement(fixedId, name, column, row, icon, parent);
		return finish(a, id);
	}

	public static Achievement register(int fixedId, NamespacedIdentifier id, String name,
	                                   int column, int row, Item icon, Achievement parent) {
		ensureLangLoaded();
		Achievement a = new Achievement(fixedId, name, column, row, icon, parent);
		return finish(a, id);
	}

	public static Achievement register(int fixedId, NamespacedIdentifier id, String name,
	                                   int column, int row, ItemStack icon, Achievement parent) {
		ensureLangLoaded();
		Achievement a = new Achievement(fixedId, name, column, row, icon, parent);
		return finish(a, id);
	}

	/**
	 * Vanilla {@code Achievement} translates its title/description EAGERLY in its constructor, and
	 * Fabric runs {@code init} entrypoints in mod LOAD order (alphabetical), not dependency order -
	 * so a dependent mod's init (e.g. "aether") can reach here BEFORE {@code RetroAPI.init} has
	 * loaded the mod lang files. Force them in now; {@code loadModLangFiles} is idempotent and
	 * no-ops under StationAPI.
	 */
	private static void ensureLangLoaded() {
		LangLoader.loadModLangFiles();
	}

	@SuppressWarnings("unchecked")
	private static Achievement finish(Achievement a, NamespacedIdentifier id) {
		// Registers into Stats.ID_TO_STAT + Stats.ALL - REQUIRED for MP sync + persistence + screen.
		a.addStat();
		Achievements.ACHIEVEMENTS.add(a);
		ID_BY_NAME.put(id.toString(), a.id);

		// Under StationAPI, also bind into its StatRegistry or it won't freeze.
		if (stationAPI()) {
			com.periut.retroapi.compat.StationBridges.get().bindAchievement(a, id.namespace(), id.identifier());
		}
		LOGGER.info("Registered achievement {} -> id {}", id, a.id);
		return a;
	}

	private static int allocateId(NamespacedIdentifier id) {
		Integer existing = ID_BY_NAME.get(id.toString());
		if (existing != null) return existing;
		return nextId++;
	}

	// ---------------------------------------------------------------------------------------------
	// Grant - pure vanilla; mirrors Aether's giveAchievement
	// ---------------------------------------------------------------------------------------------

	/** Grants (or counts) an achievement/stat for a player. Vanilla handles SP immediately and MP
	 *  via {@code IncreaseStatS2CPacket}. */
	public static void grant(Achievement achievement, PlayerEntity player) {
		player.incrementStat(achievement);
	}

	// ---------------------------------------------------------------------------------------------
	// Lazy item/block-stat helper - never NPE on expanded null slots
	// ---------------------------------------------------------------------------------------------

	/**
	 * Lazily creates + registers a stat in an expanded {@link net.minecraft.stat.Stats} array slot
	 * (e.g. {@code Stats.MINE_BLOCK}) so the {@link com.periut.retroapi.mixin.register.StatsExpandMixin}
	 * null slots never NPE in {@code increaseStat}. Returns the existing stat if already populated.
	 *
	 * <p>Each of the four arrays gets its own {@code 0x10000}-spaced id sub-band inside the RetroAPI
	 * item-stat band ({@code 0x1040000}-{@code 0x107FFFF}), mirroring vanilla's per-array spacing
	 * ({@code 0x1000000}/{@code 0x1010000}/...). This keeps lazily-created ids unique <em>across</em>
	 * arrays (so the same {@code index} in {@code MINE_BLOCK} and {@code CRAFTED} never collide in
	 * {@code Stats.ID_TO_STAT}) and clear of both vanilla item-stat ids and achievement ids
	 * ({@code 0x1540000}+).
	 *
	 * @param array       one of {@code Stats.MINE_BLOCK/CRAFTED/USED/BROKEN}
	 * @param index       the modded block/item id (must be < array length after expansion)
	 * @param statKey     the stat's translation key
	 */
	public static Stat itemStat(Stat[] array, int index, String statKey) {
		if (array == null || index < 0 || index >= array.length) return null;
		Stat existing = array[index];
		if (existing != null) return existing;
		int statId = ID_BASE + (retroapi$arrayBand(array) << 16) + index;
		Stat created = new Stat(statId, statKey).addStat();
		array[index] = created;
		return created;
	}

	/** Distinct {@code 0x10000} sub-band per stat array so lazily-created ids never collide across arrays. */
	private static int retroapi$arrayBand(Stat[] array) {
		if (array == Stats.MINE_BLOCK) return 0;
		if (array == Stats.CRAFTED) return 1;
		if (array == Stats.USED) return 2;
		if (array == Stats.BROKEN) return 3;
		return 4; // unknown array: park in its own band to stay collision-free
	}

	/** @return the allocated numeric id for a registered achievement string id, or null. */
	public static Integer getNumericId(NamespacedIdentifier id) {
		return ID_BY_NAME.get(id.toString());
	}
}
