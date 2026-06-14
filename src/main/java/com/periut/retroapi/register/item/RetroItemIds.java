package com.periut.retroapi.register.item;

import net.minecraft.item.Item;

import java.util.HashSet;
import java.util.Set;

/**
 * Central allocator for placeholder item ids.
 *
 * <p>Registering an item is inherently circular. Vanilla's {@code Item(int)} constructor writes
 * {@code this} into {@code Item.ITEMS[256 + id]}, so you must hand the constructor a free slot - but
 * the only way to find a free slot is to read that same array. RetroAPI bridges the two with a
 * temporary <em>placeholder</em> id at construction time; the real, world-stable id is assigned
 * later by {@link com.periut.retroapi.registry.IdAssigner} when a world loads.
 *
 * <p>The hazard is the window between "scan {@code Item.ITEMS} for a free slot" and "the constructor
 * actually stores into it". If a second item is constructed inside that window - directly, through a
 * subclass constructor chain, or as a side effect of class initialization - both allocations can pick
 * the SAME slot. The second store silently overwrites the first (vanilla prints {@code CONFLICT @
 * <id>}), leaving a registered item whose {@code Item.ITEMS} entry points at someone else. That
 * surfaces, intermittently and depending on class-load order, as a {@link NullPointerException} the
 * next time anything looks the item up by id.
 *
 * <p>This allocator closes the window: every slot it hands out is recorded in {@link #RESERVED} and
 * never handed out again, even before the constructor has run, and the whole operation is
 * synchronized so concurrent registration can't race either. Combined with the {@link
 * RetroItemAccess#AUTO_ID} sentinel (which allocates from <em>inside</em> the constructor, so the
 * scan and the store are a single atomic step) the old failure mode is gone.
 */
public final class RetroItemIds {
	private RetroItemIds() {}

	/**
	 * Raw {@code Item.ITEMS} index of the first slot we hand out. Item ids carry a +256 offset, so
	 * this corresponds to item id 2000 - just above vanilla's highest items, the music discs, which
	 * sit at item ids 2000/2001 ({@code Item.ITEMS[2256]}/{@code [2257]}) and are skipped by the
	 * null check below.
	 */
	private static final int FIRST_SLOT = 2000 + 256;

	private static final Set<Integer> RESERVED = new HashSet<>();

	/**
	 * Reserve and return a free placeholder item id - the value to pass to {@code Item(int)} or
	 * {@code super(int)}. The constructor will store the item at {@code Item.ITEMS[256 + id]}.
	 *
	 * <p>Synchronized and reservation-tracked, so two allocations - even reentrant or on different
	 * threads - can never collide on a slot regardless of when their constructors run.
	 */
	public static synchronized int allocate() {
		Item[] byId = Item.ITEMS;
		for (int i = FIRST_SLOT; i < byId.length; i++) {
			if (byId[i] == null && !RESERVED.contains(i)) {
				RESERVED.add(i);
				return i - 256;
			}
		}
		throw new RuntimeException("No more placeholder item IDs available");
	}
}
