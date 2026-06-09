package com.periut.retroapi.registry;

/**
 * Single source of truth for RetroAPI's id-space constants. These are deliberately centralized
 * because the values are <em>coupled across multiple mixins</em>: an encode site and a decode
 * site (or several array-sizing sites) must agree exactly, and a silent drift between them is a
 * whole class of bug (e.g. a break-particle packing where the send side shifted metadata by one
 * amount and the receive side unpacked by another, corrupting cross-block particles).
 *
 * <p>All fields are compile-time constants, so referencing them from a mixin inlines the literal
 * at compile time - no runtime class-load of {@code RetroIds} is triggered, which keeps the very
 * early {@code Block.<clinit>} array-expansion mixins safe.
 *
 * <h2>Block-id capacity ({@link #BLOCK_ID_CAPACITY})</h2>
 * RetroAPI blocks live at ids &ge; 256, and every block also needs a {@code BlockItem} slot at
 * {@code Item.ITEMS[blockId]} - so block ids and item ids share one numeric space whose size is
 * {@code Item.ITEMS.length} (vanilla = 32000). That shared space is therefore the real ceiling,
 * and it conveniently fits the 16-bit ({@code & 0xFFFF}, max 65535) block-id fields in RetroAPI's
 * chunk/block-update packets, so raising to it needs no wire-protocol change.
 *
 * <p>Remaining ceilings <em>above</em> this value, if blocks ever need to exceed 32000:
 * <ul>
 *   <li>{@code Item.ITEMS} (size 32000) would have to be expanded too - block {@code BlockItem}s index it.</li>
 *   <li>The packet block-id fields are {@code short} read as {@code & 0xFFFF} (cap 65535); going past that
 *       means widening those {@code short[]} to {@code int[]} (a wire-format change).</li>
 * </ul>
 * Per-block blockstate count is a <em>separate</em> axis (see {@code RetroStateDefinition}: 4096 states /
 * 12 xmeta storage bits) and is unrelated to this id capacity.
 */
public final class RetroIds {
	private RetroIds() {}

	/**
	 * Exclusive upper bound for block ids = length the {@code Block.*} / FireBlock chance / Stats
	 * arrays are grown to. Equal to vanilla {@code Item.ITEMS.length} (the shared block+item id space).
	 * Valid block ids are {@code 256 .. BLOCK_ID_CAPACITY - 1}.
	 */
	public static final int BLOCK_ID_CAPACITY = 32000;

	// --- worldEvent(2001) block-break packing: data = blockId + metadata * BREAK_EVENT_META_MULTIPLIER ---
	// This MUST match StationAPI's flattening layout (station-flattening-v0 InteractionManagerMixin /
	// ServerPlayerInteractionManagerMixin encode it, WorldRendererMixin decodes it) so the two mods agree
	// under compat: blockId in the low 28 bits, metadata in bits 28+. 1 << 28 == 268435456.

	/** Multiplier the encode side packs metadata by (replaces vanilla's {@code 256}). {@code = 1 << 28}. */
	public static final int BREAK_EVENT_META_MULTIPLIER = 1 << 28;
	/** Mask the decode side applies to recover blockId (low 28 bits). */
	public static final int BREAK_EVENT_ID_MASK = 0x0FFFFFFF;
	/** Right-shift the decode side applies to recover metadata. */
	public static final int BREAK_EVENT_META_SHIFT = 28;
}
