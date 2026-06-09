package com.periut.retroapi.entity.event;

import net.ornithemc.osl.core.api.events.Event;

/**
 * Fired during {@code RetroAPI.init()} so mods can register their entities (Fabric-API style, flat).
 * Mirrors {@link com.periut.retroapi.register.block.event.BlockRegistrationCallback}; built on OSL's
 * {@code Event<T>}, never StationAPI UnsafeEvents.
 */
public class EntityRegistrationCallback {
	public static final Event<Runnable> EVENT = Event.runnable();
}
