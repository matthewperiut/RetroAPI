package com.periut.retroapi.register.block;

import net.minecraft.block.Block;
import net.minecraft.world.BlockView;

/**
 * A block that presents as a DIFFERENT block at a given position.
 *
 * <p>Beta answers three questions per block type, and a block whose appearance is per position needs them
 * answered per position instead:
 *
 * <ul>
 *   <li><b>Which tool works on it.</b> Mining speed and the harvest check both take a {@code Block} and
 *       nothing else, so every position of a block gets the same tool.</li>
 *   <li><b>What its particles look like.</b> {@code BlockParticle} reads
 *       {@code block.getTexture(0, meta)}, the block's static sprite.</li>
 *   <li><b>What it sounds like</b> to walk on or to break. {@code Block.soundGroup} is a field.</li>
 * </ul>
 *
 * <p>Implement this and RetroAPI answers all three from the disguise instead. A block wearing stone is
 * mined with a pickaxe, breaks into stone-coloured dust and crunches underfoot, without the block itself
 * having to know anything about how mining, particles or sounds are wired.
 *
 * <p>The disguise is additive for tools: whatever the block itself declares still works, so a wooden
 * frame wearing stone answers to an axe <em>and</em> a pickaxe. That is deliberate. Taking the axe away
 * because it is currently wearing stone would make a block harder to break the more you had decorated it.
 *
 * <p>Return null for a position with no disguise, which is the common case and costs nothing.
 */
public interface RetroBlockDisguise {

	/** The block this position presents as, or null to present as itself. */
	Block disguisedBlock(BlockView world, int x, int y, int z);

	/** The metadata of the presented block, for its sprite and sound. */
	default int disguisedMeta(BlockView world, int x, int y, int z) {
		return 0;
	}
}
