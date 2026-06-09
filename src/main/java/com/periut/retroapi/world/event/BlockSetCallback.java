package com.periut.retroapi.world.event;

import net.minecraft.world.World;
import net.ornithemc.osl.core.api.events.Event;

/**
 * Fired whenever a block is set in the world ({@code World.setBlock} /
 * {@code setBlockWithoutNotifyingNeighbors}, both overloads). A listener may cancel the
 * set by returning {@code true} - typically to replace it with something else (e.g. a
 * water-on-glowstone portal). Mirrors StationAPI's {@code BlockSetEvent} as a lean,
 * Fabric-API-style callback.
 */
public final class BlockSetCallback {

	@FunctionalInterface
	public interface Listener {
		/** @return true to cancel the block set (the world is left untouched by vanilla). */
		boolean onBlockSet(World world, int x, int y, int z, int blockId, int meta);
	}

	public static final Event<Listener> EVENT = Event.of(listeners -> (world, x, y, z, blockId, meta) -> {
		for (Listener listener : listeners) {
			if (listener.onBlockSet(world, x, y, z, blockId, meta)) {
				return true;
			}
		}
		return false;
	});

	private BlockSetCallback() {}
}
