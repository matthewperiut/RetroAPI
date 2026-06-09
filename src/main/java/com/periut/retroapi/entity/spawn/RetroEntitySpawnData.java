package com.periut.retroapi.entity.spawn;

import net.ornithemc.osl.networking.api.PacketBuffer;

import java.io.IOException;

/**
 * A non-living modded entity that supplies its own spawn data. Mirrors StationAPI's
 * {@code EntitySpawnDataProvider}. {@link #writeExtra}/{@link #readExtra} replace StationAPI's
 * {@code writeToMessage}/{@code readFromMessage} (the carrier is the raw OSL {@link PacketBuffer}).
 */
public interface RetroEntitySpawnData extends RetroSpawnDataProvider {

	/** Whether to include the full DataTracker blob in the spawn payload (mobs always do; entities opt in). */
	default boolean syncTrackerAtSpawn() {
		return false;
	}

	/** Write custom spawn fields after the standard payload. */
	default void writeExtra(PacketBuffer buf) throws IOException {
	}

	/** Read the custom spawn fields written by {@link #writeExtra}. */
	default void readExtra(PacketBuffer buf) throws IOException {
	}
}
