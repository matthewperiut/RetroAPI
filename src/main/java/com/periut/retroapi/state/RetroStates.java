package com.periut.retroapi.state;

import com.periut.retroapi.RetroAPI;
import com.periut.retroapi.storage.ChunkExtendedBlocks;
import com.periut.retroapi.storage.ExtendedBlocksAccess;
import net.minecraft.block.Block;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * The flattened block state platform. Blocks declare properties in code
 * ({@code RetroBlockAccess.states(...)}) or in their blockstate JSON
 * ({@code "properties": {...}}); every combination gets a flattened index whose bits 0-3
 * ride vanilla metadata and bits 4-11 ("secondary meta", xmeta) live in the region
 * sidecar. Blocks that never declare properties get an implicit {@code meta} property
 * (0-15) on first query, so the whole platform (blockstate JSONs, models, variant keys)
 * works uniformly on plain meta blocks.
 */
public final class RetroStates {

	private static final Map<Block, RetroStateDefinition> DEFINITIONS = new HashMap<>();

	private RetroStates() {}

	/** Declares a block's state definition. Called by {@code RetroBlockAccess.states(...)}. */
	public static void define(Block block, List<RetroProperty<?>> properties, UnaryOperator<RetroBlockState> defaultOp) {
		if (DEFINITIONS.containsKey(block)) {
			RetroAPI.LOGGER.warn("State definition for {} declared twice; keeping the first", block.getTranslationKey());
			return;
		}
		DEFINITIONS.put(block, new RetroStateDefinition(block, properties, defaultOp));
	}

	/**
	 * Declares a definition from blockstate JSON data. Code declarations win: if the block
	 * already has one, the data declaration is checked for agreement and otherwise ignored.
	 */
	public static void defineFromData(Block block, List<RetroProperty<?>> properties) {
		RetroStateDefinition existing = DEFINITIONS.get(block);
		if (existing != null) {
			if (existing.properties.size() != properties.size()) {
				RetroAPI.LOGGER.warn("Blockstate JSON for {} declares {} properties but code declared {}; code wins",
					block.getTranslationKey(), properties.size(), existing.properties.size());
			}
			return;
		}
		DEFINITIONS.put(block, new RetroStateDefinition(block, properties, null));
	}

	/** True if the block declared properties (code or data), false if it only has the implicit meta. */
	public static boolean hasExplicitDefinition(Block block) {
		return DEFINITIONS.containsKey(block);
	}

	private static RetroStateDefinition definitionOf(Block block) {
		RetroStateDefinition def = DEFINITIONS.get(block);
		if (def == null) {
			// Implicit definition: the single property "meta" (0-15), states aligned with the nibble.
			def = new RetroStateDefinition(block,
				new ArrayList<>(Arrays.asList(RetroIntProperty.of("meta", 0, 15))), null);
			DEFINITIONS.put(block, def);
		}
		return def;
	}

	/** The block's default state (first value of each property unless overridden). */
	public static RetroBlockState getDefault(Block block) {
		return definitionOf(block).defaultState;
	}

	/** The state with the given flattened index; out-of-range indices clamp to the default. */
	public static RetroBlockState fromIndex(Block block, int index) {
		RetroStateDefinition def = definitionOf(block);
		if (index < 0 || index >= def.states.length) {
			return def.defaultState;
		}
		return def.states[index];
	}

	/** Looks up a property by name, from code or data declarations. Null if unknown. */
	public static RetroProperty<?> property(Block block, String name) {
		return definitionOf(block).byName(name);
	}

	/** The number of states in the block's definition (16 for the implicit meta property). */
	public static int stateCount(Block block) {
		return definitionOf(block).states.length;
	}

	/**
	 * Reads the state at a position: vanilla meta nibble plus the sidecar's xmeta bits.
	 * Works for any BlockView; xmeta needs a real World (renderers always pass one).
	 */
	public static RetroBlockState get(BlockView world, int x, int y, int z) {
		int blockId = world.getBlockId(x, y, z);
		if (blockId <= 0 || blockId >= Block.BLOCKS.length || Block.BLOCKS[blockId] == null) {
			return null;
		}
		Block block = Block.BLOCKS[blockId];
		int index = world.getBlockMeta(x, y, z);
		ChunkExtendedBlocks extended = extendedAt(world, x, z);
		if (extended != null) {
			index |= extended.getXmeta(ChunkExtendedBlocks.toIndex(x & 15, y, z & 15)) << 4;
		}
		return fromIndex(block, index);
	}

	/**
	 * Writes a state at a position: the low 4 bits go through {@code world.setBlockMeta}
	 * (block updates, re-render, chunk dirty), the high bits into the chunk's xmeta. On a
	 * dedicated server the full index is also synced to clients over
	 * {@code retroapi:state_sync}.
	 */
	public static void set(World world, int x, int y, int z, RetroBlockState state) {
		int index = state.getIndex();
		ChunkExtendedBlocks extended = extendedAt(world, x, z);
		if (extended != null) {
			extended.setXmeta(ChunkExtendedBlocks.toIndex(x & 15, y, z & 15), (index >> 4) & 0xFF);
		}
		world.setBlockMeta(x, y, z, index & 15);

		// A state change is a block change: re-render the position (and, server-side, flag
		// it for sending) and propagate a block update to neighbors, exactly as a
		// setBlock would. setBlockMeta alone does neither reliably, and xmeta-only
		// changes never touch the nibble at all.
		world.setBlockDirty(x, y, z);
		world.notifyNeighbors(x, y, z, state.getBlock().id);

		if (!world.isRemote
			&& net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.SERVER) {
			StateSyncServer.send(world, x, y, z, index);
		}
	}

	/** Internal: applies a synced full index on the client (low bits + xmeta + re-render). */
	public static void applySynced(World world, int x, int y, int z, int index) {
		ChunkExtendedBlocks extended = extendedAt(world, x, z);
		if (extended != null) {
			extended.setXmeta(ChunkExtendedBlocks.toIndex(x & 15, y, z & 15), (index >> 4) & 0xFF);
		}
		world.setBlockMeta(x, y, z, index & 15);
		world.setBlocksDirty(x, y, z, x, y, z);
	}

	private static ChunkExtendedBlocks extendedAt(BlockView view, int x, int z) {
		// Chunk rendering passes a WorldRegion wrapper, not the World; unwrap so xmeta
		// reads keep working at render time (states past index 15 would otherwise read
		// their low nibble only and pick the wrong variant).
		World world = com.periut.retroapi.world.RetroWorlds.unwrap(view);
		if (world == null) {
			return null;
		}
		Chunk chunk = world.getChunk(x >> 4, z >> 4);
		if (chunk == null) {
			return null;
		}
		return ((ExtendedBlocksAccess) chunk).retroapi$getExtendedBlocks();
	}
}
