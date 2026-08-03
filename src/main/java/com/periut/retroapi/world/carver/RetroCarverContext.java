package com.periut.retroapi.world.carver;

import net.minecraft.block.Block;
import net.minecraft.world.World;

/**
 * What a {@link RetroCarver} gets handed: the chunk being generated, its raw block array, and the few
 * derived facts every carver needs.
 *
 * <p>The block array is the whole point of carving at this stage. It is the chunk before it is a chunk -
 * no lighting, no heightmap, no block entities, nothing registered with the world - so writing to it is
 * a plain array store with no side effects, no neighbour notifications, and no risk of recursively
 * re-entering generation. The same edit made later through {@code world.setBlock} costs light updates
 * and re-renders per block.
 *
 * <p>Coordinates are <b>chunk-local</b>: x and z in 0-15, y in 0-127.
 */
public final class RetroCarverContext {

	/** b1.7.3 chunk layout: {@code x << 11 | z << 7 | y}, 16x128x16. */
	public static final int CHUNK_HEIGHT = 128;

	private final World world;
	private final byte[] blocks;
	private final int chunkX;
	private final int chunkZ;
	private final long seed;
	private final int bottomY;
	private final int topY;

	/** Lazily computed topmost solid (non-air, non-fluid) Y per column, indexed {@code x << 4 | z}. */
	private int[] surfaceTops;

	public RetroCarverContext(World world, byte[] blocks, int chunkX, int chunkZ, long seed,
			int bottomY, int topY) {
		this.world = world;
		this.blocks = blocks;
		this.chunkX = chunkX;
		this.chunkZ = chunkZ;
		this.seed = seed;
		this.bottomY = bottomY;
		this.topY = topY;
	}

	public World getWorld() {
		return world;
	}

	/** The chunk's raw block array. Writes here are free; see the class doc. */
	public byte[] getBlocks() {
		return blocks;
	}

	public int getChunkX() {
		return chunkX;
	}

	public int getChunkZ() {
		return chunkZ;
	}

	/** The world seed, for deriving deterministic noise and per-chunk randomness. */
	public long getSeed() {
		return seed;
	}

	/**
	 * The lowest Y the dimension has, from
	 * {@link com.periut.retroapi.world.height.RetroWorldHeight}. Zero unless the dimension is extended
	 * downward. Carvers use it to scale depth-dependent rules instead of hardcoding 0.
	 */
	public int getBottomY() {
		return bottomY;
	}

	/** The highest Y the dimension has. 127 unless the dimension is extended upward. */
	public int getTopY() {
		return topY;
	}

	public int getBlockX() {
		return chunkX << 4;
	}

	public int getBlockZ() {
		return chunkZ << 4;
	}

	// --- raw array access ---------------------------------------------------------------------

	public static int index(int x, int y, int z) {
		return x << 11 | z << 7 | y;
	}

	/** The block id at a chunk-local position, or 0 (air) outside the array. */
	public int getBlock(int x, int y, int z) {
		if (y < 0 || y >= CHUNK_HEIGHT || x < 0 || x > 15 || z < 0 || z > 15) {
			return 0;
		}
		return blocks[index(x, y, z)] & 0xFF;
	}

	/** Sets a vanilla (&lt;256) block id at a chunk-local position. Out-of-range writes are dropped. */
	public void setBlock(int x, int y, int z, int blockId) {
		if (y < 0 || y >= CHUNK_HEIGHT || x < 0 || x > 15 || z < 0 || z > 15) {
			return;
		}
		blocks[index(x, y, z)] = (byte) blockId;
	}

	/** Sets a position to air. */
	public void carve(int x, int y, int z) {
		setBlock(x, y, z, 0);
	}

	// --- derived facts -------------------------------------------------------------------------

	/**
	 * The topmost <b>solid</b> block Y in a column - air and fluids do not count.
	 *
	 * <p>This, and specifically not the heightmap or the sea level, is what a cave carver has to measure
	 * depth against. Beta's heightmap counts water (it tracks light opacity), so under an ocean it
	 * reports the water surface, and a carver that trusted it would happily open a hole in the sea floor
	 * and drain the ocean into the caves. Measuring from the solid top protects land and sea floor with
	 * the same rule.
	 *
	 * @return the Y of the highest solid block, or -1 for a column with none
	 */
	public int surfaceTop(int x, int z) {
		if (surfaceTops == null) {
			computeSurfaceTops();
		}
		if (x < 0 || x > 15 || z < 0 || z > 15) {
			return -1;
		}
		return surfaceTops[x << 4 | z];
	}

	private void computeSurfaceTops() {
		surfaceTops = new int[256];
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				int top = -1;
				for (int y = CHUNK_HEIGHT - 1; y >= 0; y--) {
					int id = blocks[index(x, y, z)] & 0xFF;
					if (id != 0 && !isFluid(id)) {
						top = y;
						break;
					}
				}
				surfaceTops[x << 4 | z] = top;
			}
		}
	}

	private static boolean isFluid(int blockId) {
		return blockId == Block.FLOWING_WATER.id || blockId == Block.WATER.id
			|| blockId == Block.FLOWING_LAVA.id || blockId == Block.LAVA.id;
	}
}
