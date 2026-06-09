package com.periut.retroapi.entity.spawn;

import net.minecraft.entity.Entity;

/**
 * A modded entity with an owner (e.g. projectiles), whose owner id + velocity are sent at spawn so the
 * client can resolve the owner and reproduce the trajectory. Mirrors StationAPI's {@code HasOwner}.
 */
public interface RetroHasOwner {
	Entity getOwner();

	void setOwner(Entity owner);
}
