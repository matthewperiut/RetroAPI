package com.periut.retroapi.world.feature;

import com.periut.retroapi.RetroAPI;
import net.minecraft.block.Block;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * World-generation features you can register instead of mixin into a chunk generator.
 *
 * <p>In beta, what generates in a chunk is a hardcoded list inside each generator's {@code decorate},
 * with ore veins created as {@code new OreFeature(id, size)} and their height bands written as literals.
 * Adding anything - even one ore - meant mixin-ing vanilla generation. This registry hooks decoration
 * once, for every dimension and every generator (including custom ones), and runs whatever mods put in
 * it, after vanilla's own decoration for that chunk.
 *
 * <pre>
 * // in your `retroapi` entrypoint
 * RetroFeatures.ore(MyMod.RUBY_ORE).size(6).count(8).heightRange(4, 32).register();
 * RetroFeatures.ore(MyMod.MARBLE).meta(1).size(40).count(2).rarity(3).heightRange(0, 96).register();
 * RetroFeatures.cluster(Block.GRAVEL, 32).count(4).heightRange(48, 96).replace(Block.STONE).register();
 *
 * RetroFeatures.custom((world, random, x, y, z) -&gt; {
 *     // your own structure; return true if you placed something
 *     return true;
 * }).count(1).rarity(64).heightRange(60, 80).register();
 * </pre>
 *
 * <p>Generation is deterministic: the decoration {@link Random} is seeded from the world seed and chunk
 * coordinates exactly the way vanilla seeds its own, so the same seed gives the same world.
 */
public final class RetroFeatures {

	private static final List<RetroPlacedFeature> FEATURES = new ArrayList<>();

	private RetroFeatures() {}

	// --- building ---------------------------------------------------------------------------------

	/** An ore-shaped vein of a block. Configure the vein on the feature, the placement on the result. */
	public static RetroPlacedFeature ore(Block block) {
		return ore(block.id);
	}

	/** An ore-shaped vein of a raw block id. */
	public static RetroPlacedFeature ore(int blockId) {
		return new RetroPlacedFeature(new RetroOreFeature(blockId));
	}

	/** Sugar for {@link #ore(Block)} with a vein size, matching vanilla's {@code new OreFeature(id, size)}. */
	public static RetroPlacedFeature cluster(Block block, int size) {
		return ore(block).size(size);
	}

	/** Any feature of your own. */
	public static RetroPlacedFeature custom(RetroFeature feature) {
		return new RetroPlacedFeature(feature);
	}

	static void add(RetroPlacedFeature feature) {
		FEATURES.add(feature);
		Block block = feature.describeBlock();
		RetroAPI.LOGGER.debug("Registered world feature{} ({} per chunk, y {}-{}{})",
			block != null ? " for " + block.getTranslationKey() : "",
			feature.count, feature.minY, feature.maxY,
			feature.rarity > 1 ? ", 1 chunk in " + feature.rarity : "");
	}

	/** Every registered feature, in registration order. */
	public static List<RetroPlacedFeature> getAll() {
		return java.util.Collections.unmodifiableList(FEATURES);
	}

	// --- generation -------------------------------------------------------------------------------

	/**
	 * Runs every registered feature for one chunk. Called by RetroAPI's decoration hook after vanilla's
	 * own decoration; a custom chunk generator that does its own thing can call this directly.
	 */
	public static void decorate(World world, int chunkX, int chunkZ) {
		if (FEATURES.isEmpty() || world == null) {
			return;
		}
		// Vanilla's per-chunk decoration seeding, so features are stable for a world seed.
		Random random = new Random(world.getSeed());
		long xSeed = random.nextLong() / 2L * 2L + 1L;
		long zSeed = random.nextLong() / 2L * 2L + 1L;
		random.setSeed((long) chunkX * xSeed + (long) chunkZ * zSeed ^ world.getSeed());

		int blockX = chunkX * 16;
		int blockZ = chunkZ * 16;
		for (int i = 0; i < FEATURES.size(); i++) {
			RetroPlacedFeature feature = FEATURES.get(i);
			if (!feature.appliesTo(world)) continue;
			try {
				feature.generate(world, random, blockX, blockZ);
			} catch (RuntimeException e) {
				// One broken feature must not take the whole chunk (and with it the world) down.
				RetroAPI.LOGGER.error("Feature failed while decorating chunk {},{}", chunkX, chunkZ, e);
			}
		}
	}

	/**
	 * Generation-safe block placement: sets id + metadata without notifying neighbors, so nothing
	 * cascades into re-generating the chunk you are still inside. Use this from features instead of
	 * {@code world.setBlock}.
	 */
	public static void setBlock(World world, int x, int y, int z, int blockId, int meta) {
		if (y < 0 || y > 127) {
			return;
		}
		world.setBlockWithoutNotifyingNeighbors(x, y, z, blockId, meta);
	}

	/**
	 * Generation-safe placement of a full block STATE, for blocks with more than 16 states. The
	 * {@code meta} overload above can only carry the vanilla nibble, so a state index past 15 would be
	 * truncated - this writes the sidecar bits too, still without notifying neighbors.
	 *
	 * <pre>
	 * RetroFeatures.custom((world, random, x, y, z) -&gt; {
	 *     RetroFeatures.setBlock(world, x, y, z,
	 *         RetroStates.getDefault(MyMod.PILLAR).with(MyMod.AGE, 7));
	 *     return true;
	 * }).count(1).register();
	 * </pre>
	 */
	public static void setBlock(World world, int x, int y, int z,
			com.periut.retroapi.state.RetroBlockState state) {
		com.periut.retroapi.state.RetroStates.placeWithoutNotifyingNeighbors(world, x, y, z, state);
	}

	/** The topmost solid block Y at a column, for surface-placed features. */
	public static int surfaceY(World world, int x, int z) {
		return world.getTopY(x, z);
	}
}
