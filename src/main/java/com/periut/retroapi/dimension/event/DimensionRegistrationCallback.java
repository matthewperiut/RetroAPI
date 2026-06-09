package com.periut.retroapi.dimension.event;

import net.ornithemc.osl.core.api.events.Event;

/**
 * Fired during {@code RetroAPI.init()} (StationAPI absent) so mods register their dimensions flat,
 * Fabric-API style - all dimensions are known before any world loads (so the future array-expansion
 * mixins see a complete registry). Mirrors {@link com.periut.retroapi.entity.event.EntityRegistrationCallback};
 * built on OSL {@code Event<T>}, never StationAPI UnsafeEvents.
 */
public class DimensionRegistrationCallback {
	public static final Event<Runnable> EVENT = Event.runnable();
}
