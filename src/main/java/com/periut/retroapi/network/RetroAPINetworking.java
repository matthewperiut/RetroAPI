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

	// Auxiliary per-position data (server -> client): x, y, z, data-type key, value. Whole-chunk data
	// rides the chunk packet (WorldChunkPacketMixin); this covers post-load single-position changes
	// (RetroBlockData.set), and is sent only to players whose view can contain the position.
	public static final NamespacedIdentifier BLOCK_DATA_CHANNEL =
		ChannelRegistry.register(ChannelIdentifiers.from("retroapi", "block_data"), true, false);

	// Cubic (cave) biome cells (server -> client): chunkX, chunkZ, count, then per cell a cellY and the
	// biome's id STRING. Deliberately its own channel rather than a section on the chunk packet:
	// WorldChunkPacketMixin is disabled under StationAPI (StationAPI owns chunk packets there), so a
	// cbio section riding it would silently stop syncing in exactly the configuration RetroAPI has to
	// support. Ids are strings because each side allocates runtime ids from its own registration order.
	public static final NamespacedIdentifier CUBIC_BIOME_CHANNEL =
		ChannelRegistry.register(ChannelIdentifiers.from("retroapi", "cubic_biome"), true, false);

	// Block entity sync (server -> client): x, y, z, then the block entity's sync NBT. b1.7.3's protocol
	// has no generic block-entity packet at all (the only one that carries block-entity data is the sign
	// packet), so a modded block entity's server-side state could never reach the client except by
	// masquerading as a container. Sent on chunk send and on every setBlockDirty for block entities that
	// implement RetroSyncedBlockEntity. See RetroBlockEntities.sync.
	// Also client -> server, as a two-int chunk position: "I have this chunk now, what is in it?".
	// Volunteering the state as the chunk goes out cannot stand on its own - at join the client's
	// channels are not up yet and OSL discards the send, and later it can beat the chunk it describes -
	// so the client asks once the chunk is actually in its world.
	public static final NamespacedIdentifier BLOCK_ENTITY_SYNC_CHANNEL =
		ChannelRegistry.register(ChannelIdentifiers.from("retroapi", "block_entity_sync"), true, true);

	// Game rules (server -> client): a count, then that many key/value pairs. Sent in full on join and
	// as a single pair when one changes. A client needs its own copy because some rules are decided
	// client-side before the server ever sees the move (sprinting, swimming, the death screen).
	public static final NamespacedIdentifier GAMERULE_CHANNEL =
		ChannelRegistry.register(ChannelIdentifiers.from("retroapi", "gamerules"), true, false);

	// Mod configs, both ways (see com.periut.retroapi.config.ConfigSync). Server -> client: whether this
	// player may edit the server's settings, then every Scope.WORLD option as a key/value pair, sent on
	// join and again whenever an operator changes one. Client -> server: one key/value pair, an
	// operator's edit - which the server re-checks against its own op list before applying, because the
	// "may edit" flag it sent is for drawing the screen and never for deciding anything.
	public static final NamespacedIdentifier CONFIG_CHANNEL =
		ChannelRegistry.register(ChannelIdentifiers.from("retroapi", "config"), true, true);

	// Game modes (server -> client): a count, then that many player-name/mode-id pairs. Every client is
	// told about every player because rendering a spectator as invisible is a decision each client makes
	// about somebody else.
	public static final NamespacedIdentifier GAMEMODE_CHANNEL =
		ChannelRegistry.register(ChannelIdentifiers.from("retroapi", "gamemode"), true, false);

	// Creative flight, both ways. Client -> server: the double-tap of the jump key is only visible on
	// the client, so the client asks and the server decides - it checks the player's mode before
	// agreeing. Server -> client: a single boolean saying whether that player is now flying, sent back
	// after a toggle and again when they join, because a client's own physics is what actually holds it
	// in the air and it cannot guess a state the server has been keeping since their last session.
	public static final NamespacedIdentifier FLIGHT_CHANNEL =
		ChannelRegistry.register(ChannelIdentifiers.from("retroapi", "flight"), true, true);

	// Creative inventory pick (client -> server): item id, damage and count. The server checks the
	// player is actually in creative before handing anything over - a client may not simply assert it.
	public static final NamespacedIdentifier CREATIVE_GIVE_CHANNEL =
		ChannelRegistry.register(ChannelIdentifiers.from("retroapi", "creative_give"), false, true);

	// Command block edits (client -> server): position, command, mode, conditional, always-active and
	// track-output. Modern's ServerboundSetCommandBlockPacket, and re-checked the same way: the server
	// confirms the sender may use game-master blocks before believing any of it.
	public static final NamespacedIdentifier COMMAND_BLOCK_CHANNEL =
		ChannelRegistry.register(ChannelIdentifiers.from("retroapi", "command_block"), false, true);

	// World sound bridge (server -> client): b1.7.3's protocol has NO sound packet - vanilla's
	// ServerWorldEventListener.playSound is an empty method, so every world.playSound on a dedicated
	// server is silently dropped (mob attack sounds, custom block sounds...). The bridge sends
	// sound id + position + volume/pitch to players in hearing range; the client plays it locally.
	public static final NamespacedIdentifier PLAY_SOUND_CHANNEL =
		ChannelRegistry.register(ChannelIdentifiers.from("retroapi", "play_sound"), true, false);

	// World particle bridge (server -> client): the same story as sounds. b1.7.3 has no particle packet
	// and vanilla's ServerWorldEventListener.addParticle is empty, so a world.addParticle on a dedicated
	// server reaches nobody. The bridge sends name + position + velocity to players in range; the client
	// replays it through its own world, which routes registered names to RetroAPI particle factories.
	public static final NamespacedIdentifier PARTICLE_CHANNEL =
		ChannelRegistry.register(ChannelIdentifiers.from("retroapi", "particle"), true, false);
}
