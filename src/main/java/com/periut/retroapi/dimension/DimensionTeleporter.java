package com.periut.retroapi.dimension;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.dimension.PortalForcer;

/**
 * Environment-specific back end that actually moves a player between dimensions. The signature uses
 * only environment-neutral classes ({@link PlayerEntity}, {@link PortalForcer}) so this interface and
 * {@link DimensionHelper} load on both the client and the dedicated server. The concrete
 * implementation - which touches server-only ({@code ServerPlayerEntity}, {@code MinecraftServer}) or
 * client-only ({@code Minecraft}) classes - is registered per environment via
 * {@link DimensionHelper#setTeleporter}, so the wrong side never loads it.
 */
public interface DimensionTeleporter {
	/**
	 * @param player      the player to move
	 * @param targetDim   the destination dimension serial id (overworld {@code 0} or a modded id)
	 * @param moddedDim   the modded dimension serial id of this portal (for the travel message / direction)
	 * @param coordFactor multiply the player's x/z by this when repositioning ({@code 1.0} = no scaling)
	 * @param agent       the portal forcer used to find/build the arrival portal
	 */
	void teleport(PlayerEntity player, int targetDim, int moddedDim, double coordFactor, PortalForcer agent);
}
