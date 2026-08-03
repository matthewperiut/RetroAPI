package com.periut.retroapi.storage;


import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.ornithemc.osl.core.api.util.NamespacedIdentifier;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Auxiliary data attached to a block <em>position</em>: one 32-bit value per position per registered
 * {@link RetroBlockDataType}, persisted in the region sidecar, carried on the chunk packet, and pushed
 * to clients when it changes.
 *
 * <p>This is the third and last storage tier RetroAPI offers a block, and it exists because the first
 * two run out:
 *
 * <ul>
 *   <li><b>State</b> ({@link com.periut.retroapi.state.RetroStates}) is 12 bits, total, for every
 *       property a block has. That is the right home for a stair's facing and half - and it cannot also
 *       hold "which block is this stair pretending to be", because a block reference alone needs 16.</li>
 *   <li><b>A block entity</b> holds anything, but b1.7.3 walks every loaded block entity every tick and
 *       range-checks it against the chunk map before calling {@code tick()}. That is fine for the dozens
 *       of chests a world has and ruinous for the tens of thousands of blocks a decoration mod places:
 *       the cost is paid per block per tick, forever, for data that changes when a player right-clicks.</li>
 * </ul>
 *
 * <p>So: sparse maps on the chunk, one entry per position that actually carries data, no per-tick cost
 * at all, and the same save/sync plumbing the extended block ids already ride.
 *
 * <pre>{@code
 * // once, at registration time (both sides)
 * public static final RetroBlockDataType CAMO = RetroBlockData.registerBlockRef(id("camo"));
 *
 * // when the player clads the block
 * RetroBlockData.set(world, x, y, z, CAMO, RetroBlockData.encodeBlockRef(Block.GLASS.id, 0));
 *
 * // at render time
 * int camo = RetroBlockData.get(world, x, y, z, CAMO);
 * int blockId = RetroBlockData.blockRefId(camo);
 * }</pre>
 *
 * <h2>Lifetime</h2>
 * A position's data is dropped when the block at that position changes, so a value can never be
 * inherited by whatever is placed there next. Reading a position that has no value gives {@code 0},
 * which is therefore the "unset" value for every type - use {@link #has} when zero is meaningful.
 *
 * <h2>Sides</h2>
 * Register the type on both sides (a plain static field in common init is enough - the call touches no
 * world state). {@link #set} must be called with a server-side (or singleplayer) world; on a dedicated
 * server it also sends the change to every player tracking the position. Reads work anywhere, including
 * from the chunk-render thread's {@code WorldRegion} view.
 */
public final class RetroBlockData {

	private static final Map<String, RetroBlockDataType> TYPES = new LinkedHashMap<>();

	private RetroBlockData() {}

	// --- registration -------------------------------------------------------------------------

	/**
	 * Registers a raw 32-bit data type. The value is stored verbatim; it means whatever the mod says.
	 * Use {@link #registerBlockRef} instead for anything that identifies a block, so it survives a
	 * change in the installed mod set.
	 */
	public static RetroBlockDataType register(NamespacedIdentifier id) {
		return register(id, false);
	}

	/**
	 * Registers a block-reference data type: values are {@code blockId | meta << 12}
	 * ({@link #encodeBlockRef}), saved through a per-chunk string-id palette so a stored reference
	 * still names the same block after mods are added, removed or reordered.
	 */
	public static RetroBlockDataType registerBlockRef(NamespacedIdentifier id) {
		return register(id, true);
	}

	private static synchronized RetroBlockDataType register(NamespacedIdentifier id, boolean blockRef) {
		String key = id.toString();
		RetroBlockDataType existing = TYPES.get(key);
		if (existing != null) {
			if (existing.isBlockRef() != blockRef) {
				throw new IllegalArgumentException("Block data type " + key
					+ " is already registered as " + (existing.isBlockRef() ? "a block ref" : "raw")
					+ "; the two forms are stored differently and cannot share a key");
			}
			return existing;
		}
		RetroBlockDataType type = new RetroBlockDataType(id, blockRef);
		TYPES.put(key, type);
		return type;
	}

	/** The registered type for a storage key, or null. Used by the sidecar to know how to save a section. */
	public static RetroBlockDataType byKey(String key) {
		return TYPES.get(key);
	}

	/** Every registered type, in registration order. */
	public static Collection<RetroBlockDataType> types() {
		return java.util.Collections.unmodifiableCollection(TYPES.values());
	}

	// --- block references ---------------------------------------------------------------------

	/** Packs a block id (0-4095) and metadata (0-15) into one block-reference value. */
	public static int encodeBlockRef(int blockId, int meta) {
		return (blockId & 0xFFF) | ((meta & 0xF) << 12);
	}

	/** The block id half of a block-reference value. */
	public static int blockRefId(int value) {
		return value & 0xFFF;
	}

	/** The metadata half of a block-reference value. */
	public static int blockRefMeta(int value) {
		return (value >> 12) & 0xF;
	}

	// --- read / write -------------------------------------------------------------------------

	/** The value at a position, or {@code 0} when none is stored. Safe on any {@link BlockView}. */
	public static int get(BlockView world, int x, int y, int z, RetroBlockDataType type) {
		ChunkExtendedBlocks extended = extendedAt(world, x, z);
		if (extended == null) {
			return 0;
		}
		return extended.getData(type.getKey(), ChunkExtendedBlocks.toIndex(x & 15, y, z & 15));
	}

	/** True when a value is actually stored here, as opposed to {@link #get} returning the default 0. */
	public static boolean has(BlockView world, int x, int y, int z, RetroBlockDataType type) {
		ChunkExtendedBlocks extended = extendedAt(world, x, z);
		if (extended == null) {
			return false;
		}
		return extended.hasData(type.getKey(), ChunkExtendedBlocks.toIndex(x & 15, y, z & 15));
	}

	/**
	 * Stores a value at a position and re-renders it. Passing {@code 0} clears the entry (see
	 * {@link #clear}). On a dedicated server the change is also sent to the players tracking the
	 * position; in singleplayer the client already holds the chunk this wrote to.
	 */
	public static void set(World world, int x, int y, int z, RetroBlockDataType type, int value) {
		ChunkExtendedBlocks extended = extendedAt(world, x, z);
		if (extended == null) {
			return;
		}
		extended.setData(type.getKey(), ChunkExtendedBlocks.toIndex(x & 15, y, z & 15), value);
		Chunk chunk = world.getChunk(x >> 4, z >> 4);
		if (chunk != null) {
			chunk.dirty = true;
		}
		world.setBlockDirty(x, y, z);

		if (!world.isRemote
			&& net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.SERVER) {
			BlockDataSyncServer.send(world, x, y, z, type, value);
		}
	}

	/** Removes the value at a position. Equivalent to {@code set(..., 0)}. */
	public static void clear(World world, int x, int y, int z, RetroBlockDataType type) {
		set(world, x, y, z, type, 0);
	}

	/** Internal: applies a value received from the server, without echoing it back. */
	public static void applySynced(World world, int x, int y, int z, String key, int value) {
		ChunkExtendedBlocks extended = extendedAt(world, x, z);
		if (extended == null) {
			return;
		}
		extended.setData(key, ChunkExtendedBlocks.toIndex(x & 15, y, z & 15), value);
		world.setBlocksDirty(x, y, z, x, y, z);
	}

	private static ChunkExtendedBlocks extendedAt(BlockView view, int x, int z) {
		// Chunk rendering hands the renderer a WorldRegion wrapper rather than the World, and a
		// block's own renderer is exactly the caller that needs this data - unwrap so reads keep
		// working there (see RetroStates, which unwraps for the same reason).
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
