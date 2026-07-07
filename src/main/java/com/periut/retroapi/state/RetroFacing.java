package com.periut.retroapi.state;

/**
 * The four horizontal directions a block can face, built in so mods stop hand-writing their own
 * {@code Facing} enum. Constants serialize to their lowercase names ({@code NORTH -> "north"}), which is
 * exactly what a blockstate JSON's {@code "facing=north"} keys match. The first-declared constant
 * ({@code NORTH}) is the property's default state.
 *
 * <p>Pair it with the ready-made {@link #PROPERTY} (named {@code "facing"}) via {@code .states(...)}, or
 * let {@code RetroBlockAccess.facing()} both declare it and orient the block toward the placer on
 * placement (the furnace/chest behavior), so you write no {@code onPlaced} at all.</p>
 */
public enum RetroFacing {
	NORTH,
	SOUTH,
	WEST,
	EAST;

	/** The shared {@code "facing"} property; use it as a state and in blockstate JSON keys. */
	public static final RetroEnumProperty<RetroFacing> PROPERTY = RetroEnumProperty.of("facing", RetroFacing.class);

	/**
	 * The direction a block should face when placed by an entity looking along {@code yaw}. A block faces
	 * the placer, so this is the OPPOSITE of the way they look (furnace/chest rule). Yaw 0 is south in beta.
	 */
	public static RetroFacing fromYaw(float yaw) {
		int quadrant = Math.round(yaw / 90.0F) & 3;
		switch (quadrant) {
			case 0: return NORTH; // looking south, face north
			case 1: return EAST;
			case 2: return SOUTH;
			default: return WEST;
		}
	}
}
