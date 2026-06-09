package com.periut.retroapi.entity.spawn;

import net.ornithemc.osl.networking.api.PacketBuffer;

import java.io.IOException;

/**
 * A living modded entity (mob) that supplies its own spawn data. Mirrors StationAPI's
 * {@code MobSpawnDataProvider}: the spawn payload always carries yaw/pitch and the full DataTracker blob.
 */
public interface RetroMobSpawnData extends RetroSpawnDataProvider {

	/** Write custom spawn fields after the standard mob payload. */
	default void writeExtra(PacketBuffer buf) throws IOException {
	}

	/** Read the custom spawn fields written by {@link #writeExtra}. */
	default void readExtra(PacketBuffer buf) throws IOException {
	}
}
