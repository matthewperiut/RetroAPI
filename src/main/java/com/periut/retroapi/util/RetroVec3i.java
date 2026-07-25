package com.periut.retroapi.util;

import net.minecraft.block.Block;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * An immutable integer vector - the block position type b1.7.3 never had.
 *
 * <p>Beta passes positions around as three loose {@code int}s, which is why mod code fills up with
 * {@code world.setBlock(x + dx, y + dy, z + dz, ...)} and hand-written neighbor offsets. This is the
 * modern {@code Vec3i}/{@code BlockPos} shape: values are never mutated, every operation returns a new
 * vector, and the world helpers let a position read or write itself.
 *
 * <pre>
 * RetroVec3i front = RetroVec3i.of(x, y, z).offset(facing);      // one block toward `facing`
 * if (front.blockId(world) == 0) front.setBlock(world, Block.TORCH.id);
 *
 * RetroVec3i corner = pos.add(1, 0, 1).multiply(2).subtract(RetroVec3i.UP);
 * </pre>
 *
 * <p>Cheap to make and safe to share: two vectors with the same coordinates are {@link #equals equal} and
 * hash alike, so they work as {@code HashMap} keys for multiblock parts, machine networks, and caches.
 */
public final class RetroVec3i {

	public static final RetroVec3i ZERO = new RetroVec3i(0, 0, 0);
	public static final RetroVec3i UP = new RetroVec3i(0, 1, 0);
	public static final RetroVec3i DOWN = new RetroVec3i(0, -1, 0);
	public static final RetroVec3i NORTH = new RetroVec3i(0, 0, -1);
	public static final RetroVec3i SOUTH = new RetroVec3i(0, 0, 1);
	public static final RetroVec3i WEST = new RetroVec3i(-1, 0, 0);
	public static final RetroVec3i EAST = new RetroVec3i(1, 0, 0);

	public final int x;
	public final int y;
	public final int z;

	public RetroVec3i(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static RetroVec3i of(int x, int y, int z) {
		return new RetroVec3i(x, y, z);
	}

	/** The block position containing a point in world space (floor, so negatives round the right way). */
	public static RetroVec3i of(double x, double y, double z) {
		return new RetroVec3i(floor(x), floor(y), floor(z));
	}

	/** The block position an entity is standing in. */
	public static RetroVec3i of(net.minecraft.entity.Entity entity) {
		return of(entity.x, entity.y, entity.z);
	}

	private static int floor(double value) {
		int i = (int) value;
		return value < i ? i - 1 : i;
	}

	// --- arithmetic -------------------------------------------------------------------------------

	public RetroVec3i add(int dx, int dy, int dz) {
		return (dx | dy | dz) == 0 ? this : new RetroVec3i(x + dx, y + dy, z + dz);
	}

	public RetroVec3i add(RetroVec3i other) {
		return add(other.x, other.y, other.z);
	}

	public RetroVec3i subtract(int dx, int dy, int dz) {
		return add(-dx, -dy, -dz);
	}

	public RetroVec3i subtract(RetroVec3i other) {
		return add(-other.x, -other.y, -other.z);
	}

	/** Scales every component. */
	public RetroVec3i multiply(int scale) {
		return scale == 1 ? this : new RetroVec3i(x * scale, y * scale, z * scale);
	}

	/** Component-wise multiply. */
	public RetroVec3i multiply(int sx, int sy, int sz) {
		return new RetroVec3i(x * sx, y * sy, z * sz);
	}

	public RetroVec3i negate() {
		return new RetroVec3i(-x, -y, -z);
	}

	public RetroVec3i up() { return add(0, 1, 0); }
	public RetroVec3i up(int n) { return add(0, n, 0); }
	public RetroVec3i down() { return add(0, -1, 0); }
	public RetroVec3i down(int n) { return add(0, -n, 0); }
	public RetroVec3i north() { return add(0, 0, -1); }
	public RetroVec3i north(int n) { return add(0, 0, -n); }
	public RetroVec3i south() { return add(0, 0, 1); }
	public RetroVec3i south(int n) { return add(0, 0, n); }
	public RetroVec3i west() { return add(-1, 0, 0); }
	public RetroVec3i west(int n) { return add(-n, 0, 0); }
	public RetroVec3i east() { return add(1, 0, 0); }
	public RetroVec3i east(int n) { return add(n, 0, 0); }

	/** One block along {@code direction}. */
	public RetroVec3i offset(RetroDirection direction) {
		return offset(direction, 1);
	}

	/** {@code distance} blocks along {@code direction} (negative walks backwards). */
	public RetroVec3i offset(RetroDirection direction, int distance) {
		return add(direction.offsetX * distance, direction.offsetY * distance, direction.offsetZ * distance);
	}

	/** One block along a horizontal facing. */
	public RetroVec3i offset(com.periut.retroapi.state.RetroFacing facing) {
		return offset(facing.toDirection(), 1);
	}

	public RetroVec3i offset(com.periut.retroapi.state.RetroFacing facing, int distance) {
		return offset(facing.toDirection(), distance);
	}

	/**
	 * Interprets this vector as a rotation-relative offset and rotates it around Y so that "north"
	 * becomes {@code facing} - the move that turns one hand-authored multiblock pattern into all four
	 * orientations. {@code (0,0,-1)} (north) rotated to EAST is {@code (1,0,0)}.
	 */
	public RetroVec3i rotateTo(com.periut.retroapi.state.RetroFacing facing) {
		switch (facing) {
			case NORTH: return this;
			case SOUTH: return new RetroVec3i(-x, y, -z);
			case EAST:  return new RetroVec3i(-z, y, x);
			default:    return new RetroVec3i(z, y, -x); // WEST
		}
	}

	// --- distances --------------------------------------------------------------------------------

	/** Squared straight-line distance; compare against {@code r * r} to avoid a square root. */
	public int distanceSquared(RetroVec3i other) {
		int dx = x - other.x, dy = y - other.y, dz = z - other.z;
		return dx * dx + dy * dy + dz * dz;
	}

	public double distance(RetroVec3i other) {
		return Math.sqrt(distanceSquared(other));
	}

	/** Steps along the axes (the "how many block moves apart" distance). */
	public int manhattanDistance(RetroVec3i other) {
		return Math.abs(x - other.x) + Math.abs(y - other.y) + Math.abs(z - other.z);
	}

	/** True when the other position is within {@code radius} blocks on every axis (a cube test). */
	public boolean isWithin(RetroVec3i other, int radius) {
		return Math.abs(x - other.x) <= radius
			&& Math.abs(y - other.y) <= radius
			&& Math.abs(z - other.z) <= radius;
	}

	// --- world access -----------------------------------------------------------------------------

	/** The block id at this position. */
	public int blockId(BlockView world) {
		return world.getBlockId(x, y, z);
	}

	/** The block at this position, or null for air / an unknown id. */
	public Block block(BlockView world) {
		int id = world.getBlockId(x, y, z);
		return id > 0 && id < Block.BLOCKS.length ? Block.BLOCKS[id] : null;
	}

	/** The vanilla metadata nibble at this position. For full states use {@link #state}. */
	public int meta(BlockView world) {
		return world.getBlockMeta(x, y, z);
	}

	/** The flattened RetroAPI state at this position (null for air). */
	public com.periut.retroapi.state.RetroBlockState state(BlockView world) {
		return com.periut.retroapi.state.RetroStates.get(world, x, y, z);
	}

	/** True when this position holds the given block. */
	public boolean isBlock(BlockView world, Block block) {
		return block != null && world.getBlockId(x, y, z) == block.id;
	}

	public boolean isAir(BlockView world) {
		return world.getBlockId(x, y, z) == 0;
	}

	public boolean setBlock(World world, int blockId) {
		return world.setBlock(x, y, z, blockId);
	}

	public boolean setBlock(World world, int blockId, int meta) {
		return world.setBlock(x, y, z, blockId, meta);
	}

	public boolean setBlock(World world, Block block) {
		return world.setBlock(x, y, z, block == null ? 0 : block.id);
	}

	/** Writes a full flattened state (nibble + sidecar bits) here. */
	public void setState(World world, com.periut.retroapi.state.RetroBlockState state) {
		com.periut.retroapi.state.RetroStates.set(world, x, y, z, state);
	}

	/** The block entity here, or null. */
	public net.minecraft.block.entity.BlockEntity blockEntity(BlockView world) {
		return world.getBlockEntity(x, y, z);
	}

	// --- identity ---------------------------------------------------------------------------------

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		return o instanceof RetroVec3i other && other.x == x && other.y == y && other.z == z;
	}

	@Override
	public int hashCode() {
		return (y + z * 31) * 31 + x;
	}

	@Override
	public String toString() {
		return "(" + x + ", " + y + ", " + z + ")";
	}
}
