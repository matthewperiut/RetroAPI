package com.periut.retroapi.network;

import net.ornithemc.osl.core.api.util.NamespacedIdentifier;
import net.ornithemc.osl.networking.api.ChannelIdentifiers;
import net.ornithemc.osl.networking.api.ChannelRegistry;

public class RetroAPINetworking {
	public static final NamespacedIdentifier ID_SYNC_CHANNEL =
		ChannelRegistry.register(ChannelIdentifiers.from("retroapi", "id_sync"), true, false);
	public static final NamespacedIdentifier CHUNK_EXTENDED_CHANNEL =
		ChannelRegistry.register(ChannelIdentifiers.from("retroapi", "chunk_ext"), true, false);

	// Entity spawn channels (server -> client). Modded entities are spawned via these instead of vanilla
	// spawn packets; living vs non-living use separate channels to mirror StationAPI's split cleanly.
	public static final NamespacedIdentifier ENTITY_SPAWN_CHANNEL =
		ChannelRegistry.register(ChannelIdentifiers.from("retroapi", "entity_spawn"), true, false);
	public static final NamespacedIdentifier ENTITY_SPAWN_MOB_CHANNEL =
		ChannelRegistry.register(ChannelIdentifiers.from("retroapi", "entity_spawn_mob"), true, false);

	// Dimension id sync (server -> client): a table of modded dimension identifier -> serial id, sent at
	// join so the client aligns its serial ids with the server's (each side assigns from its own
	// id_map.dat). Needed so PlayerRespawnPacket byte dimension ids resolve to the right dimension.
	public static final NamespacedIdentifier DIM_SYNC_CHANNEL =
		ChannelRegistry.register(ChannelIdentifiers.from("retroapi", "dim_sync"), true, false);

	// Container GUI open (server -> client): handler id string + window sync id. The client looks up
	// the screen factory in RetroGuiRegistry and opens it; vanilla window-item packets do the rest.
	public static final NamespacedIdentifier OPEN_GUI_CHANNEL =
		ChannelRegistry.register(ChannelIdentifiers.from("retroapi", "open_gui"), true, false);

	// Single-block state sync (server -> client): x, y, z, flattened state index. Chunk-level
	// state data rides the chunk packet; this covers post-load single-block changes
	// (RetroStates.set). Stays enabled in StationAPI mode (no overlap with the disabled
	// network mixins, which only cover id/chunk packets).
	public static final NamespacedIdentifier STATE_SYNC_CHANNEL =
		ChannelRegistry.register(ChannelIdentifiers.from("retroapi", "state_sync"), true, false);

	// World sound bridge (server -> client): b1.7.3's protocol has NO sound packet - vanilla's
	// ServerWorldEventListener.playSound is an empty method, so every world.playSound on a dedicated
	// server is silently dropped (mob attack sounds, custom block sounds...). The bridge sends
	// sound id + position + volume/pitch to players in hearing range; the client plays it locally.
	public static final NamespacedIdentifier PLAY_SOUND_CHANNEL =
		ChannelRegistry.register(ChannelIdentifiers.from("retroapi", "play_sound"), true, false);
}
