package com.periut.retroapi.server;

import com.periut.retroapi.RetroAPI;
import com.periut.retroapi.network.RetroAPINetworking;
import com.periut.retroapi.register.blockentity.RetroMenu;
import com.periut.retroapi.mixin.network.ServerPlayerEntityAccessor;
import com.periut.retroapi.registry.BlockRegistration;
import com.periut.retroapi.registry.ItemRegistration;
import com.periut.retroapi.registry.RetroRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.inventory.Inventory;
import net.minecraft.network.packet.OpenInventoryMenuPacket;
import net.minecraft.server.entity.mob.player.ServerPlayerEntity;
import net.ornithemc.osl.entrypoints.api.server.ServerModInitializer;
import net.ornithemc.osl.networking.api.server.ServerConnectionEvents;
import net.ornithemc.osl.networking.api.server.ServerPlayNetworking;

public class RetroAPIServer implements ServerModInitializer {
	@Override
	public void initServer() {
		RetroMenu.setServerOpener((player, menu, menuType) -> {
			ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
			ServerPlayerEntityAccessor accessor = (ServerPlayerEntityAccessor) serverPlayer;
			accessor.invokeIncrementSyncId();
			int menuId = accessor.getMenuId();

			String name = "";
			int size = 0;
			if (menu.getBlockEntity() instanceof Inventory inv) {
				name = inv.getInventoryName();
				size = inv.getSize();
			}

			serverPlayer.networkHandler.sendPacket(
				new OpenInventoryMenuPacket(menuId, menuType, name, size)
			);

			serverPlayer.menu = menu;
			menu.networkId = menuId;
			menu.addListener(serverPlayer);
		});

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
