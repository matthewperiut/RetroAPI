package com.periut.retroapi.world;

import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * BlockView helpers. The one everybody needs: {@link #unwrap}, because chunk rendering
 * hands renderers (and anything they call, like color providers or mixin handlers) a
 * {@code WorldRegion} wrapper rather than the World itself, so naive
 * {@code view instanceof World} checks silently fail during rendering.
 */
public final class RetroWorlds {

	private RetroWorlds() {}

	/**
	 * The World behind a BlockView: the view itself, the world inside a render-time
	 * {@code WorldRegion}, or null for world-less views (item-form rendering).
	 */
	public static World unwrap(BlockView view) {
		if (view instanceof World) {
			return (World) view;
		}
		if (view == null) {
			return null;
		}
		// WorldRegion is a client rendering class; keep its reference in a holder class
		// that only loads when a non-World view actually shows up (never on a dedicated
		// server, where every BlockView IS the World).
		return RegionUnwrap.tryUnwrap(view);
	}

	private static final class RegionUnwrap {
		static World tryUnwrap(BlockView view) {
			if (view instanceof net.minecraft.world.WorldRegion) {
				return ((com.periut.retroapi.mixin.client.WorldRegionAccessor) view).retroapi$getWorld();
			}
			return null;
		}
	}
}
