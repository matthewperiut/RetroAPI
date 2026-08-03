package com.periut.retroapi.biome.cubic;

import com.periut.retroapi.storage.ChunkExtendedBlocks;
import com.periut.retroapi.storage.ExtendedBlocksAccess;
import com.periut.retroapi.world.RetroWorlds;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.ornithemc.osl.core.api.util.NamespacedIdentifier;
import net.ornithemc.osl.core.api.util.NamespacedIdentifiers;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Biomes on a <b>cubic grid</b>, stored with the world rather than derived from the surface.
 *
 * <p>Beta has exactly one notion of "where am I": {@code Biome.getBiome(temperature, rainfall)}, a pure
 * function of two 2D noises. Everything at a given X/Z is in the same biome from bedrock to sky, and the
 * answer is recomputed from the seed every time rather than stored. That is fine for a surface, and it
 * cannot express a cave: a mushroom cavern under a desert is not a property of the desert, two caverns
 * a hundred blocks apart vertically are not the same place, and a cavern a player has walked through has
 * an identity that must survive being unloaded.
 *
 * <p>So this is the other thing: a sparse grid of 16x16x16 cells, each either <b>unassigned</b> or
 * holding a {@link RetroCubicBiome}, written into RetroAPI's sidecar and synced to clients.
 *
 * <h2>Unassigned is the default, and it costs nothing</h2>
 * Every cell starts with no value. {@link #get} returns {@code null} there - not a fallback biome, not
 * a "plains" - because "this volume has no cave biome" is the correct answer for the overwhelming
 * majority of the world, and code that has to distinguish "unassigned" from "assigned to the default"
 * cannot if the API lies about it. A chunk whose cells are all unassigned has <b>no cbio section written
 * at all</b>, so its sidecar file is byte-identical to one from a world that never had this API.
 *
 * <h2>Cells</h2>
 * A chunk is 16 wide and 16 deep, so a 16-cube cell is addressed by its vertical index alone
 * ({@code cellY = y >> 4}, negative below y0 when
 * {@link com.periut.retroapi.world.height.RetroWorldHeight} extends the world downward). Cell
 * granularity is coarse on purpose. Blocky boundaries are not solved by smaller cells - they are solved
 * by assigning from noise whose features are hundreds of blocks across, so that neighbouring cells
 * almost always agree and a boundary is rare enough for an irregular cave wall to hide.
 *
 * <pre>{@code
 * // once, in common init on both sides
 * public static final RetroCubicBiome MUSHROOM = RetroCubicBiomes.register(id("mushroom"));
 *
 * // while generating, before the chunk belongs to the world
 * RetroCubicBiomes.setCell(chunk, cellY, MUSHROOM);
 *
 * // at runtime
 * RetroCubicBiome here = RetroCubicBiomes.get(world, x, y, z);
 * if (here == MUSHROOM) { ... }
 * }</pre>
 *
 * <h2>Sides</h2>
 * Register on both sides (a static field in common init is enough - registration touches no world
 * state). Writes want a server-side or singleplayer world and are pushed to the clients that can see
 * them; reads work anywhere, including from the chunk-render thread's {@code WorldRegion} view, because
 * fog and ambience are exactly the callers that need this.
 */
public final class RetroCubicBiomes {

	private static final Map<String, RetroCubicBiome> BY_ID = new LinkedHashMap<>();
	private static final Map<Integer, RetroCubicBiome> BY_RUNTIME_ID = new LinkedHashMap<>();
	/** 0 is reserved for "unassigned", so allocation starts at 1. */
	private static int nextRuntimeId = 1;

	private RetroCubicBiomes() {}

	// --- registration -------------------------------------------------------------------------

	/**
	 * Registers a cubic biome, or returns the existing one for this id. Idempotent, so calling it from
	 * both a common and a client init is safe.
	 */
	public static synchronized RetroCubicBiome register(NamespacedIdentifier id) {
		String key = id.toString();
		RetroCubicBiome existing = BY_ID.get(key);
		if (existing != null) {
			return existing;
		}
		RetroCubicBiome biome = new RetroCubicBiome(id, nextRuntimeId++);
		BY_ID.put(key, biome);
		BY_RUNTIME_ID.put(biome.getRuntimeId(), biome);
		return biome;
	}

	/** The biome registered under an id, or null. */
	public static RetroCubicBiome byId(NamespacedIdentifier id) {
		return BY_ID.get(id.toString());
	}

	/** The biome registered under an id string ({@code "cavebiomes:mushroom"}), or null. */
	public static RetroCubicBiome byId(String id) {
		return BY_ID.get(id);
	}

	/** The biome for a per-session runtime id, or null. Used by the sidecar and the sync packet. */
	public static RetroCubicBiome byRuntimeId(int runtimeId) {
		return BY_RUNTIME_ID.get(runtimeId);
	}

	/** Every registered cubic biome, in registration order. */
	public static Collection<RetroCubicBiome> all() {
		return java.util.Collections.unmodifiableCollection(BY_ID.values());
	}

	/** Convenience for mods that want their own namespace shorthand. */
	public static NamespacedIdentifier id(String namespace, String path) {
		return NamespacedIdentifiers.from(namespace, path);
	}

	// --- cell addressing ----------------------------------------------------------------------

	/** The cell index a world Y falls in. Floor division, so it is correct for negative Y. */
	public static int cellY(int y) {
		return y >> 4;
	}

	/** The lowest world Y inside a cell. */
	public static int cellBottomY(int cellY) {
		return cellY << 4;
	}

	// --- reading ------------------------------------------------------------------------------

	/**
	 * The cubic biome at a block position, or {@code null} when the containing cell is unassigned.
	 * Safe on any {@link BlockView}, including the render thread's region view.
	 */
	public static RetroCubicBiome get(BlockView view, int x, int y, int z) {
		ChunkExtendedBlocks extended = extendedAt(view, x, z);
		if (extended == null) {
			return null;
		}
		return byRuntimeId(extended.getCubicBiome(cellY(y)));
	}

	/** The cubic biome of one cell, or {@code null} when it is unassigned. */
	public static RetroCubicBiome getCell(BlockView view, int chunkX, int cellY, int chunkZ) {
		ChunkExtendedBlocks extended = extendedAt(view, chunkX << 4, chunkZ << 4);
		if (extended == null) {
			return null;
		}
		return byRuntimeId(extended.getCubicBiome(cellY));
	}

	/** True when the cell containing this position has been assigned anything. */
	public static boolean has(BlockView view, int x, int y, int z) {
		ChunkExtendedBlocks extended = extendedAt(view, x, z);
		return extended != null && extended.hasCubicBiome(cellY(y));
	}

	// --- writing ------------------------------------------------------------------------------

	/**
	 * Assigns the cell containing a block position. Pass {@code null} to clear it. On a dedicated
	 * server the change is sent to the players tracking the chunk; in singleplayer the client already
	 * shares this world.
	 */
	public static void set(World world, int x, int y, int z, RetroCubicBiome biome) {
		setCell(world, x >> 4, cellY(y), z >> 4, biome);
	}

	/** Assigns one cell by cell coordinates. Pass {@code null} to clear it. */
	public static void setCell(World world, int chunkX, int cellY, int chunkZ, RetroCubicBiome biome) {
		Chunk chunk = world.getChunk(chunkX, chunkZ);
		if (chunk == null) {
			return;
		}
		ChunkExtendedBlocks extended = ((ExtendedBlocksAccess) chunk).retroapi$getExtendedBlocks();
		int runtimeId = biome == null ? 0 : biome.getRuntimeId();
		if (extended.getCubicBiome(cellY) == runtimeId && extended.hasCubicBiome(cellY) == (runtimeId != 0)) {
			return;
		}
		extended.setCubicBiome(cellY, runtimeId);
		chunk.dirty = true;

		if (!world.isRemote
			&& net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.SERVER) {
			CubicBiomeSyncServer.sendCell(world, chunkX, cellY, chunkZ, biome);
		}
	}

	/**
	 * Assigns a cell on a chunk that is not in the world yet - the generation-time entry point. A
	 * generator holds its chunk before {@code ChunkSource.getChunk} returns, so there is no world to
	 * look it up in and nothing to sync to; the assignment simply rides the chunk into storage.
	 */
	public static void setCell(Chunk chunk, int cellY, RetroCubicBiome biome) {
		if (chunk == null) {
			return;
		}
		((ExtendedBlocksAccess) chunk).retroapi$getExtendedBlocks()
			.setCubicBiome(cellY, biome == null ? 0 : biome.getRuntimeId());
	}

	/** Reads a cell straight off a chunk, for generation-time code that has no world. */
	public static RetroCubicBiome getCell(Chunk chunk, int cellY) {
		if (chunk == null) {
			return null;
		}
		return byRuntimeId(((ExtendedBlocksAccess) chunk).retroapi$getExtendedBlocks().getCubicBiome(cellY));
	}

	/** Internal: applies a cell received from the server, without echoing it back. */
	public static void applySynced(World world, int chunkX, int cellY, int chunkZ, String biomeId) {
		Chunk chunk = world.getChunk(chunkX, chunkZ);
		if (chunk == null) {
			return;
		}
		RetroCubicBiome biome = biomeId == null || biomeId.isEmpty() ? null : byId(biomeId);
		((ExtendedBlocksAccess) chunk).retroapi$getExtendedBlocks()
			.setCubicBiome(cellY, biome == null ? 0 : biome.getRuntimeId());
	}

	private static ChunkExtendedBlocks extendedAt(BlockView view, int x, int z) {
		// Chunk rendering hands the renderer a WorldRegion wrapper rather than the World, and ambience
		// is exactly the caller that reads from there - unwrap so those reads keep working (the same
		// reason RetroStates and RetroBlockData unwrap).
		World world = RetroWorlds.unwrap(view);
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
