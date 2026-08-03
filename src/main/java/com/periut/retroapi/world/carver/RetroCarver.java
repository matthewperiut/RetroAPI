package com.periut.retroapi.world.carver;

/**
 * Shapes terrain while a chunk is still a raw block array. Register with {@link RetroCarvers}.
 *
 * <p>A carver is not a {@link com.periut.retroapi.world.feature.RetroFeature}. A feature runs during
 * <em>decoration</em>, on a chunk that already belongs to the world, and places things - a tree, an ore
 * vein, a pond. A carver runs before any of that exists and <em>removes</em> things, across chunk
 * boundaries, from an array nobody is watching. Caves have to be carvers: they are larger than a chunk,
 * they must not trigger lighting or neighbour updates a hundred thousand times per chunk, and they have
 * to be finished before the surface pass and the heightmap look at the column.
 *
 * <pre>{@code
 * RetroCarvers.register((ctx, blocks) -> {
 *     for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
 *         int top = ctx.surfaceTop(x, z);
 *         for (int y = 1; y < top; y++) {
 *             if (myDensity(ctx.getBlockX() + x, y, ctx.getBlockZ() + z, top) <= 0) ctx.carve(x, y, z);
 *         }
 *     }
 * }).dimension(0).register();
 * }</pre>
 *
 * <p>Must be deterministic in the world seed and the chunk coordinates and nothing else: neighbouring
 * chunks are generated in an unpredictable order and at unpredictable times, so a carver that depends on
 * anything else produces seams.
 */
@FunctionalInterface
public interface RetroCarver {

	/**
	 * Carve one chunk. {@code blocks} is the same array as {@link RetroCarverContext#getBlocks()},
	 * passed separately because it is what nearly every implementation touches first.
	 */
	void carve(RetroCarverContext context, byte[] blocks);
}
