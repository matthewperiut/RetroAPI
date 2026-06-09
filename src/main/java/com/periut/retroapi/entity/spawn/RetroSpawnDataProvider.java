package com.periut.retroapi.entity.spawn;

import net.ornithemc.osl.core.api.util.NamespacedIdentifier;

/**
 * Base for modded entities that spawn over RetroAPI's OSL channels (instead of vanilla spawn packets,
 * which throw {@code IllegalArgumentException} for unknown entity types). Mirrors StationAPI's
 * {@code StationSpawnDataProvider}. Implement {@link RetroEntitySpawnData} (non-living) or
 * {@link RetroMobSpawnData} (living), not this directly.
 */
public interface RetroSpawnDataProvider {
	/** The handler id; written on the wire and used client-side to pick the factory. */
	NamespacedIdentifier getHandlerId();
}
