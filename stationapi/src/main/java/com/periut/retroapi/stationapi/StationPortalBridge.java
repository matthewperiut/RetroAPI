package com.periut.retroapi.stationapi;

import com.periut.retroapi.dimension.CustomPortal;
import net.minecraft.entity.player.PlayerEntity;
import net.modificationstation.stationapi.api.entity.HasTeleportationManager;
import net.modificationstation.stationapi.api.util.Identifier;
import net.modificationstation.stationapi.api.world.dimension.DimensionHelper;
import net.modificationstation.stationapi.api.world.dimension.TeleportationManager;

/**
 * Bridges a RetroAPI portal onto StationAPI's dimension-teleport system. Under StationAPI, RetroAPI's own
 * dimension-change mixins are disabled and StationAPI instead redirects the player's dimension switch to
 * {@code player.getTeleportationManager().switchDimension(...)} (its OWN
 * {@link net.modificationstation.stationapi.api.world.dimension.TeleportationManager}). A RetroAPI portal
 * only attaches its (RetroAPI) manager, so StationAPI's slot is null -> NPE. So whenever a RetroAPI portal
 * attaches a manager to the player, mirror it onto the player's StationAPI slot with a bridge that runs the
 * same transition through StationAPI's {@link DimensionHelper} - which owns the dimension worlds under
 * StationAPI (the RetroAPI dimension is forwarded into StationAPI's {@code DimensionRegistry} by
 * {@code StationAPIRegistryForwarder}, so the destination resolves).
 *
 * <p>Referenced only from RetroAPI's {@code dimension.PlayerEntityMixin} behind an {@code isModLoaded}
 * gate, so this class (and its StationAPI imports) never loads without StationAPI.
 */
public final class StationPortalBridge {
	private StationPortalBridge() {}

	/** Mirror a RetroAPI teleport manager onto the player's StationAPI TeleportationManager slot. */
	public static void attach(PlayerEntity player, com.periut.retroapi.dimension.TeleportationManager retroManager) {
		HasTeleportationManager holder = (HasTeleportationManager) player;
		holder.setTeleportationManager(retroManager == null ? null : new Bridge(retroManager));
	}

	/** A StationAPI TeleportationManager that defers to a RetroAPI CustomPortal via StationAPI's DimensionHelper. */
	private record Bridge(com.periut.retroapi.dimension.TeleportationManager retro) implements TeleportationManager {
		@Override
		public void switchDimension(PlayerEntity player) {
			if (retro instanceof CustomPortal portal) {
				DimensionHelper.switchDimension(player,
					Identifier.of(portal.getDimension(player).toString()),
					portal.getDimensionScale(player),
					portal.getTravelAgent(player));
			} else {
				// Non-portal manager: best-effort RetroAPI path (its own DimensionHelper is disabled under
				// StationAPI, so this only does something useful for CustomPortal-based teleports).
				retro.switchDimension(player);
			}
		}
	}
}
