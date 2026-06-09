package com.periut.retroapi.world;

import com.periut.retroapi.storage.ExtendedBlocksAccess;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

/**
 * Generation-safe chunk creation + block placement for custom chunk generators, working in whichever block
 * storage model is active.
 *
 * <p><b>Without StationAPI</b> RetroAPI keeps the vanilla model: a legacy {@link Chunk} with a {@code byte[]}
 * for ids &lt;256 and a per-chunk overlay for modded ids &ge;256. <b>With StationAPI</b> the world is flattened,
 * so a generator must produce a {@code FlattenedChunk} with blocks as {@code BlockState}s - StationAPI patches
 * the vanilla generators to do this, but a custom generator (e.g. an Aether {@code ChunkProvider}) isn't
 * patched and would produce an unsavable legacy chunk. Route a custom generator through this class and it
 * works either way, unchanged:
 *
 * <pre>{@code
 * Chunk chunk = RetroWorldGen.createChunk(world, vanillaBlocks, chunkX, chunkZ); // <256 in the byte[]
 * RetroWorldGen.setBlockInChunk(chunk, x, y, z, AetherBlocks.Holystone.id, 0);   // any id, incl. >=256
 * }</pre>
 *
 * <p>During {@code ChunkSource.getChunk} the chunk isn't registered with the world yet, so neither entry point
 * fires {@code Block.onPlaced} (which could touch neighbours and recursively re-generate this chunk).
 *
 * <p>The storage model is chosen ONCE at class load into a {@code static final} {@link ChunkGenBackend} (the
 * JIT devirtualizes + inlines it), so the per-chunk generation calls carry no StationAPI branch - mirroring
 * the rendering atlas strategy.
 */
public final class RetroWorldGen {

	private static final ChunkGenBackend IMPL = chooseBackend();

	private RetroWorldGen() {}

	/**
	 * Create a freshly generated chunk in the active storage model. Pass the chunk-layout {@code byte[]} of
	 * vanilla (&lt;256) ids; modded (&ge;256) ids - or any block you'd rather place individually - go through
	 * {@link #setBlockInChunk} afterward. Returns a {@code FlattenedChunk} under StationAPI, a legacy
	 * {@code Chunk} otherwise.
	 */
	public static Chunk createChunk(World world, byte[] vanillaBlocks, int chunkX, int chunkZ) {
		return IMPL.createChunk(world, vanillaBlocks, chunkX, chunkZ);
	}

	/**
	 * Set a block - ANY id, vanilla (&lt;256) or modded (&ge;256) - in a chunk from {@link #createChunk}, with
	 * no placement side effects. Coordinates are chunk-local (0-15, 0-127, 0-15).
	 *
	 * <ul>
	 *   <li>Under StationAPI: writes the block's default {@code BlockState} into the FlattenedChunk section.</li>
	 *   <li>Otherwise: vanilla ids go to the chunk's raw block array (+ metadata nibble); modded ids go to
	 *       RetroAPI's per-chunk extended-block overlay.</li>
	 * </ul>
	 */
	public static void setBlockInChunk(Chunk chunk, int x, int y, int z, int blockId, int meta) {
		IMPL.setBlock(chunk, x, y, z, blockId, meta);
	}

	/**
	 * The StationAPI backend lives in the optional {@code retroapi-stationapi} mod and is loaded reflectively
	 * by class name, so core carries no StationAPI reference. This runs once at class load; when StationAPI is
	 * absent (or the bridge mod is missing), the legacy vanilla backend is used.
	 */
	private static ChunkGenBackend chooseBackend() {
		if (FabricLoader.getInstance().isModLoaded("stationapi")) {
			try {
				return (ChunkGenBackend) Class.forName("com.periut.retroapi.stationapi.StationChunkGen")
					.getDeclaredConstructor().newInstance();
			} catch (ReflectiveOperationException | LinkageError e) {
				throw new IllegalStateException("StationAPI present but RetroAPI StationAPI chunk backend failed to load", e);
			}
		}
		return new Legacy();
	}

	/** RetroAPI's vanilla storage model: legacy {@link Chunk} byte array (+ metadata) for ids &lt;256, the
	 *  per-chunk extended-block overlay for ids &ge;256. */
	private static final class Legacy implements ChunkGenBackend {
		@Override
		public Chunk createChunk(World world, byte[] vanillaBlocks, int chunkX, int chunkZ) {
			return new Chunk(world, vanillaBlocks, chunkX, chunkZ);
		}

		@Override
		public void setBlock(Chunk chunk, int x, int y, int z, int blockId, int meta) {
			if (blockId >= 256) {
				int index = (x * 16 + z) * 128 + y;
				((ExtendedBlocksAccess) chunk).retroapi$getExtendedBlocks().set(index, blockId, meta);
			} else {
				chunk.blocks[(x * 16 + z) * 128 + y] = (byte) blockId;
				if (meta != 0) {
					chunk.setBlockMeta(x, y, z, meta);
				}
			}
		}
	}
}
