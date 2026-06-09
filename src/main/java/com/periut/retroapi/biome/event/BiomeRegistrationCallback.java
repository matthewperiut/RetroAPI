package com.periut.retroapi.biome.event;

import net.ornithemc.osl.core.api.events.Event;

/**
 * Fired during {@code RetroAPI.init()} (StationAPI absent) so mods can build their modded biomes via
 * {@link com.periut.retroapi.biome.BiomeBuilder}. Mirrors
 * {@link com.periut.retroapi.entity.event.EntityRegistrationCallback}; OSL {@code Event<T>}, never
 * StationAPI UnsafeEvents.
 */
public class BiomeRegistrationCallback {
	public static final Event<Runnable> EVENT = Event.runnable();
}
