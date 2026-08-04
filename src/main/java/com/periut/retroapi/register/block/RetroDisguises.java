package com.periut.retroapi.register.block;

import net.minecraft.block.Block;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.world.BlockView;

/**
 * Resolves {@link RetroBlockDisguise} for the hooks that need it, and remembers the last few disguises to
 * leave the world.
 *
 * <p>That second job is not an optimisation, it is the only way two of these can work at all. A block's
 * break sound and its cloud of debris are not produced while the block is being broken; they are produced
 * by a world event that arrives after it is already gone, carrying a block id and a metadata value and no
 * way to ask the world anything. By then the disguise has been cleared along with the block. So the
 * disguise is written down on the way out and read back by those two hooks.
 *
 * <p>A tiny ring is enough. The event follows the removal within the same tick, and nothing here needs to
 * be right about a position that was broken a thousand blocks ago.
 */
public final class RetroDisguises {

	private RetroDisguises() {}

	// --- live lookup --------------------------------------------------------------------------

	/** The block a position presents as, or null when it presents as itself. */
	public static Block at(BlockView world, int x, int y, int z) {
		if (world == null) {
			return null;
		}
		int id = world.getBlockId(x, y, z);
		if (id <= 0 || id >= Block.BLOCKS.length) {
			return null;
		}
		Block block = Block.BLOCKS[id];
		if (!(block instanceof RetroBlockDisguise disguise)) {
			return null;
		}
		Block worn = disguise.disguisedBlock(world, x, y, z);
		return worn == block ? null : worn;
	}

	/** The metadata of the block a position presents as. */
	public static int metaAt(BlockView world, int x, int y, int z) {
		if (world == null) {
			return 0;
		}
		int id = world.getBlockId(x, y, z);
		if (id <= 0 || id >= Block.BLOCKS.length) {
			return 0;
		}
		Block block = Block.BLOCKS[id];
		return block instanceof RetroBlockDisguise disguise ? disguise.disguisedMeta(world, x, y, z) : 0;
	}

	/** The sound group a position should use: the disguise's when it has one, otherwise the block's own. */
	public static BlockSoundGroup soundsAt(BlockView world, int x, int y, int z, Block fallback) {
		Block worn = at(world, x, y, z);
		return worn != null ? worn.soundGroup : (fallback != null ? fallback.soundGroup : null);
	}

	/**
	 * The disguise at the block a player is currently breaking, when that block is the one asked about.
	 *
	 * <p>Beta's mining questions carry a block and no coordinates, so this is the only way to answer one
	 * per position. The break record is validated against the world, so it can only ever answer for a
	 * block that is genuinely still standing where it was recorded.
	 */
	public static Block atCurrentBreak(Block block) {
		com.periut.retroapi.tag.RetroBreakTarget target =
			com.periut.retroapi.tag.RetroBreakTarget.currentAny(block);
		if (target == null) {
			return null;
		}
		return at(target.world(), target.x(), target.y(), target.z());
	}

	/**
	 * The disguise at a position that a player is currently breaking. For hooks that are handed
	 * coordinates but no world, which beta's break sound and progress hooks are.
	 */
	public static Block atBreakPosition(int x, int y, int z) {
		com.periut.retroapi.tag.RetroBreakTarget target =
			com.periut.retroapi.tag.RetroBreakTarget.currentAt(x, y, z);
		return target == null ? null : at(target.world(), x, y, z);
	}

	// --- what has just left the world ----------------------------------------------------------

	private static final int REMEMBERED = 16;
	private static final int[] POSITIONS = new int[REMEMBERED * 3];
	private static final Block[] BLOCKS = new Block[REMEMBERED];
	private static final int[] METAS = new int[REMEMBERED];
	private static int next = 0;

	/**
	 * Records the disguise at a position, called just before the block there is replaced. A position with
	 * no disguise is not recorded, so the ring only ever holds entries that would otherwise be lost.
	 */
	public static void remember(BlockView world, int x, int y, int z) {
		Block worn = at(world, x, y, z);
		if (worn == null) {
			return;
		}
		int slot = next;
		next = (next + 1) % REMEMBERED;
		POSITIONS[slot * 3] = x;
		POSITIONS[slot * 3 + 1] = y;
		POSITIONS[slot * 3 + 2] = z;
		BLOCKS[slot] = worn;
		METAS[slot] = metaAt(world, x, y, z);
	}

	/** The disguise a position had when it was last removed, or null. */
	public static Block remembered(int x, int y, int z) {
		int slot = findRemembered(x, y, z);
		return slot < 0 ? null : BLOCKS[slot];
	}

	/** The metadata the remembered disguise had. */
	public static int rememberedMeta(int x, int y, int z) {
		int slot = findRemembered(x, y, z);
		return slot < 0 ? 0 : METAS[slot];
	}

	private static int findRemembered(int x, int y, int z) {
		for (int slot = 0; slot < REMEMBERED; slot++) {
			if (BLOCKS[slot] != null
				&& POSITIONS[slot * 3] == x && POSITIONS[slot * 3 + 1] == y && POSITIONS[slot * 3 + 2] == z) {
				return slot;
			}
		}
		return -1;
	}

	/**
	 * The disguise for a position that may or may not still hold the block: live if it is there, the
	 * remembered one if it has just gone. This is what the break sound and the debris cloud want.
	 */
	public static Block liveOrRemembered(BlockView world, int x, int y, int z) {
		Block worn = at(world, x, y, z);
		return worn != null ? worn : remembered(x, y, z);
	}
}
