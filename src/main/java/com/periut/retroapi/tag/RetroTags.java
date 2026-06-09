package com.periut.retroapi.tag;

import com.periut.retroapi.RetroAPI;
import com.periut.retroapi.registry.BlockRegistration;
import com.periut.retroapi.registry.RetroRegistry;
import net.minecraft.block.Block;
import net.ornithemc.osl.core.api.util.NamespacedIdentifiers;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The block tag engine. Tags come from two places that union together:
 *
 * <ul>
 *   <li><b>Data</b>: {@code data/{ns}/tags/block/**.json} files in any mod (modern schema,
 *       see {@link RetroTagLoader}), resolved lazily on first query so every mod has
 *       finished registering its blocks by then.</li>
 *   <li><b>Code</b>: {@link #addToTag(RetroTagKey, Block...)}, which is also what
 *       {@code RetroBlockAccess.mineable(RetroTool...)} sugars to.</li>
 * </ul>
 *
 * <p>The flagship consumer is the {@code mineable/{pickaxe,axe,shovel,hoe,sword}} family,
 * wired into mining speed and drops by {@code PlayerEntityMixin}.</p>
 */
public final class RetroTags {

	/** Code-registered membership, live immediately. */
	private static final Map<String, Set<Block>> CODE_TAGS = new HashMap<>();
	/** Data-file membership, resolved on first query. */
	private static Map<String, Set<Block>> dataTags = null;
	/** Block to the set of mineable tools whose tag contains it, cached for the harvest hook. */
	private static Map<Block, Set<RetroTool>> mineableCache = null;

	private RetroTags() {}

	/** True if the block is in the tag (data or code membership). */
	public static boolean isIn(Block block, RetroTagKey tag) {
		if (block == null) {
			return false;
		}
		Set<Block> code = CODE_TAGS.get(tag.getPath());
		if (code != null && code.contains(block)) {
			return true;
		}
		Set<Block> data = resolved().get(tag.getPath());
		return data != null && data.contains(block);
	}

	/** Every block in the tag (data and code membership unioned). Never null. */
	public static Set<Block> all(RetroTagKey tag) {
		Set<Block> result = new LinkedHashSet<>();
		Set<Block> data = resolved().get(tag.getPath());
		if (data != null) {
			result.addAll(data);
		}
		Set<Block> code = CODE_TAGS.get(tag.getPath());
		if (code != null) {
			result.addAll(code);
		}
		return Collections.unmodifiableSet(result);
	}

	/** Adds blocks to a tag from code. Unions with data files, replaces nothing. */
	public static void addToTag(RetroTagKey tag, Block... blocks) {
		Set<Block> set = CODE_TAGS.computeIfAbsent(tag.getPath(), k -> new LinkedHashSet<>());
		Collections.addAll(set, blocks);
		mineableCache = null;
		tierCache = null;
	}

	/**
	 * The mineable tools whose tag contains this block, or an empty set. Cached; this is
	 * the hot path for {@code PlayerEntityMixin.canHarvest}/{@code getBlockBreakingSpeed}.
	 */
	public static Set<RetroTool> mineableTools(Block block) {
		if (mineableCache == null) {
			Map<Block, Set<RetroTool>> cache = new HashMap<>();
			for (RetroTool tool : RetroTool.values()) {
				for (Block b : all(tool.mineableTag())) {
					cache.computeIfAbsent(b, k -> new HashSet<>()).add(tool);
				}
			}
			mineableCache = cache;
		}
		Set<RetroTool> tools = mineableCache.get(block);
		return tools != null ? tools : Collections.emptySet();
	}

	/** Block to its required tool tier, from the needs_<tier>_tool tags; cached like mineable. */
	private static Map<Block, RetroToolTier> tierCache = null;

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
				for (Block b : all(tag)) {
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

	// ------------------------------------------------------------ resolution --

	private static synchronized Map<String, Set<Block>> resolved() {
		if (dataTags != null) {
			return dataTags;
		}
		Map<String, List<RetroTagLoader.Entry>> raw = RetroTagLoader.loadAll();
		Map<String, Set<Block>> result = new HashMap<>();
		for (String path : raw.keySet()) {
			resolveTag(path, raw, result, new HashSet<>());
		}
		dataTags = result;
		mineableCache = null;
		tierCache = null;
		return result;
	}

	private static Set<Block> resolveTag(String path, Map<String, List<RetroTagLoader.Entry>> raw,
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
					if (raw.containsKey(ref) || CODE_TAGS.containsKey(ref)) {
						blocks.addAll(resolveTag(ref, raw, result, resolving));
						Set<Block> code = CODE_TAGS.get(ref);
						if (code != null) {
							blocks.addAll(code);
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
}
