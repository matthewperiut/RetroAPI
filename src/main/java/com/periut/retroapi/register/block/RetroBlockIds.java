package com.periut.retroapi.register.block;

import net.minecraft.block.Block;
import net.minecraft.item.Item;

import java.util.HashSet;
import java.util.Set;

/**
 * Central allocator for placeholder block ids, the block twin of
 * {@link com.periut.retroapi.register.item.RetroItemIds}.
 *
 * <p>Registering a block is circular the same way registering an item is: vanilla's
 * {@code Block(int, Material)} constructor writes {@code this} into {@code Block.BLOCKS[id]}, so the
 * constructor needs a free slot, and the only way to find one is to read that same array. RetroAPI hands
 * out a temporary <em>placeholder</em> id at construction time; the real, world-stable id is assigned
 * later by {@link com.periut.retroapi.registry.IdAssigner} when a world loads.
 *
 * <p>The hazard is the window between "scan {@code Block.BLOCKS} for a free slot" and "the constructor
 * actually stores into it". Two allocations inside that window pick the SAME slot and the second store
 * wins, leaving a registered block whose {@code Block.BLOCKS} entry points at someone else. Every slot
 * handed out here is recorded in {@link #RESERVED} and never handed out again, and the whole operation is
 * synchronized, so that can't happen even for reentrant or concurrent registration.
 *
 * <p>A block slot also has to have a matching free item slot: a block's {@code BlockItem} lives at
 * {@code Item.ITEMS[id]}, so a slot that is free on one side and taken on the other is unusable.
 */
public final class RetroBlockIds {
	private RetroBlockIds() {}

	/** First slot handed out: vanilla blocks occupy 1-255, so modded ids start at 256. */
	private static final int FIRST_SLOT = 256;

	private static final Set<Integer> RESERVED = new HashSet<>();

	/**
	 * Reserve and return a free placeholder block id - the value to pass to {@code Block(int, Material)}
	 * or {@code super(int, Material)}. Prefer {@link RetroBlockAccess#AUTO_ID}, which allocates from
	 * inside the constructor so the scan and the store are a single atomic step.
	 */
	public static synchronized int allocate() {
		Block[] byId = Block.BLOCKS;
		Item[] itemById = Item.ITEMS;
		for (int i = FIRST_SLOT; i < byId.length; i++) {
			if (byId[i] == null && !RESERVED.contains(i)
				&& (i >= itemById.length || itemById[i] == null)) {
				RESERVED.add(i);
				return i;
			}
		}
		throw new RuntimeException("No more placeholder block IDs available (256-" + (byId.length - 1) + " exhausted)");
	}
}
