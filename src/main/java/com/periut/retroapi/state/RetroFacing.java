package com.periut.retroapi.state;

import com.periut.retroapi.util.RetroDirection;
import com.periut.retroapi.util.RetroVec3i;

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

	// --- geometry helpers -------------------------------------------------------------------------
	// The point of these: a block that "does something in front of itself" should never have to
	// hand-write a switch over four directions and their ± x/z offsets again.

	/** The block offset along this facing: -1, 0 or +1. */
	public int offsetX() {
		return toDirection().offsetX;
	}

	/** The block offset along this facing: -1, 0 or +1. */
	public int offsetZ() {
		return toDirection().offsetZ;
	}

	/** This facing as an offset vector, e.g. {@code pos.add(facing.vector())}. */
	public RetroVec3i vector() {
		return toDirection().vector();
	}

	/** The position one block along this facing from {@code (x, y, z)}. */
	public RetroVec3i offset(int x, int y, int z) {
		return RetroVec3i.of(x, y, z).offset(this);
	}

	/** The position {@code distance} blocks along this facing from {@code (x, y, z)}. */
	public RetroVec3i offset(int x, int y, int z, int distance) {
		return RetroVec3i.of(x, y, z).offset(this, distance);
	}

	/** The facing pointing the other way. */
	public RetroFacing opposite() {
		switch (this) {
			case NORTH: return SOUTH;
			case SOUTH: return NORTH;
			case WEST: return EAST;
			default: return WEST;
		}
	}

	/** A quarter turn clockwise seen from above: north -&gt; east -&gt; south -&gt; west. */
	public RetroFacing rotateRight() {
		switch (this) {
			case NORTH: return EAST;
			case EAST: return SOUTH;
			case SOUTH: return WEST;
			default: return NORTH;
		}
	}

	/** A quarter turn counter-clockwise seen from above: north -&gt; west -&gt; south -&gt; east. */
	public RetroFacing rotateLeft() {
		switch (this) {
			case NORTH: return WEST;
			case WEST: return SOUTH;
			case SOUTH: return EAST;
			default: return NORTH;
		}
	}

	/** The vanilla face index of this facing (2 north, 3 south, 4 west, 5 east). */
	public int face() {
		return toDirection().face();
	}

	/** This facing as a full 6-way {@link RetroDirection}. */
	public RetroDirection toDirection() {
		switch (this) {
			case NORTH: return RetroDirection.NORTH;
			case SOUTH: return RetroDirection.SOUTH;
			case WEST: return RetroDirection.WEST;
			default: return RetroDirection.EAST;
		}
	}
}
