package com.periut.retroapi.world.height;

import com.periut.retroapi.RetroAPI;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-dimension vertical extension: world space above y127 and below y0, stored in RetroAPI's sidecar so
 * a vanilla session loses nothing.
 *
 * <h2>Why this cannot just change a constant</h2>
 * Beta's chunk is a {@code byte[32768]} indexed {@code x << 11 | z << 7 | y}. Y is <b>seven bits</b>. It
 * is not that the height is configured as 128 somewhere - it is that a Y of 128 or -1 has nowhere to go
 * in the array at all, and the light arrays, the heightmap byte, the chunk packet and the region format
 * all agree with it. So extension blocks cannot live in vanilla's storage, and that turns out to be
 * exactly what makes them vanilla-safe: they live in the sidecar, and a vanilla session simply plays a
 * 0-127 world, unaware and undamaged.
 *
 * <h2>Declaring it</h2>
 * <pre>{@code
 * // in your `retroapi` entrypoint, before any world loads
 * RetroWorldHeight.extend(0, 48, 0);      // overworld: y -48 .. 127
 * RetroWorldHeight.extend(-1, 0, 64);     // nether: y 0 .. 191
 *
 * int bottom = RetroWorldHeight.bottomY(0);   // -48
 * int top    = RetroWorldHeight.topY(0);      // 127
 * }</pre>
 *
 * Extension is per dimension because the reasons to want it are per dimension, and it is declared once
 * at init rather than per world because a world's geometry must not change under a save that already
 * exists.
 *
 * <h2>The bedrock rule</h2>
 * With a downward extension the vanilla bedrock band at y0-4 is <b>not</b> reproduced at the new bottom
 * and is not kept where it was: {@link #bedrockY} reports the extended bottom, and a generator is
 * expected to clear y1-4 to stone and lay bedrock at {@code bottomY}. The floor of the world moves down
 * rather than the world gaining a second floor halfway up its new depth.
 *
 * <p>The cost is explicit: a vanilla session finds breakable stone where bedrock used to be, so a vanilla
 * player can mine through the bottom of the world. That is a gameplay difference, not data loss, and it
 * is the deliberate intent of the feature - a bedrock ceiling 48 blocks above the actual floor would make
 * the extension unreachable and pointless.
 *
 * <h2>Scope of this release</h2>
 * <b>Storage, addressing and world access are implemented; client rendering and lighting are not.</b>
 *
 * <p>What works today: declaring bounds, {@link RetroExtendedSections} storage on every chunk, sidecar
 * persistence (a new {@code vext} section, absent from chunks that have none), and - as of 0.3.8 - the whole
 * game's own block API at extension Y. {@code World.getBlockId}, {@code getBlockMeta}, {@code setBlock},
 * {@code isAir}, {@code getMaterial} and {@code isPosLoaded} all answer for a block at y-1 the way they
 * answer for one at y64; see {@code mixin.world.WorldHeightMixin}. Until 0.3.8 only this class's own
 * {@link #getBlock} / {@link #setBlock} could see such a block, which meant nothing in the game agreed it
 * existed.
 *
 * <p>What does not: beta's {@code WorldRenderer} builds its chunk grid over a hardcoded 0-127 column, and
 * its {@code blockLight} / {@code skyLight} arrays are sized {@code 16*128*16} with a one-byte-per-column
 * heightmap that cannot hold a negative Y at all. So an extension block is stored, saved, reloaded, read,
 * written and collided with correctly, and is still <em>not drawn and not lit</em>. Rendering is the next
 * step and is independently testable with light faked to a constant; lighting is the larger half after it.
 * A dimension that never calls {@link #extend} is completely unaffected either way.
 */
public final class RetroWorldHeight {

	/** The vanilla column every beta world has, and the only part vanilla itself can see. */
	public static final int VANILLA_BOTTOM_Y = 0;
	public static final int VANILLA_TOP_Y = 127;
	public static final int VANILLA_HEIGHT = 128;

	/** Sections are 16 tall, matching the cubic biome cell and modern chunk sections. */
	public static final int SECTION_HEIGHT = 16;

	private static final Map<Integer, Extension> EXTENSIONS = new HashMap<>();

	/**
	 * True once any dimension has been extended, and the first thing every hot-path hook tests.
	 *
	 * <p>{@code World.getBlockId} is called several million times a second, so the routing hook on it cannot
	 * afford a map lookup - or anything else - in the overwhelmingly common case where the answer is "no". A
	 * Y inside 0-127 leaves before this is even read; a Y outside it leaves here unless some mod has actually
	 * asked for an extension. Written once during init, read everywhere after, hence volatile.
	 */
	private static volatile boolean anyExtended = false;

	private RetroWorldHeight() {}

	private record Extension(int down, int up) {}

	// --- declaring ----------------------------------------------------------------------------

	/**
	 * Extends a dimension's world column. Both amounts are in blocks and are rounded up to a whole
	 * 16-block section, because the storage, the cubic biome grid and every renderer that will ever read
	 * this are all section-aligned.
	 *
	 * @param dimensionId the dimension to extend (0 overworld, -1 nether)
	 * @param down        blocks below y0, 0 for none
	 * @param up          blocks above y127, 0 for none
	 */
	public static void extend(int dimensionId, int down, int up) {
		if (down < 0 || up < 0) {
			throw new IllegalArgumentException("Extension amounts cannot be negative");
		}
		int alignedDown = align(down);
		int alignedUp = align(up);
		if (alignedDown == 0 && alignedUp == 0) {
			EXTENSIONS.remove(dimensionId);
			anyExtended = !EXTENSIONS.isEmpty();
			return;
		}
		Extension existing = EXTENSIONS.get(dimensionId);
		if (existing != null && (existing.down() != alignedDown || existing.up() != alignedUp)) {
			// Two mods disagreeing about a dimension's height would silently corrupt whichever one lost.
			// Take the larger of each so both mods' content still has somewhere to live, and say so.
			alignedDown = Math.max(alignedDown, existing.down());
			alignedUp = Math.max(alignedUp, existing.up());
			RetroAPI.LOGGER.warn("Dimension {} extension declared twice with different bounds; widening to -{}/+{}",
				dimensionId, alignedDown, alignedUp);
		}
		EXTENSIONS.put(dimensionId, new Extension(alignedDown, alignedUp));
		anyExtended = true;
		RetroAPI.LOGGER.info("Dimension {} extended to y {} .. {}", dimensionId,
			VANILLA_BOTTOM_Y - alignedDown, VANILLA_TOP_Y + alignedUp);
	}

	private static int align(int blocks) {
		return (blocks + SECTION_HEIGHT - 1) / SECTION_HEIGHT * SECTION_HEIGHT;
	}

	// --- querying -----------------------------------------------------------------------------

	/**
	 * True when <em>any</em> dimension is extended. A single static read, for hooks on paths hot enough that
	 * {@link #isExtended(int)}'s map lookup would show up in a profile.
	 */
	public static boolean anyExtended() {
		return anyExtended;
	}

	/** True when the dimension has any extension at all. The fast path everything else checks first. */
	public static boolean isExtended(int dimensionId) {
		return EXTENSIONS.containsKey(dimensionId);
	}

	/** The lowest Y the dimension has. {@code 0} unless extended downward. */
	public static int bottomY(int dimensionId) {
		Extension extension = EXTENSIONS.get(dimensionId);
		return extension == null ? VANILLA_BOTTOM_Y : VANILLA_BOTTOM_Y - extension.down();
	}

	/** The highest Y the dimension has. {@code 127} unless extended upward. */
	public static int topY(int dimensionId) {
		Extension extension = EXTENSIONS.get(dimensionId);
		return extension == null ? VANILLA_TOP_Y : VANILLA_TOP_Y + extension.up();
	}

	/** The dimension's full column height in blocks. */
	public static int height(int dimensionId) {
		return topY(dimensionId) - bottomY(dimensionId) + 1;
	}

	/**
	 * Where the world's unbreakable floor belongs: the extended bottom when the dimension is extended
	 * downward, y0 otherwise. See the bedrock rule in the class doc.
	 */
	public static int bedrockY(int dimensionId) {
		return bottomY(dimensionId);
	}

	/**
	 * True when the vanilla bedrock band at y1-4 should be cleared to stone. Only when the dimension is
	 * extended downward - otherwise y0-4 is still the bottom of the world and its bedrock is load-bearing.
	 */
	public static boolean clearsVanillaBedrock(int dimensionId) {
		Extension extension = EXTENSIONS.get(dimensionId);
		return extension != null && extension.down() > 0;
	}

	/** The lowest section index the dimension has ({@code bottomY >> 4}; negative when extended down). */
	public static int bottomSection(int dimensionId) {
		return bottomY(dimensionId) >> 4;
	}

	/** The highest section index the dimension has. */
	public static int topSection(int dimensionId) {
		return topY(dimensionId) >> 4;
	}

	/** True when a Y is inside the dimension's column at all. */
	public static boolean isInWorld(int dimensionId, int y) {
		return y >= bottomY(dimensionId) && y <= topY(dimensionId);
	}

	/**
	 * True when a Y is in the extension - outside vanilla's 0-127 but inside the dimension's column.
	 * This is the branch that decides whether a block read or write goes to vanilla storage or to
	 * {@link RetroExtendedSections}.
	 */
	public static boolean isExtendedY(int dimensionId, int y) {
		if (y >= VANILLA_BOTTOM_Y && y <= VANILLA_TOP_Y) {
			return false;
		}
		return isInWorld(dimensionId, y);
	}

	// --- world convenience --------------------------------------------------------------------

	public static int bottomY(World world) {
		return bottomY(world.dimension.id);
	}

	public static int topY(World world) {
		return topY(world.dimension.id);
	}

	public static boolean isExtended(World world) {
		return isExtended(world.dimension.id);
	}

	/**
	 * Reads a block at any Y in the dimension's column, routing out-of-range Y to the extension
	 * sections and everything else to vanilla. Returns 0 (air) outside the column entirely.
	 */
	public static int getBlock(World world, int x, int y, int z) {
		if (!isExtendedY(world.dimension.id, y)) {
			return isInWorld(world.dimension.id, y) ? world.getBlockId(x, y, z) : 0;
		}
		RetroExtendedSections sections = RetroExtendedSections.of(world, x, z);
		return sections == null ? 0 : sections.getBlock(x & 15, y, z & 15);
	}

	/** Metadata at any Y in the dimension's column. */
	public static int getMeta(World world, int x, int y, int z) {
		if (!isExtendedY(world.dimension.id, y)) {
			return isInWorld(world.dimension.id, y) ? world.getBlockMeta(x, y, z) : 0;
		}
		RetroExtendedSections sections = RetroExtendedSections.of(world, x, z);
		return sections == null ? 0 : sections.getMeta(x & 15, y, z & 15);
	}

	/**
	 * Writes a block at any Y in the dimension's column. Out-of-range Y goes to the extension sections,
	 * everything else through {@code world.setBlock} as usual. Returns false when the Y is outside the
	 * column.
	 */
	public static boolean setBlock(World world, int x, int y, int z, int blockId, int meta) {
		int dimensionId = world.dimension.id;
		if (!isInWorld(dimensionId, y)) {
			return false;
		}
		if (!isExtendedY(dimensionId, y)) {
			return world.setBlock(x, y, z, blockId, meta);
		}
		RetroExtendedSections sections = RetroExtendedSections.of(world, x, z);
		if (sections == null) {
			return false;
		}
		sections.setBlock(x & 15, y, z & 15, blockId, meta);
		net.minecraft.world.chunk.Chunk chunk = world.getChunk(x >> 4, z >> 4);
		if (chunk != null) {
			chunk.dirty = true;
		}
		return true;
	}
}
