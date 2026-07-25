package com.periut.retroapi.util;

import com.periut.retroapi.state.RetroEnumProperty;
import com.periut.retroapi.state.RetroFacing;
import net.minecraft.entity.LivingEntity;

/**
 * All six directions, with the offsets, rotations and face indices beta makes you write by hand.
 *
 * <p>The constants serialize to their lowercase names ({@code UP -> "up"}), so a blockstate JSON's
 * {@code "facing=up"} keys match, and {@link #PROPERTY} is the ready-made 6-way {@code "facing"} property -
 * the vertical-capable sibling of {@link RetroFacing#PROPERTY}. {@code RetroBlockAccess.facingAll()}
 * declares it and aims the block at its placer, including straight up or down.
 *
 * <pre>
 * RetroDirection back = facing.opposite();
 * RetroVec3i behind   = pos.offset(back);          // no ± x/y/z arithmetic anywhere
 * int frontFace       = facing.face();             // the face index getTexture(side, …) is asked about
 * </pre>
 *
 * <p>{@link #face()} returns the index vanilla uses for block faces and textures
 * (0 down, 1 up, 2 north, 3 south, 4 west, 5 east); {@link #fromFace(int)} goes back.
 */
public enum RetroDirection {
	DOWN(0, 0, -1, 0),
	UP(1, 0, 1, 0),
	NORTH(2, 0, 0, -1),
	SOUTH(3, 0, 0, 1),
	WEST(4, -1, 0, 0),
	EAST(5, 1, 0, 0);

	/** The shared 6-way {@code "facing"} property; use with {@code .states(...)} or {@code .facingAll()}. */
	public static final RetroEnumProperty<RetroDirection> PROPERTY =
		RetroEnumProperty.of("facing", RetroDirection.class);

	private static final RetroDirection[] BY_FACE = new RetroDirection[6];
	/** Clockwise horizontal ring, viewed from above: north -> east -> south -> west. */
	private static final RetroDirection[] CLOCKWISE = {NORTH, EAST, SOUTH, WEST};

	static {
		for (RetroDirection direction : values()) {
			BY_FACE[direction.face] = direction;
		}
	}

	private final int face;
	public final int offsetX;
	public final int offsetY;
	public final int offsetZ;

	RetroDirection(int face, int offsetX, int offsetY, int offsetZ) {
		this.face = face;
		this.offsetX = offsetX;
		this.offsetY = offsetY;
		this.offsetZ = offsetZ;
	}

	/** The vanilla face index: 0 down, 1 up, 2 north, 3 south, 4 west, 5 east. */
	public int face() {
		return face;
	}

	/** The direction for a vanilla face index; out-of-range values give {@link #DOWN}. */
	public static RetroDirection fromFace(int face) {
		return face >= 0 && face < BY_FACE.length ? BY_FACE[face] : DOWN;
	}

	/** The offset as a vector, for {@code pos.add(direction.vector())} style math. */
	public RetroVec3i vector() {
		return new RetroVec3i(offsetX, offsetY, offsetZ);
	}

	public RetroDirection opposite() {
		switch (this) {
			case DOWN: return UP;
			case UP: return DOWN;
			case NORTH: return SOUTH;
			case SOUTH: return NORTH;
			case WEST: return EAST;
			default: return WEST;
		}
	}

	/** True for the four compass directions (everything except {@link #UP} / {@link #DOWN}). */
	public boolean isHorizontal() {
		return offsetY == 0;
	}

	/** 'X', 'Y' or 'Z' - the axis this direction runs along. */
	public char axis() {
		if (offsetX != 0) return 'X';
		return offsetY != 0 ? 'Y' : 'Z';
	}

	/**
	 * A quarter turn clockwise around the Y axis, seen from above (north -&gt; east). Vertical
	 * directions are returned unchanged, since they have no horizontal rotation.
	 */
	public RetroDirection rotateRight() {
		if (!isHorizontal()) return this;
		return CLOCKWISE[(horizontalIndex() + 1) & 3];
	}

	/** A quarter turn counter-clockwise around Y (north -&gt; west). */
	public RetroDirection rotateLeft() {
		if (!isHorizontal()) return this;
		return CLOCKWISE[(horizontalIndex() + 3) & 3];
	}

	/** Position in the clockwise horizontal ring (north 0, east 1, south 2, west 3); -1 if vertical. */
	public int horizontalIndex() {
		for (int i = 0; i < CLOCKWISE.length; i++) {
			if (CLOCKWISE[i] == this) return i;
		}
		return -1;
	}

	/** The matching horizontal {@link RetroFacing}, or null for {@link #UP} / {@link #DOWN}. */
	public RetroFacing toFacing() {
		switch (this) {
			case NORTH: return RetroFacing.NORTH;
			case SOUTH: return RetroFacing.SOUTH;
			case WEST: return RetroFacing.WEST;
			case EAST: return RetroFacing.EAST;
			default: return null;
		}
	}

	/**
	 * The horizontal direction an entity looking along {@code yaw} would place a block facing THEM
	 * (the furnace/chest rule, same as {@link RetroFacing#fromYaw}).
	 */
	public static RetroDirection fromYaw(float yaw) {
		return RetroFacing.fromYaw(yaw).toDirection();
	}

	/**
	 * The direction a block placed by this entity should face, including vertically: looking steeply
	 * down or up gives {@link #UP} / {@link #DOWN} (the piston/dispenser rule - the block's face points
	 * back at the placer), otherwise the horizontal facing.
	 */
	public static RetroDirection fromPlacer(LivingEntity placer) {
		if (placer == null) return NORTH;
		float pitch = placer.pitch;
		if (pitch < -60.0F) return UP;      // looking up: face up
		if (pitch > 60.0F) return DOWN;     // looking down: face down
		return fromYaw(placer.yaw);
	}

	/** The direction pointing most strongly along a delta, e.g. from a block to the entity that hit it. */
	public static RetroDirection nearest(double dx, double dy, double dz) {
		double ax = Math.abs(dx), ay = Math.abs(dy), az = Math.abs(dz);
		if (ax >= ay && ax >= az) return dx > 0 ? EAST : WEST;
		if (ay >= az) return dy > 0 ? UP : DOWN;
		return dz > 0 ? SOUTH : NORTH;
	}
}
