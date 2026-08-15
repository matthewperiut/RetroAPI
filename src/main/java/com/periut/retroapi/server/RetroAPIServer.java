package com.periut.retroapi.server;

import com.periut.retroapi.RetroAPI;
import com.periut.retroapi.dimension.DimensionHelper;
import com.periut.retroapi.dimension.DimensionRegistration;
import com.periut.retroapi.dimension.RetroDimensionRegistry;
import com.periut.retroapi.dimension.server.ServerTeleporter;
import com.periut.retroapi.entity.EntitySpawnCodec;
import com.periut.retroapi.network.RetroAPINetworking;
import com.periut.retroapi.registry.BlockRegistration;
import com.periut.retroapi.registry.ItemRegistration;
import com.periut.retroapi.registry.RetroRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.ornithemc.osl.entrypoints.api.server.ServerModInitializer;
import net.ornithemc.osl.networking.api.server.ServerConnectionEvents;
import net.ornithemc.osl.networking.api.server.ServerPlayNetworking;

public class RetroAPIServer implements ServerModInitializer {
	@Override
	public void initServer() {
		boolean hasStationAPI = FabricLoader.getInstance().isModLoaded("stationapi");

		// The server's dimension teleport back end. Referenced only here (server entrypoint), so the
		// client never loads ServerTeleporter or its server-only class refs. Registered into the SERVER
		// slot so that under the integrated server (both entrypoints in one JVM) it does not clobber the
		// client back end.
		DimensionHelper.setServerTeleporter(new ServerTeleporter());

		// The server's container-GUI opener (window sync id + open_gui packet). Referenced only here.
		com.periut.retroapi.gui.RetroGuis.setServerOpener(new com.periut.retroapi.gui.server.ServerGuiOpener());

		// Game rules are the server's to decide; a client needs the values because some of them
		// (sprinting, swimming, the death screen) are acted on before the server ever sees the move.
		ServerConnectionEvents.PLAY_READY.register((server, player) ->
			com.periut.retroapi.gamerule.GameRuleSync.sendAll(player));

		// Game modes: every client is told about every player, because whether to draw somebody is a
		// decision each client makes about somebody else.
		ServerConnectionEvents.PLAY_READY.register((server, player) ->
			com.periut.retroapi.gamemode.GameModeSync.sendAll(player));

		// Synced block entities within sight, and the command tree, resent HERE rather than trusted to
		// the join itself. Both are first sent while the player is still logging in, and OSL drops -
		// not queues - a send to a player whose channel handshake has not finished, so both were being
		// thrown away: a spawner came back as a pig on every rejoin, and the client parsed commands
		// against its own guess of a tree instead of the server's.
		ServerConnectionEvents.PLAY_READY.register((server, player) -> {
			com.periut.retroapi.register.blockentity.BlockEntitySyncServer.sendNearby(player);
			com.periut.retroapi.commands.network.ServerCommandNetworking.sendTree(player);
			com.periut.retroapi.commands.util.ServerUtil.informPlayerOpStatus(player.name);
		});

		// "I have this chunk now, what is in it?" - the client asks as each chunk lands, which is the one
		// moment it is certainly ready to receive and certainly holds the chunk being described.
		ServerPlayNetworking.registerListener(RetroAPINetworking.BLOCK_ENTITY_SYNC_CHANNEL, (context, buffer) -> {
			int chunkX = buffer.readInt();
			int chunkZ = buffer.readInt();
			context.ensureOnMainThread();
			com.periut.retroapi.register.blockentity.BlockEntitySyncServer.sendChunk(context.player(), chunkX, chunkZ);
		});

		// The two things a client may ask for. Both are re-checked here against the player's actual
		// mode - a client that says it is in creative is not evidence that it is.
		ServerPlayNetworking.registerListener(RetroAPINetworking.FLIGHT_CHANNEL, (context, buffer) -> {
			buffer.readBoolean();
			context.ensureOnMainThread();
			com.periut.retroapi.gamemode.RetroGameModes.toggleFlying(context.player().name);
			// Answer with what the server decided: the client asked, and it is the client's own
			// movement code that has to act on it.
			com.periut.retroapi.gamemode.GameModeSync.sendFlight(context.player());
		});

		ServerPlayNetworking.registerListener(RetroAPINetworking.COMMAND_BLOCK_CHANNEL, (context, buffer) -> {
			int x = buffer.readInt();
			int y = buffer.readInt();
			int z = buffer.readInt();
			String command = buffer.readString();
			int mode = buffer.readVarInt();
			boolean conditional = buffer.readBoolean();
			boolean automatic = buffer.readBoolean();
			boolean trackOutput = buffer.readBoolean();
			context.ensureOnMainThread();
			com.periut.retroapi.commandblock.CommandBlockNetworking.apply(context.player().world, context.player(),
				x, y, z, command,
				com.periut.retroapi.commandblock.CommandBlockMode.values()[
					Math.max(0, Math.min(com.periut.retroapi.commandblock.CommandBlockMode.values().length - 1, mode))],
				conditional, automatic, trackOutput);
		});

		ServerPlayNetworking.registerListener(RetroAPINetworking.CREATIVE_GIVE_CHANNEL, (context, buffer) -> {
			int kind = buffer.readVarInt();
			if (kind == com.periut.retroapi.gamemode.GameModeNetworking.SET_SLOT) {
				int slot = buffer.readVarInt();
				int itemId = buffer.readVarInt();
				int damage = buffer.readVarInt();
				int count = buffer.readVarInt();
				context.ensureOnMainThread();
				com.periut.retroapi.gamemode.CreativeGive.setSlot(context.player(), slot,
					count <= 0 || itemId <= 0 ? null : new net.minecraft.item.ItemStack(itemId, count, damage));
				return;
			}

			int itemId = buffer.readVarInt();
			int damage = buffer.readVarInt();
			int count = buffer.readVarInt();
			context.ensureOnMainThread();
			com.periut.retroapi.gamemode.CreativeGive.give(context.player(),
				new net.minecraft.item.ItemStack(itemId, count, damage));
		});

		if (!hasStationAPI) {
			ServerConnectionEvents.PLAY_READY.register((server, player) -> {
				RetroAPI.LOGGER.debug("Sending ID sync packet to player");

				ServerPlayNetworking.send(player, RetroAPINetworking.ID_SYNC_CHANNEL, buffer -> {
					buffer.writeVarInt(RetroRegistry.getBlocks().size());
					for (BlockRegistration reg : RetroRegistry.getBlocks()) {
						buffer.writeString(reg.getId().toString());
						buffer.writeVarInt(reg.getBlock().id);
					}

					buffer.writeVarInt(RetroRegistry.getItems().size());
					for (ItemRegistration reg : RetroRegistry.getItems()) {
						buffer.writeString(reg.getId().toString());
						buffer.writeVarInt(reg.getItem().id);
					}
				});

				ServerPlayNetworking.send(player, RetroAPINetworking.DIM_SYNC_CHANNEL, buffer -> {
					buffer.writeVarInt(RetroDimensionRegistry.getAll().size());
					for (DimensionRegistration reg : RetroDimensionRegistry.getAll()) {
						buffer.writeString(reg.getId().toString());
						buffer.writeVarInt(reg.getSerialId());
					}
				});
			});
		}

		// Flush entity spawn packets the tracker tried to send before this player's OSL channel
		// handshake completed. OSL's ServerPlayNetworking.send SILENTLY DROPS sends to not-yet-ready
		// players, and the tracker fires on the join tick (vanilla onEntityAdded pushes every existing
		// tracked entity through updateListener immediately, then marks the player a listener and never
		// re-sends) - without this re-send, every pre-existing modded entity is permanently invisible
		// to a joining client. Registered AFTER the id-sync listener so the client applies ID mappings
		// before any spawn payloads, and unconditionally: the RetroAPI entity spawn path stays active
		// under StationAPI too (orthogonal coexistence).
		ServerConnectionEvents.PLAY_READY.register((server, player) ->
			EntitySpawnCodec.flushPending(player));

		// The `retroapi-server` entrypoint: mods' dedicated-server-only setup, after the server
		// platform above is in place. Never runs in singleplayer (b1.7.3 has no integrated server).
		com.periut.retroapi.entrypoint.RetroEntrypoints.invokeServer();
	}
}
