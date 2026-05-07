package com.periut.retroapi.server;

import com.periut.retroapi.RetroAPI;
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
			});
		}
	}
}
