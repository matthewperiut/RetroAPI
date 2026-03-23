package com.periut.retroapi.client;

import com.periut.retroapi.RetroAPI;
import com.periut.retroapi.network.RetroAPINetworking;
import com.periut.retroapi.lang.LangLoader;
import com.periut.retroapi.registry.IdAssigner;
import net.fabricmc.loader.api.FabricLoader;
import net.ornithemc.osl.entrypoints.api.client.ClientModInitializer;
#if MC_VER != 131
import net.ornithemc.osl.lifecycle.api.client.MinecraftClientEvents;
#endif
import net.ornithemc.osl.networking.api.client.ClientPlayNetworking;

public class RetroAPIClient implements ClientModInitializer {
	@Override
	public void initClient() {
		boolean hasStationAPI = FabricLoader.getInstance().isModLoaded("stationapi");

#if MC_VER != 131
		MinecraftClientEvents.READY.register(minecraft -> {
			LangLoader.loadTranslations();
		});
#else
		LangLoader.loadTranslations();
#endif

		if (!hasStationAPI) {
			ClientPlayNetworking.registerListener(RetroAPINetworking.ID_SYNC_CHANNEL, (ctx, buffer) -> {
				// Do NOT use ensureOnMainThread() here — the ID remap must complete
				// synchronously on the network thread before any subsequent packets
				// (inventory, chunk data) are deserialized, otherwise Item.BY_ID / Block.BY_ID
				// lookups will NPE for server-assigned IDs that haven't been remapped yet.
				RetroAPI.LOGGER.info("Received ID sync packet from server");
				IdAssigner.applyFromNetwork(buffer);
			});
		}
	}
}
