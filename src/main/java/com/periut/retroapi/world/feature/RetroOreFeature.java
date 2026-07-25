package com.periut.retroapi.world.feature;

import net.minecraft.block.Block;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Vanilla's ore-vein shape, with the three things vanilla's {@code OreFeature} cannot do:
 *
 * <ul>
 *   <li><b>metadata / state</b> - places a specific block state, not just a bare id, so a "marble" or
 *       "andesite" variant of one block can generate;</li>
 *   <li><b>a target other than stone</b> - {@link #replace} takes any set of block ids, so ore can
 *       generate inside a modded stone type (or inside several, for the "many stone types underground"
 *       pattern) instead of only vanilla stone;</li>
 *   <li><b>size and placement decoupled</b> - vein size here, count/height/rarity on the placement.</li>
 * </ul>
 *
 * <pre>
 * RetroFeatures.ore(MyMod.RUBY_ORE).size(6).count(8).heightRange(4, 32).register();
 *
 * // slate that replaces stone deep down, carrying state index 2
 * RetroFeatures.ore(MyMod.STONE_TYPES).meta(2).size(48).count(3)
 *     .heightRange(0, 48).replace(Block.STONE).register();
 * </pre>
 */
public final class RetroOreFeature implements RetroFeature {

	private final int blockId;
	private int meta;
	private int size = 8;
	private int[] replaceIds = {Block.STONE.id};

	RetroOreFeature(int blockId) {
		this.blockId = blockId;
	}

	/** Blocks per vein (vanilla ores use 4-33). Bigger veins are longer and fatter. */
	public RetroOreFeature size(int size) {
		this.size = Math.max(1, size);
		return this;
	}

	/** The metadata (or flattened state index) every block of the vein is placed with. */
	public RetroOreFeature meta(int meta) {
		this.meta = meta;
		return this;
	}

	/** The blocks this vein may carve into. Defaults to vanilla stone. */
	public RetroOreFeature replace(Block... blocks) {
		int[] ids = new int[blocks.length];
		for (int i = 0; i < blocks.length; i++) {
			ids[i] = blocks[i].id;
		}
		this.replaceIds = ids;
		return this;
	}

	/** The blocks this vein may carve into, by raw id (for vanilla ids you do not have a Block for). */
	public RetroOreFeature replace(int... blockIds) {
		this.replaceIds = blockIds.clone();
		return this;
	}

	/** Carves into anything solid, not just one target - useful for cave-fill style features. */
	public RetroOreFeature replaceAnything() {
		this.replaceIds = null;
		return this;
	}

	Block block() {
		return blockId >= 0 && blockId < Block.BLOCKS.length ? Block.BLOCKS[blockId] : null;
	}

	@Override
	public boolean generate(World world, Random random, int x, int y, int z) {
		// Vanilla's vein shape: a line between two random endpoints, swollen into overlapping ellipsoids.
		float angle = random.nextFloat() * (float) Math.PI;
		double startX = x + 8 + MathHelper.sin(angle) * size / 8.0F;
		double endX = x + 8 - MathHelper.sin(angle) * size / 8.0F;
		double startZ = z + 8 + MathHelper.cos(angle) * size / 8.0F;
		double endZ = z + 8 - MathHelper.cos(angle) * size / 8.0F;
		double startY = y + random.nextInt(3) - 2;
		double endY = y + random.nextInt(3) - 2;

		boolean placed = false;
		for (int step = 0; step <= size; step++) {
			double centerX = startX + (endX - startX) * step / size;
			double centerY = startY + (endY - startY) * step / size;
			double centerZ = startZ + (endZ - startZ) * step / size;
			double swell = random.nextDouble() * size / 16.0D;
			double radiusXZ = (MathHelper.sin(step * (float) Math.PI / size) + 1.0F) * swell + 1.0D;
			double radiusY = (MathHelper.sin(step * (float) Math.PI / size) + 1.0F) * swell + 1.0D;

			for (int bx = MathHelper.floor(centerX - radiusXZ / 2.0D); bx <= MathHelper.floor(centerX + radiusXZ / 2.0D); bx++) {
				for (int by = MathHelper.floor(centerY - radiusY / 2.0D); by <= MathHelper.floor(centerY + radiusY / 2.0D); by++) {
					for (int bz = MathHelper.floor(centerZ - radiusXZ / 2.0D); bz <= MathHelper.floor(centerZ + radiusXZ / 2.0D); bz++) {
						if (by < 0 || by > 127) continue;
						double dx = (bx + 0.5D - centerX) / (radiusXZ / 2.0D);
						double dy = (by + 0.5D - centerY) / (radiusY / 2.0D);
						double dz = (bz + 0.5D - centerZ) / (radiusXZ / 2.0D);
						if (dx * dx + dy * dy + dz * dz >= 1.0D) continue;
						if (!canReplace(world.getBlockId(bx, by, bz))) continue;
						RetroFeatures.setBlock(world, bx, by, bz, blockId, meta);
						placed = true;
					}
				}
			}
		}
		return placed;
	}

	private boolean canReplace(int id) {
		if (replaceIds == null) {
			return id != 0;
		}
		for (int target : replaceIds) {
			if (id == target) return true;
		}
		return false;
	}
}
