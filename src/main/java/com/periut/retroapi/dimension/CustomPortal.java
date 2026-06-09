package com.periut.retroapi.dimension;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.dimension.PortalForcer;
import net.ornithemc.osl.core.api.util.NamespacedIdentifier;

/**
 * A portal block that sends a player to a registered modded dimension. Implementors supply the
 * destination identifier, an optional coordinate scale (default 1 - no scaling, unlike vanilla's
 * 8x nether), and the {@link PortalForcer} that finds/builds the arrival portal. The default
 * {@link #switchDimension} routes everything through {@link DimensionHelper}. API-compatible with
 * StationAPI's {@code CustomPortal} so a consuming mod (e.g. an Aether portal) barely changes.
 */
public interface CustomPortal extends TeleportationManager {
	NamespacedIdentifier getDimension(PlayerEntity player);

	default double getDimensionScale(PlayerEntity player) {
		return 1.0;
	}

	PortalForcer getTravelAgent(PlayerEntity player);

	@Override
	default void switchDimension(PlayerEntity player) {
		DimensionHelper.switchDimension(player, getDimension(player), getDimensionScale(player), getTravelAgent(player));
	}
}
