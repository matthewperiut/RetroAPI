package com.periut.retroapi.world.feature;

import net.minecraft.world.World;

import java.util.Random;

/**
 * One thing that gets placed while a chunk is decorated: an ore vein, a boulder, a hut, a patch of
 * something. Implement this directly for anything the built-ins do not cover, or pass a lambda to
 * {@code RetroFeatures.custom(...)}.
 *
 * <p>The signature matches vanilla's own {@code Feature.generate}, so porting an existing generator is a
 * copy/paste. The position is one attempt rolled by the feature's placement (see
 * {@link RetroPlacedFeature}: count per chunk, height band, rarity), and the {@link Random} is seeded per
 * chunk, so generation stays deterministic for a world seed.
 */
@FunctionalInterface
public interface RetroFeature {

	/**
	 * Places one instance.
	 *
	 * @param world  the world being generated; write blocks with {@link RetroFeatures#setBlock} so
	 *               placement does not trigger block updates mid-generation
	 * @param random the chunk's decoration random
	 * @param x      block X of this attempt
	 * @param y      block Y of this attempt (rolled inside the placement's height band)
	 * @param z      block Z of this attempt
	 * @return true if anything was placed (informational; generation continues either way)
	 */
	boolean generate(World world, Random random, int x, int y, int z);
}
