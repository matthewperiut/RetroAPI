package com.periut.retroapi.tag;

import com.periut.retroapi.RetroAPI;
import com.periut.retroapi.registry.BlockRegistration;
import com.periut.retroapi.registry.ItemRegistration;
import com.periut.retroapi.registry.RetroRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.ornithemc.osl.core.api.util.NamespacedIdentifiers;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The tag engine for both blocks and items. Membership comes from two places that union together:
 *
 * <ul>
 *   <li><b>Data</b>: {@code data/{ns}/tags/block(s)/**.json} and {@code tags/item(s)/**.json} files in any
 *       mod (modern schema), plus StationAPI's {@code data/{ns}/stationapi/tags/blocks|items/**.json}
 *       layout (see {@link RetroTagLoader}), resolved lazily on first query so every mod has finished
 *       registering by then.</li>
 *   <li><b>Code</b>: {@link #addToTag(RetroTagKey, Block...)} / {@link #addToTag(RetroTagKey, Item...)},
 *       callable at any time (RetroAPI's code tags are mutable at runtime, unlike StationAPI's, whose
 *       tags are frozen from resources). {@code RetroBlockAccess.mineable(...)} and
 *       {@code RetroItemAccess.tool(...)} sugar down to this.</li>
 * </ul>
 *
 * <p>Tags are namespace-blind: a tag file at {@code data/minecraft/.../mineable/pickaxe.json} and one at
 * {@code data/mymod/.../mineable/pickaxe.json} both feed the same {@code mineable/pickaxe} tag, so vanilla
 * files copied from modern Minecraft and mod additions union with zero ceremony.</p>
 *
 * <p>The flagship consumers are the {@code mineable/{pickaxe,axe,shovel,hoe,sword,shears}} family and the
 * {@code needs_<tier>_tool} tiers, wired into mining speed and drops by {@code PlayerEntityMixin}.</p>
 */
public final class RetroTags {

	/** Code-registered membership, live immediately, keyed by the full tag (category + path). */
	private static final Map<RetroTagKey, Set<Object>> CODE_TAGS = new HashMap<>();
	/** Data-file block membership, resolved on first query. */
	private static Map<String, Set<Block>> dataBlockTags = null;
	/** Data-file item membership, resolved on first query. */
	private static Map<String, Set<Item>> dataItemTags = null;
	/** Block to the set of mineable tools whose tag contains it, cached for the harvest hook. */
	private static Map<Block, Set<RetroTool>> mineableCache = null;
	/** Block to its required tool tier, cached like mineable. */
	private static Map<Block, RetroToolTier> tierCache = null;
	/** Guards the one-time lazy registration of the default vanilla tool-tag membership. */
	private static boolean vanillaDefaultsLoaded = false;

	private RetroTags() {}

	/**
	 * Registers the beta-accurate default {@code mineable}/{@code needs_<tier>_tool} membership for vanilla
	 * blocks on first query, so it is present no matter the mod-init order (an eager call in
	 * {@code RetroAPI.init()} could lose a race with a consumer mod that queries tags from ITS init first).
	 * Skipped under StationAPI, which owns vanilla harvesting itself.
	 */
	private static void ensureVanillaDefaults() {
		if (vanillaDefaultsLoaded) {
			return;
		}
		vanillaDefaultsLoaded = true;
		if (!FabricLoader.getInstance().isModLoaded("stationapi")) {
			VanillaToolTags.registerDefaults();
		}
	}

	// -------------------------------------------------------------- membership --

	/** True if the block is in the tag (data or code membership). */
	public static boolean isIn(Block block, RetroTagKey tag) {
		if (block == null) {
			return false;
		}
		ensureVanillaDefaults();
		Set<Object> code = CODE_TAGS.get(tag);
		if (code != null && code.contains(block)) {
			return true;
		}
		Set<Block> data = resolvedBlocks().get(tag.getPath());
		return data != null && data.contains(block);
	}

	/** True if the item is in the tag (data or code membership). */
	public static boolean isIn(Item item, RetroTagKey tag) {
		if (item == null) {
			return false;
		}
		Set<Object> code = CODE_TAGS.get(tag);
		if (code != null && code.contains(item)) {
			return true;
		}
		Set<Item> data = resolvedItems().get(tag.getPath());
		return data != null && data.contains(item);
	}

	/** Every block in the tag (data and code membership unioned). Never null. */
	public static Set<Block> blocksIn(RetroTagKey tag) {
		ensureVanillaDefaults();
		Set<Block> result = new LinkedHashSet<>();
		Set<Block> data = resolvedBlocks().get(tag.getPath());
		if (data != null) {
			result.addAll(data);
		}
		Set<Object> code = CODE_TAGS.get(tag);
		if (code != null) {
			for (Object o : code) {
				if (o instanceof Block b) {
					result.add(b);
				}
			}
		}
		return Collections.unmodifiableSet(result);
	}

	/** Every item in the tag (data and code membership unioned). Never null. */
	public static Set<Item> itemsIn(RetroTagKey tag) {
		Set<Item> result = new LinkedHashSet<>();
		Set<Item> data = resolvedItems().get(tag.getPath());
		if (data != null) {
			result.addAll(data);
		}
		Set<Object> code = CODE_TAGS.get(tag);
		if (code != null) {
			for (Object o : code) {
				if (o instanceof Item i) {
					result.add(i);
				}
			}
		}
		return Collections.unmodifiableSet(result);
	}

	/** @deprecated Ambiguous name; use {@link #blocksIn(RetroTagKey)}. */
	@Deprecated
	public static Set<Block> all(RetroTagKey tag) {
		return blocksIn(tag);
	}

	// -------------------------------------------------------- code registration --

	/** Adds blocks to a tag from code. Unions with data files, replaces nothing. Live immediately. */
	public static void addToTag(RetroTagKey tag, Block... blocks) {
		Set<Object> set = CODE_TAGS.computeIfAbsent(tag, k -> new LinkedHashSet<>());
		Collections.addAll(set, blocks);
		invalidateToolCaches();
	}

	/** Adds items to a tag from code. Unions with data files, replaces nothing. Live immediately. */
	public static void addToTag(RetroTagKey tag, Item... items) {
		Set<Object> set = CODE_TAGS.computeIfAbsent(tag, k -> new LinkedHashSet<>());
		Collections.addAll(set, items);
	}

	/** Removes code-registered blocks from a tag (data-file membership is untouched). */
	public static void removeFromTag(RetroTagKey tag, Block... blocks) {
		Set<Object> set = CODE_TAGS.get(tag);
		if (set != null) {
			for (Block b : blocks) {
				set.remove(b);
			}
			invalidateToolCaches();
		}
	}

	/** Removes code-registered items from a tag (data-file membership is untouched). */
	public static void removeFromTag(RetroTagKey tag, Item... items) {
		Set<Object> set = CODE_TAGS.get(tag);
		if (set != null) {
			for (Item i : items) {
				set.remove(i);
			}
		}
	}

	private static void invalidateToolCaches() {
		mineableCache = null;
		tierCache = null;
	}

	// ------------------------------------------------------------ tool helpers --

	/**
	 * The mineable tools whose tag contains this block, or an empty set. Cached; this is
	 * the hot path for {@code PlayerEntityMixin.canHarvest}/{@code getBlockBreakingSpeed}.
	 */
	public static Set<RetroTool> mineableTools(Block block) {
		if (mineableCache == null) {
			Map<Block, Set<RetroTool>> cache = new HashMap<>();
			for (RetroTool tool : RetroTool.values()) {
				for (Block b : blocksIn(tool.mineableTag())) {
					cache.computeIfAbsent(b, k -> new HashSet<>()).add(tool);
				}
			}
			mineableCache = cache;
		}
		Set<RetroTool> tools = mineableCache.get(block);
		return tools != null ? tools : Collections.emptySet();
	}

	/**
	 * The tool tier a block demands ({@code needs_stone_tool} and friends), or WOOD when it
	 * demands none. The highest tier wins if a block somehow sits in several tags.
	 */
	public static RetroToolTier requiredTier(Block block) {
		if (tierCache == null) {
			Map<Block, RetroToolTier> cache = new HashMap<>();
			for (RetroToolTier tier : RetroToolTier.values()) {
				RetroTagKey tag = tier.needsTag();
				if (tag == null) {
					continue;
				}
				for (Block b : blocksIn(tag)) {
					RetroToolTier existing = cache.get(b);
					if (existing == null || tier.getLevel() > existing.getLevel()) {
						cache.put(b, tier);
					}
				}
			}
			tierCache = cache;
		}
		RetroToolTier tier = tierCache.get(block);
		return tier != null ? tier : RetroToolTier.WOOD;
	}

	// ------------------------------------------------------------- resolution --

	private static synchronized Map<String, Set<Block>> resolvedBlocks() {
		if (dataBlockTags != null) {
			return dataBlockTags;
		}
		Map<String, List<RetroTagLoader.Entry>> raw = RetroTagLoader.loadBlockTags();
		Map<String, Set<Block>> result = new HashMap<>();
		for (String path : raw.keySet()) {
			resolveBlockTag(path, raw, result, new HashSet<>());
		}
		dataBlockTags = result;
		invalidateToolCaches();
		return result;
	}

	private static synchronized Map<String, Set<Item>> resolvedItems() {
		if (dataItemTags != null) {
			return dataItemTags;
		}
		Map<String, List<RetroTagLoader.Entry>> raw = RetroTagLoader.loadItemTags();
		Map<String, Set<Item>> result = new HashMap<>();
		for (String path : raw.keySet()) {
			resolveItemTag(path, raw, result, new HashSet<>());
		}
		dataItemTags = result;
		return result;
	}

	private static Set<Block> resolveBlockTag(String path, Map<String, List<RetroTagLoader.Entry>> raw,
			Map<String, Set<Block>> result, Set<String> resolving) {
		Set<Block> existing = result.get(path);
		if (existing != null) {
			return existing;
		}
		if (!resolving.add(path)) {
			RetroAPI.LOGGER.warn("Tag reference cycle at #{}; treating as empty", path);
			return Collections.emptySet();
		}
		Set<Block> blocks = new LinkedHashSet<>();
		List<RetroTagLoader.Entry> entries = raw.get(path);
		if (entries != null) {
			for (RetroTagLoader.Entry entry : entries) {
				if (entry.isReference()) {
					String ref = RetroTagKey.normalize(entry.value);
					if (raw.containsKey(ref) || CODE_TAGS.containsKey(RetroTagKey.block(ref))) {
						blocks.addAll(resolveBlockTag(ref, raw, result, resolving));
						for (Block b : blocksIn(RetroTagKey.block(ref))) {
							blocks.add(b);
						}
					} else if (entry.required) {
						RetroAPI.LOGGER.warn("Tag #{} references unknown tag {}", path, entry.value);
					}
				} else {
					Block block = resolveBlock(entry.value);
					if (block != null) {
						blocks.add(block);
					} else if (entry.required && !entry.value.startsWith("minecraft:")) {
						RetroAPI.LOGGER.warn("Tag #{} references unknown block {}", path, entry.value);
					}
					// Unknown minecraft: names skip silently: modern tag files are full of
					// post-beta blocks, and skipping them is what makes those files reusable.
				}
			}
		}
		resolving.remove(path);
		result.put(path, blocks);
		return blocks;
	}

	private static Set<Item> resolveItemTag(String path, Map<String, List<RetroTagLoader.Entry>> raw,
			Map<String, Set<Item>> result, Set<String> resolving) {
		Set<Item> existing = result.get(path);
		if (existing != null) {
			return existing;
		}
		if (!resolving.add(path)) {
			RetroAPI.LOGGER.warn("Item tag reference cycle at #{}; treating as empty", path);
			return Collections.emptySet();
		}
		Set<Item> items = new LinkedHashSet<>();
		List<RetroTagLoader.Entry> entries = raw.get(path);
		if (entries != null) {
			for (RetroTagLoader.Entry entry : entries) {
				if (entry.isReference()) {
					String ref = RetroTagKey.normalize(entry.value);
					if (raw.containsKey(ref) || CODE_TAGS.containsKey(RetroTagKey.item(ref))) {
						items.addAll(resolveItemTag(ref, raw, result, resolving));
						for (Item i : itemsIn(RetroTagKey.item(ref))) {
							items.add(i);
						}
					} else if (entry.required) {
						RetroAPI.LOGGER.warn("Item tag #{} references unknown tag {}", path, entry.value);
					}
				} else {
					Item item = resolveItem(entry.value);
					if (item != null) {
						items.add(item);
					} else if (entry.required && !entry.value.startsWith("minecraft:")) {
						RetroAPI.LOGGER.warn("Item tag #{} references unknown item {}", path, entry.value);
					}
				}
			}
		}
		resolving.remove(path);
		result.put(path, items);
		return items;
	}

	private static Block resolveBlock(String name) {
		int colon = name.indexOf(':');
		String ns = colon >= 0 ? name.substring(0, colon) : "minecraft";
		String id = colon >= 0 ? name.substring(colon + 1) : name;
		if ("minecraft".equals(ns)) {
			return VanillaBlockNames.resolve(id);
		}
		BlockRegistration reg = RetroRegistry.getBlockById(NamespacedIdentifiers.from(ns, id));
		return reg != null ? reg.getBlock() : null;
	}

	private static Item resolveItem(String name) {
		int colon = name.indexOf(':');
		String ns = colon >= 0 ? name.substring(0, colon) : "minecraft";
		String id = colon >= 0 ? name.substring(colon + 1) : name;
		if ("minecraft".equals(ns)) {
			Item vanilla = VanillaItemNames.resolve(id);
			if (vanilla != null) {
				return vanilla;
			}
			// A vanilla block name in an item tag resolves to that block's item.
			Block block = VanillaBlockNames.resolve(id);
			return block != null && block.id < Item.ITEMS.length ? Item.ITEMS[block.id] : null;
		}
		ItemRegistration reg = RetroRegistry.getItemById(NamespacedIdentifiers.from(ns, id));
		return reg != null ? reg.getItem() : null;
	}
}
