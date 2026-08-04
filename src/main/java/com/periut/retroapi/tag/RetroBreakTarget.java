package com.periut.retroapi.tag;

import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.BlockView;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Where the block a player is currently breaking actually is.
 *
 * <p>Beta's harvest hooks - {@code PlayerEntity.canHarvest(Block)} and
 * {@code PlayerEntity.getBlockBreakingSpeed(Block)} - are handed the block TYPE and nothing else. Every
 * state of a block is the same {@code Block} object, so a tool that wants to answer "diamond-tier on lit
 * ore, wood-tier on unlit" has no way to look: no coordinates, no metadata, no world. That is the wall
 * {@link RetroToolTier.Contextual} could not get past.
 *
 * <p>The interaction managers DO know the position - they are the ones calling into the harvest hooks -
 * so RetroAPI records it there and hands it back here. Reads are validated against the world: a target is
 * only returned while the block at those coordinates is still the block being asked about, so a stale
 * record from a break the player walked away from can never be mistaken for the current one.
 *
 * <p>This is plumbing for {@link RetroToolTier.Positional}; most code should declare a positional tier and
 * let RetroAPI do the lookup.
 */
public final class RetroBreakTarget {

	private static final Map<PlayerEntity, RetroBreakTarget> CURRENT = new WeakHashMap<>();

	private final BlockView world;
	private final int x;
	private final int y;
	private final int z;

	private RetroBreakTarget(BlockView world, int x, int y, int z) {
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/** Records what this player is breaking. Called by the interaction-manager mixins. */
	public static void set(PlayerEntity player, BlockView world, int x, int y, int z) {
		if (player == null || world == null) {
			return;
		}
		synchronized (CURRENT) {
			CURRENT.put(player, new RetroBreakTarget(world, x, y, z));
		}
	}

	/** Forgets what this player was breaking. */
	public static void clear(PlayerEntity player) {
		if (player == null) {
			return;
		}
		synchronized (CURRENT) {
			CURRENT.remove(player);
		}
	}

	/**
	 * What this player is breaking, or null when there is no current break. Not validated against a
	 * block; prefer {@link #current(PlayerEntity, Block)}.
	 */
	public static RetroBreakTarget current(PlayerEntity player) {
		if (player == null) {
			return null;
		}
		synchronized (CURRENT) {
			return CURRENT.get(player);
		}
	}

	/**
	 * What this player is breaking, but only if the block still standing there is {@code block} - so a
	 * leftover record can never answer for the wrong block. Null when there is no current break, or the
	 * world has moved on.
	 */
	public static RetroBreakTarget current(PlayerEntity player, Block block) {
		RetroBreakTarget target = current(player);
		if (target == null || block == null) {
			return null;
		}
		return target.world.getBlockId(target.x, target.y, target.z) == block.id ? target : null;
	}

	/**
	 * The break currently in progress on {@code block}, by whichever player is making it.
	 *
	 * <p>{@link #current(PlayerEntity, Block)} wants the player, and the mining questions beta asks -
	 * breaking speed, the correct-tool check - are handed a block and nothing else, no player and no
	 * coordinates. Only one break can be underway per player and the record is validated against the
	 * world, so a scan for the one standing on the right block answers correctly for whoever is doing it.
	 */
	public static RetroBreakTarget currentAny(Block block) {
		if (block == null) {
			return null;
		}
		synchronized (CURRENT) {
			for (RetroBreakTarget target : CURRENT.values()) {
				if (target != null && target.world.getBlockId(target.x, target.y, target.z) == block.id) {
					return target;
				}
			}
		}
		return null;
	}

	/**
	 * The break in progress at a position, by whichever player is making it. Lets code that has
	 * coordinates but no world, which several of beta's client-side break hooks are, still reach one.
	 */
	public static RetroBreakTarget currentAt(int x, int y, int z) {
		synchronized (CURRENT) {
			for (RetroBreakTarget target : CURRENT.values()) {
				if (target != null && target.x == x && target.y == y && target.z == z) {
					return target;
				}
			}
		}
		return null;
	}

	public BlockView world() {
		return world;
	}

	public int x() {
		return x;
	}

	public int y() {
		return y;
	}

	public int z() {
		return z;
	}
}
