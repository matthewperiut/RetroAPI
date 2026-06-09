package com.periut.retroapi.stationapi;

import com.periut.retroapi.world.ChunkGenBackend;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.modificationstation.stationapi.api.block.BlockStateHolder;
import net.modificationstation.stationapi.impl.world.chunk.ChunkSection;
import net.modificationstation.stationapi.impl.world.chunk.FlattenedChunk;

/**
 * StationAPI-flattened {@link ChunkGenBackend}. Under StationAPI a custom generator's chunk must be a
 * {@link FlattenedChunk} (blocks as {@code BlockState}s in sections) or StationAPI's chunk storage refuses to
 * save it. This builds one and writes blocks straight into its sections - the same generation-safe path
 * {@code FlattenedChunk.fromLegacy} uses (no {@code Block.onPlaced} side effects).
 *
 * <p>Only instantiated by {@link RetroWorldGen} when StationAPI is loaded, so this class and its StationAPI
 * imports never load otherwise.
 */
public final class StationChunkGen implements ChunkGenBackend {

	/** A FlattenedChunk with the vanilla (&lt;256) block array converted into flattened sections. */
	@Override
	public Chunk createChunk(World world, byte[] vanillaBlocks, int chunkX, int chunkZ) {
		FlattenedChunk chunk = new FlattenedChunk(world, chunkX, chunkZ);
		chunk.fromLegacy(vanillaBlocks);
		return chunk;
	}

	/** Write a block's default state into the flattened section at the chunk-local coordinate. */
	@Override
	public void setBlock(Chunk chunk, int x, int y, int z, int blockId, int meta) {
		if (!(chunk instanceof FlattenedChunk flattened) || blockId <= 0 || blockId >= Block.BLOCKS.length) {
			return;
		}
		Block block = Block.BLOCKS[blockId];
		if (block == null) return;
		// fromLegacy maps a local column [0,height) onto world Y [bottomY, bottomY+height); match it so the
		// modded (>=256) blocks land in the same sections as the vanilla (<256) ones. firstBlock == bottomY.
		int worldY = y + flattened.firstBlock;
		ChunkSection section = flattened.getOrCreateSection(worldY, false);
		if (section != null) {
			section.setBlockState(x, worldY & 15, z, ((BlockStateHolder) block).getDefaultState());
		}
	}
}
