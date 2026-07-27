package com.periut.retroapi.world.multiblock;

import com.periut.retroapi.util.RetroVec3i;
import net.minecraft.block.Block;
import net.minecraft.world.BlockView;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A freeform multiblock: whatever connected run of blocks the player actually built, found by flooding
 * outward from any block in it.
 *
 * <p>{@link RetroMultiblock} is the other shape - a pattern you draw once and check against the world.
 * That fits a machine with a fixed silhouette and a controller block, and it is the wrong tool for a
 * structure whose size and shape are the player's choice: a tank that is however many blocks tall, a room
 * walled off with your bricks, a pipe network, a shrine that just has to be big enough. There is no
 * pattern to write for those, only a rule for what counts as a member.
 *
 * <pre>
 * // right-click any wall block: how big is the room this belongs to?
 * RetroBlockRegion.Region region = RetroBlockRegion.flood(world, x, y, z, 512,
 *     (w, bx, by, bz) -&gt; w.getBlockId(bx, by, bz) == MyMod.SHRINE_BRICK.id);
 *
 * if (region.complete() &amp;&amp; region.size() &gt;= 32) formShrine(region);
 * </pre>
 *
 * <p>Nothing here is cached or ticked: it is a plain search over loaded blocks, so keep {@code limit}
 * honest and do not run it every tick for every block. {@code BlockView} is enough, so it works from a
 * client render pass as well as from server logic.
 */
public final class RetroBlockRegion {

	private RetroBlockRegion() {}

	/** Which positions belong to a region. Same shape as {@link RetroMultiblock.Predicate}. */
	@FunctionalInterface
	public interface Member {
		boolean test(BlockView world, int x, int y, int z);
	}

	/** The result of a flood: the positions found, and whether the search finished or ran out of budget. */
	public static final class Region {
		private final List<RetroVec3i> positions;
		private final boolean complete;

		Region(List<RetroVec3i> positions, boolean complete) {
			this.positions = Collections.unmodifiableList(positions);
			this.complete = complete;
		}

		/** Every position in the region, starting with the one the search began at. */
		public List<RetroVec3i> positions() {
			return positions;
		}

		public int size() {
			return positions.size();
		}

		public boolean isEmpty() {
			return positions.isEmpty();
		}

		/**
		 * True when the whole region was walked. False means the search hit its {@code limit} and the
		 * structure is at least that big but was not fully explored - treat it as "too big", not as a
		 * measurement.
		 */
		public boolean complete() {
			return complete;
		}

		/** The lowest corner of the region's bounding box, or null when empty. */
		public RetroVec3i min() {
			return corner(true);
		}

		/** The highest corner of the region's bounding box, or null when empty. */
		public RetroVec3i max() {
			return corner(false);
		}

		private RetroVec3i corner(boolean low) {
			if (positions.isEmpty()) {
				return null;
			}
			int x = positions.get(0).x, y = positions.get(0).y, z = positions.get(0).z;
			for (RetroVec3i pos : positions) {
				x = low ? Math.min(x, pos.x) : Math.max(x, pos.x);
				y = low ? Math.min(y, pos.y) : Math.max(y, pos.y);
				z = low ? Math.min(z, pos.z) : Math.max(z, pos.z);
			}
			return new RetroVec3i(x, y, z);
		}
	}

	/**
	 * Walks outward from {@code (x, y, z)} through face-adjacent blocks that {@code member} accepts.
	 *
	 * @param limit the most positions to visit; a structure larger than this comes back with
	 *              {@link Region#complete()} false rather than freezing the tick
	 * @return the region, empty when the starting block is not a member itself
	 */
	public static Region flood(BlockView world, int x, int y, int z, int limit, Member member) {
		return flood(world, x, y, z, limit, member, false);
	}

	/**
	 * {@link #flood} with a choice of connectivity: face-adjacent only (6 neighbors, the default) or also
	 * through edges and corners (26 neighbors), which is what a diagonally-built wall needs.
	 */
	public static Region flood(BlockView world, int x, int y, int z, int limit, Member member,
			boolean diagonals) {
		List<RetroVec3i> found = new ArrayList<>();
		if (limit <= 0 || !member.test(world, x, y, z)) {
			return new Region(found, true);
		}
		Set<RetroVec3i> seen = new HashSet<>();
		Deque<RetroVec3i> queue = new ArrayDeque<>();
		RetroVec3i start = new RetroVec3i(x, y, z);
		seen.add(start);
		queue.add(start);

		while (!queue.isEmpty()) {
			if (found.size() >= limit) {
				return new Region(found, false);
			}
			RetroVec3i pos = queue.poll();
			found.add(pos);
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						int steps = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
						if (steps == 0 || (!diagonals && steps != 1)) {
							continue;
						}
						RetroVec3i next = pos.add(dx, dy, dz);
						if (seen.add(next) && member.test(world, next.x, next.y, next.z)) {
							queue.add(next);
						}
					}
				}
			}
		}
		return new Region(found, true);
	}

	/** {@link #flood} for the common case: every connected block of one of these types. */
	public static Region flood(BlockView world, int x, int y, int z, int limit, Block... blocks) {
		int[] ids = new int[blocks.length];
		for (int i = 0; i < blocks.length; i++) {
			ids[i] = blocks[i].id;
		}
		return flood(world, x, y, z, limit, (w, bx, by, bz) -> {
			int id = w.getBlockId(bx, by, bz);
			for (int candidate : ids) {
				if (candidate == id) return true;
			}
			return false;
		});
	}
}
