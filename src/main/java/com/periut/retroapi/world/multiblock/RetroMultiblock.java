package com.periut.retroapi.world.multiblock;

import com.periut.retroapi.state.RetroBlockState;
import com.periut.retroapi.state.RetroFacing;
import com.periut.retroapi.util.RetroVec3i;
import net.minecraft.block.Block;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A multiblock pattern: what shape a structure has, drawn as ASCII layers, and how to test whether it is
 * standing in the world.
 *
 * <p>Beta gives you nothing here, so multiblocks end up as pages of hand-written neighbor checks that
 * are wrong in at least one rotation. Declare the shape once instead:
 *
 * <pre>
 * static final RetroMultiblock FURNACE_TOWER = RetroMultiblock.builder()
 *     .layer("BBB",           // y = 0, the bottom layer
 *            "BCB",
 *            "BBB")
 *     .layer(" F ",           // y = 1
 *            "F F",
 *            " F ")
 *     .where('B', Block.BRICKS)
 *     .where('C', MyMod.CORE)          // the "anchor" character marks the block you match FROM
 *     .where('F', Block.IRON_BLOCK)
 *     .where(' ', RetroMultiblock.ANY)  // space = don't care
 *     .anchor('C')
 *     .build();
 *
 * // in the core block entity's tick, or from BlockEntityLoadedCallback:
 * RetroMultiblock.Match match = FURNACE_TOWER.matchAnyRotation(world, x, y, z);
 * if (match != null) {
 *     for (RetroVec3i part : match.positions()) { … }
 * }
 * </pre>
 *
 * <p>Rows read west to east, layers bottom to top, and the first {@code .layer(...)} row is the NORTH-most
 * row - the same orientation you'd draw the structure on paper looking down at it. Matching can be tried
 * in one facing or in all four ({@link #matchAnyRotation}), so you author the shape once and it works
 * however the player built it.
 */
public final class RetroMultiblock {

	/** Predicate accepting any block, including air - the "don't care" cell. */
	public static final Predicate ANY = (world, x, y, z) -> true;

	/** Predicate accepting only air. */
	public static final Predicate AIR = (world, x, y, z) -> world.getBlockId(x, y, z) == 0;

	/** Tests one cell of a pattern against the world. */
	@FunctionalInterface
	public interface Predicate {
		boolean test(BlockView world, int x, int y, int z);
	}

	/** A successful match: the facing it matched in, and every position the pattern covers. */
	public static final class Match {
		private final RetroFacing facing;
		private final List<RetroVec3i> positions;
		private final Map<Character, List<RetroVec3i>> byKey;

		Match(RetroFacing facing, List<RetroVec3i> positions, Map<Character, List<RetroVec3i>> byKey) {
			this.facing = facing;
			this.positions = positions;
			this.byKey = byKey;
		}

		/** The rotation the structure was found in (its "front"). */
		public RetroFacing facing() {
			return facing;
		}

		/** Every world position the pattern covers, anchor included. */
		public List<RetroVec3i> positions() {
			return positions;
		}

		/** The world positions of one pattern character, e.g. every {@code 'F'} of the frame. */
		public List<RetroVec3i> positions(char key) {
			List<RetroVec3i> list = byKey.get(key);
			return list != null ? list : java.util.Collections.emptyList();
		}

		/** Replaces every matched position with one block - the "form the structure" step. */
		public void fill(World world, Block block, int meta) {
			for (RetroVec3i pos : positions) {
				world.setBlock(pos.x, pos.y, pos.z, block == null ? 0 : block.id, meta);
			}
		}

		/**
		 * Replaces every matched position with one block STATE. The {@code meta} overload can only carry
		 * the vanilla nibble, so a block with more than 16 states would be filled in truncated - this
		 * writes the sidecar bits too.
		 */
		public void fill(World world, com.periut.retroapi.state.RetroBlockState state) {
			for (RetroVec3i pos : positions) {
				world.setBlock(pos.x, pos.y, pos.z, state.getBlock().id, state.getIndex() & 15);
				com.periut.retroapi.state.RetroStates.set(world, pos.x, pos.y, pos.z, state);
			}
		}
	}

	private final List<String[]> layers;
	private final Map<Character, Predicate> keys;
	private final char anchor;

	private RetroMultiblock(List<String[]> layers, Map<Character, Predicate> keys, char anchor) {
		this.layers = layers;
		this.keys = keys;
		this.anchor = anchor;
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Tests the pattern against the world, treating {@code (x, y, z)} as the anchor cell and the pattern
	 * as drawn (its front facing north).
	 *
	 * @return the match, or null if the structure is not there
	 */
	public Match match(BlockView world, int x, int y, int z) {
		return match(world, x, y, z, RetroFacing.NORTH);
	}

	/** Tests the pattern rotated so its front points {@code facing}. */
	public Match match(BlockView world, int x, int y, int z, RetroFacing facing) {
		RetroVec3i anchorOffset = findAnchor();
		if (anchorOffset == null) {
			return null;
		}
		List<RetroVec3i> positions = new ArrayList<>();
		Map<Character, List<RetroVec3i>> byKey = new LinkedHashMap<>();

		for (int layer = 0; layer < layers.size(); layer++) {
			String[] rows = layers.get(layer);
			for (int row = 0; row < rows.length; row++) {
				String line = rows[row];
				for (int col = 0; col < line.length(); col++) {
					char key = line.charAt(col);
					Predicate predicate = keys.get(key);
					if (predicate == null) {
						throw new IllegalStateException("Multiblock pattern uses '" + key
							+ "' but no .where('" + key + "', ...) declared it");
					}
					RetroVec3i local = new RetroVec3i(col, layer, row).subtract(anchorOffset).rotateTo(facing);
					int wx = x + local.x, wy = y + local.y, wz = z + local.z;
					if (!predicate.test(world, wx, wy, wz)) {
						return null;
					}
					if (predicate != ANY) {
						RetroVec3i pos = new RetroVec3i(wx, wy, wz);
						positions.add(pos);
						byKey.computeIfAbsent(key, k -> new ArrayList<>()).add(pos);
					}
				}
			}
		}
		return new Match(facing, positions, byKey);
	}

	/**
	 * Tries all four horizontal rotations and returns the first that matches - so the player can build the
	 * structure facing whichever way they like.
	 */
	public Match matchAnyRotation(BlockView world, int x, int y, int z) {
		for (RetroFacing facing : RetroFacing.values()) {
			Match match = match(world, x, y, z, facing);
			if (match != null) {
				return match;
			}
		}
		return null;
	}

	/** The local offset of the anchor cell inside the pattern. */
	private RetroVec3i findAnchor() {
		for (int layer = 0; layer < layers.size(); layer++) {
			String[] rows = layers.get(layer);
			for (int row = 0; row < rows.length; row++) {
				int col = rows[row].indexOf(anchor);
				if (col >= 0) {
					return new RetroVec3i(col, layer, row);
				}
			}
		}
		return null;
	}

	/** Builds a {@link RetroMultiblock} from ASCII layers plus a legend. */
	public static final class Builder {
		private final List<String[]> layers = new ArrayList<>();
		private final Map<Character, Predicate> keys = new LinkedHashMap<>();
		private char anchor = 0;

		private Builder() {
			keys.put(' ', ANY);
		}

		/** Adds one horizontal layer, bottom first. Rows run north to south, columns west to east. */
		public Builder layer(String... rows) {
			layers.add(rows.clone());
			return this;
		}

		/** Maps a pattern character to a specific block (any metadata). */
		public Builder where(char key, Block block) {
			int id = block.id;
			return where(key, (world, x, y, z) -> world.getBlockId(x, y, z) == id);
		}

		/** Maps a pattern character to a specific block state. */
		public Builder where(char key, RetroBlockState state) {
			int id = state.getBlock().id;
			int index = state.getIndex();
			return where(key, (world, x, y, z) -> world.getBlockId(x, y, z) == id
				&& com.periut.retroapi.state.RetroStates.get(world, x, y, z) != null
				&& com.periut.retroapi.state.RetroStates.get(world, x, y, z).getIndex() == index);
		}

		/** Maps a pattern character to any of several blocks (an "or"). */
		public Builder whereAny(char key, Block... blocks) {
			int[] ids = new int[blocks.length];
			for (int i = 0; i < blocks.length; i++) {
				ids[i] = blocks[i].id;
			}
			return where(key, (world, x, y, z) -> {
				int id = world.getBlockId(x, y, z);
				for (int candidate : ids) {
					if (candidate == id) return true;
				}
				return false;
			});
		}

		/** Maps a pattern character to an arbitrary test. */
		public Builder where(char key, Predicate predicate) {
			keys.put(key, predicate);
			return this;
		}

		/**
		 * Which character is the anchor: the cell that {@code match(world, x, y, z)} is called on, usually
		 * the controller/core block. Defaults to the first character of the first layer.
		 */
		public Builder anchor(char key) {
			this.anchor = key;
			return this;
		}

		public RetroMultiblock build() {
			if (layers.isEmpty()) {
				throw new IllegalStateException("multiblock has no layers");
			}
			char resolved = anchor != 0 ? anchor : layers.get(0)[0].charAt(0);
			return new RetroMultiblock(layers, keys, resolved);
		}
	}
}
