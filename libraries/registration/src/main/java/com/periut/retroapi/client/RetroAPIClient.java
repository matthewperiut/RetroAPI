package com.periut.retroapi.client;

import com.periut.retroapi.RetroAPI;
import com.periut.retroapi.network.RetroAPINetworking;
import com.periut.retroapi.register.blockentity.RetroMenu;
import com.periut.retroapi.lang.LangLoader;
import com.periut.retroapi.registry.IdAssigner;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.inventory.Inventory;
import net.ornithemc.osl.entrypoints.api.client.ClientModInitializer;
#if MC_VER != 131
import net.ornithemc.osl.lifecycle.api.client.MinecraftClientEvents;
#endif
import net.ornithemc.osl.networking.api.client.ClientPlayNetworking;

public class RetroAPIClient implements ClientModInitializer {
	@Override
	public void initClient() {
		RetroMenu.setClientOpener((player, menu, menuType) -> {
			Object be = menu.getBlockEntity();
			switch (menuType) {
				case RetroMenu.MENU_CHEST -> {
					if (be instanceof Inventory inv) {
						player.openChestMenu(inv);
					}
				}
				case RetroMenu.MENU_FURNACE -> {
					if (be instanceof Inventory inv) {
						FurnaceBlockEntityProxy proxy = new FurnaceBlockEntityProxy(inv, menu);
						player.openFurnaceMenu(proxy);
					}
				}
				case RetroMenu.MENU_DISPENSER -> {
					if (be instanceof Inventory inv) {
						player.openChestMenu(inv);
					}
				}
			}
		});

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
				ctx.ensureOnMainThread();
				RetroAPI.LOGGER.info("Received ID sync packet from server");
				IdAssigner.applyFromNetwork(buffer);
			});
		}
	}
}
