package com.periut.retroapi.world;

import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

/**
 * The block-storage strategy behind {@link RetroWorldGen}: how a custom chunk generator builds a chunk and
 * places blocks in it. Chosen ONCE at class load (RetroAPI's vanilla legacy model, or StationAPI's flattened
 * model) and held in a {@code static final} field, so the generation calls are a single devirtualized +
 * inlined call with no StationAPI branch - mirroring the rendering atlas strategy. Being an interface also
 * keeps the StationAPI-referencing implementation off the verifier's radar until it is actually instantiated.
 */
public interface ChunkGenBackend {
	Chunk createChunk(World world, byte[] vanillaBlocks, int chunkX, int chunkZ);

	void setBlock(Chunk chunk, int x, int y, int z, int blockId, int meta);
}
