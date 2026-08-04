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
import java.util.EnumSet;
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
	/** Vanilla owns block ids 1-255; everything from here up is modded (see IdAssigner). */
	private static final int VANILLA_BLOCK_IDS = 256;

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
		if (tools == null) {
			tools = defaultMineableTools(block);
		}
		return withDisguise(block, tools);
	}

	/**
	 * Adds whatever the block is currently DISGUISED as answers to, on top of what it answers to itself.
	 *
	 * <p>A block that wears another block should be mined with the tool for what it is wearing, and beta
	 * has nowhere to say that: mining speed and the harvest check are both handed a {@code Block} and
	 * nothing else, so every position of a block gets one answer. The position comes from the break the
	 * player is currently making ({@link RetroBreakTarget}), which is validated against the world, so a
	 * stale record cannot answer for somewhere else.
	 *
	 * <p>Additive, never subtractive. A wooden frame wearing stone answers to an axe AND a pickaxe;
	 * taking the axe away would mean a block got harder to break the more it had been decorated.
	 */
	private static Set<RetroTool> withDisguise(Block block, Set<RetroTool> own) {
		if (!(block instanceof com.periut.retroapi.register.block.RetroBlockDisguise)) {
			return own;
		}
		Block worn = com.periut.retroapi.register.block.RetroDisguises.atCurrentBreak(block);
		if (worn == null || worn == block) {
			return own;
		}
		Set<RetroTool> wornTools = mineableTools(worn);
		if (wornTools.isEmpty()) {
			return own;
		}
		Set<RetroTool> both = EnumSet.noneOf(RetroTool.class);
		both.addAll(own);
		both.addAll(wornTools);
		return both;
	}

	/**
	 * What a MODDED block is mineable with when nothing declared it - inferred from its material, the way
	 * modern Minecraft's own tags are laid out (stone/metal -&gt; pickaxe, wood -&gt; axe, soil/sand -&gt;
	 * shovel, leaves -&gt; shears + hoe + sword, wool -&gt; shears).
	 *
	 * <p>This closes the trap behind "my tool can't break my block". Beta decides whether a tool is
	 * effective from hardcoded block LISTS inside each vanilla tool item - lists a modded block obviously
	 * is not in - so a modded stone-material block dropped nothing for ANY tool, vanilla or modded, until
	 * its author found {@code .mineable(...)}. Inferring the obvious default makes a block behave like the
	 * vanilla blocks it is made of, while an explicit {@code .mineable(...)} or a data tag still wins.
	 *
	 * <p><b>Vanilla blocks are excluded on purpose.</b> Their membership is spelled out block by block in
	 * {@link VanillaToolTags}, transcribed from beta's own tool code, and that list is the whole truth
	 * about them - falling through to material inference for the blocks it omits would silently rewrite
	 * vanilla gameplay. It did exactly that once: leaves are the {@code LEAVES} material, modern
	 * Minecraft files leaves under {@code mineable/hoe}, and so installing RetroAPI made every hoe in
	 * beta chop leaves faster than it should. A library has no business changing how vanilla plays, so
	 * inference now stops at the modded id range. A mod that WANTS beta leaves in a tag can still say so
	 * with {@code RetroTags.addToTag(RetroTool.HOE.mineableTag(), Block.LEAVES)}.
	 */
	public static Set<RetroTool> defaultMineableTools(Block block) {
		if (block == null || block.id < VANILLA_BLOCK_IDS) {
			return Collections.emptySet();
		}
		net.minecraft.block.material.Material material = block.material;
		if (material == null) {
			return Collections.emptySet();
		}
		if (material == net.minecraft.block.material.Material.STONE
			|| material == net.minecraft.block.material.Material.METAL
			|| material == net.minecraft.block.material.Material.ICE
			|| material == net.minecraft.block.material.Material.PISTON) {
			return DEFAULT_PICKAXE;
		}
		if (material == net.minecraft.block.material.Material.WOOD
			|| material == net.minecraft.block.material.Material.PUMPKIN) {
			return DEFAULT_AXE;
		}
		if (material == net.minecraft.block.material.Material.SOIL
			|| material == net.minecraft.block.material.Material.SAND
			|| material == net.minecraft.block.material.Material.CLAY
			|| material == net.minecraft.block.material.Material.SOLID_ORGANIC
			|| material == net.minecraft.block.material.Material.SNOW_BLOCK
			|| material == net.minecraft.block.material.Material.SNOW_LAYER) {
			return DEFAULT_SHOVEL;
		}
		if (material == net.minecraft.block.material.Material.LEAVES) {
			return DEFAULT_LEAVES;
		}
		if (material == net.minecraft.block.material.Material.WOOL
			|| material == net.minecraft.block.material.Material.COBWEB) {
			return DEFAULT_SHEARS;
		}
		return Collections.emptySet();
	}

	private static final Set<RetroTool> DEFAULT_PICKAXE = Collections.unmodifiableSet(EnumSet.of(RetroTool.PICKAXE));
	private static final Set<RetroTool> DEFAULT_AXE = Collections.unmodifiableSet(EnumSet.of(RetroTool.AXE));
	private static final Set<RetroTool> DEFAULT_SHOVEL = Collections.unmodifiableSet(EnumSet.of(RetroTool.SHOVEL));
	private static final Set<RetroTool> DEFAULT_SHEARS = Collections.unmodifiableSet(EnumSet.of(RetroTool.SHEARS));
	/** Leaves: shears cut them cleanly, and a sword or hoe hacks through them faster than a hand. */
	private static final Set<RetroTool> DEFAULT_LEAVES = Collections.unmodifiableSet(
		EnumSet.of(RetroTool.SHEARS, RetroTool.SWORD, RetroTool.HOE));

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
						// Code membership straight from the map, NOT via blocksIn: that calls
						// resolvedBlocks(), which is what we are in the middle of, so the cache is still
						// null and the whole resolution restarts - one referencing tag and it recurses
						// until the stack dies.
						blocks.addAll(codeMembers(RetroTagKey.block(ref), Block.class));
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

	/**
	 * Code-registered members of a tag, read directly out of {@link #CODE_TAGS}. Safe to call from inside
	 * tag resolution, which the public {@code blocksIn}/{@code itemsIn} are not.
	 */
	private static <T> Set<T> codeMembers(RetroTagKey tag, Class<T> type) {
		Set<Object> code = CODE_TAGS.get(tag);
		if (code == null || code.isEmpty()) {
			return Collections.emptySet();
		}
		Set<T> out = new LinkedHashSet<>();
		for (Object o : code) {
			if (type.isInstance(o)) {
				out.add(type.cast(o));
			}
		}
		return out;
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
						// Code membership straight from the map - see resolveBlockTag for why itemsIn()
						// must not be used here.
						items.addAll(codeMembers(RetroTagKey.item(ref), Item.class));
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
