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

		if (!hasStationAPI) {
			ServerConnectionEvents.PLAY_READY.register((server, player) -> {
				RetroAPI.LOGGER.info("Sending ID sync packet to player");

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
	}
}
