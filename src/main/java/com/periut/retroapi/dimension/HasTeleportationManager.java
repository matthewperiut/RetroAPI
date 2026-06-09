package com.periut.retroapi.dimension;

/**
 * Duck-type interface mixed into {@code PlayerEntity} so a portal can stash the
 * {@link TeleportationManager} it wants to run on the player. Read back by the dimension-change
 * mixins. API-compatible with StationAPI's same-named interface.
 */
public interface HasTeleportationManager {
	TeleportationManager getTeleportationManager();

	void setTeleportationManager(TeleportationManager manager);
}
