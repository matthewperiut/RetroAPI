package com.periut.retroapi.world.feature;

import net.minecraft.block.Block;
import net.minecraft.world.World;

import java.util.Random;

/**
 * A feature plus where, how often and in which dimensions it is placed - the "placement" half that beta
 * hardcodes inside each chunk generator, and that a mod could previously only reach with a mixin.
 *
 * <p>Build one with {@link RetroFeatures#ore}, {@link RetroFeatures#cluster} or
 * {@link RetroFeatures#custom}, then {@link #register()}:
 *
 * <pre>
 * RetroFeatures.ore(MyMod.RUBY_ORE).size(6).count(8).heightRange(4, 32).register();
 *
 * // a rarer vein, in a specific stone type, holding a specific state
 * RetroFeatures.ore(MyMod.MARBLE).size(40).count(2).heightRange(0, 96)
 *     .replace(Block.STONE).meta(2).rarity(4).register();
 * </pre>
 */
public final class RetroPlacedFeature {

	/** Dimension filter meaning "every dimension". */
	public static final int ANY_DIMENSION = Integer.MIN_VALUE;

	final RetroFeature feature;
	int count = 1;
	int minY = 0;
	int maxY = 127;
	int rarity = 1;
	int[] dimensions = {0};

	RetroPlacedFeature(RetroFeature feature) {
		this.feature = feature;
	}

	/** How many attempts per chunk (default 1). Each attempt picks its own random position. */
	public RetroPlacedFeature count(int count) {
		this.count = Math.max(0, count);
		return this;
	}

	/** The inclusive Y band attempts are rolled in (default the whole world, 0-127). */
	public RetroPlacedFeature heightRange(int minY, int maxY) {
		this.minY = Math.max(0, Math.min(minY, maxY));
		this.maxY = Math.min(127, Math.max(minY, maxY));
		return this;
	}

	/**
	 * Places in one chunk out of {@code n} - the knob vanilla's {@code OreFeature} simply does not have,
	 * so "rare but big" needed a custom generator. {@code rarity(1)} is every chunk.
	 */
	public RetroPlacedFeature rarity(int n) {
		this.rarity = Math.max(1, n);
		return this;
	}

	/**
	 * Restricts placement to these dimension ids (vanilla: -1 nether, 0 overworld, 1 sky; a RetroAPI
	 * dimension's serial id otherwise). Defaults to the overworld; pass {@link #ANY_DIMENSION} for all.
	 */
	public RetroPlacedFeature dimensions(int... dimensionIds) {
		this.dimensions = dimensionIds.clone();
		return this;
	}

	/** Places in every dimension, vanilla and modded. */
	public RetroPlacedFeature anyDimension() {
		this.dimensions = new int[]{ANY_DIMENSION};
		return this;
	}

	/** Places in a RetroAPI dimension, by the identifier it was registered with. */
	public RetroPlacedFeature dimension(net.ornithemc.osl.core.api.util.NamespacedIdentifier dimensionId) {
		com.periut.retroapi.dimension.DimensionRegistration reg =
			com.periut.retroapi.dimension.RetroDimensionRegistry.getByIdentifier(dimensionId);
		if (reg != null) {
			this.dimensions = new int[]{reg.getSerialId()};
		}
		return this;
	}

	// --- ore knobs, forwarded so one chain reads front to back ------------------------------------
	// (what it is, then where it goes: `ore(RUBY).size(6).count(8).heightRange(4, 32)`)

	/** Vein size, for an ore feature. @throws IllegalStateException on any other feature */
	public RetroPlacedFeature size(int size) {
		ore().size(size);
		return this;
	}

	/** The metadata / flattened state the vein is placed with, for an ore feature. */
	public RetroPlacedFeature meta(int meta) {
		ore().meta(meta);
		return this;
	}

	/** The blocks the vein may carve into (default vanilla stone), for an ore feature. */
	public RetroPlacedFeature replace(Block... blocks) {
		ore().replace(blocks);
		return this;
	}

	/** The blocks the vein may carve into, by raw id. */
	public RetroPlacedFeature replace(int... blockIds) {
		ore().replace(blockIds);
		return this;
	}

	/** Lets the vein carve into anything non-air. */
	public RetroPlacedFeature replaceAnything() {
		ore().replaceAnything();
		return this;
	}

	private RetroOreFeature ore() {
		if (feature instanceof RetroOreFeature ore) {
			return ore;
		}
		throw new IllegalStateException(
			"size/meta/replace only apply to RetroFeatures.ore(...) and cluster(...) features");
	}

	/** Adds this feature to the world-generation table. Call once, from your {@code retroapi} entrypoint. */
	public RetroPlacedFeature register() {
		RetroFeatures.add(this);
		return this;
	}

	// --- internals --------------------------------------------------------------------------------

	boolean appliesTo(World world) {
		if (dimensions.length == 0) {
			return false;
		}
		int id = world.dimension.id;
		for (int allowed : dimensions) {
			if (allowed == ANY_DIMENSION || allowed == id) {
				return true;
			}
		}
		return false;
	}

	void generate(World world, Random random, int chunkBlockX, int chunkBlockZ) {
		if (rarity > 1 && random.nextInt(rarity) != 0) {
			return;
		}
		for (int i = 0; i < count; i++) {
			int x = chunkBlockX + random.nextInt(16);
			int z = chunkBlockZ + random.nextInt(16);
			int y = minY + (maxY > minY ? random.nextInt(maxY - minY + 1) : 0);
			feature.generate(world, random, x, y, z);
		}
	}

	/** The block a feature places, when it is one of the built-ins (used by the test/debug logging). */
	Block describeBlock() {
		return feature instanceof RetroOreFeature ore ? ore.block() : null;
	}
}
