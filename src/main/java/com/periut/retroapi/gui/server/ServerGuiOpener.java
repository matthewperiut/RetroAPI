package com.periut.retroapi.gui.server;

import com.periut.retroapi.gui.GuiOpener;
import com.periut.retroapi.mixin.gui.ServerPlayerEntitySyncIdAccessor;
import com.periut.retroapi.network.RetroAPINetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerListener;
import net.ornithemc.osl.core.api.util.NamespacedIdentifier;
import net.ornithemc.osl.networking.api.server.ServerPlayNetworking;

/**
 * Dedicated-server GUI opener: assigns a window sync id, tells the client which registered
 * screen to open via the {@code retroapi:open_gui} channel, and installs the container on the
 * player so vanilla window-item sync packets flow. Mirrors StationAPI's {@code GuiHelperServerImpl}.
 */
public class ServerGuiOpener implements GuiOpener {

	@Override
	public void open(PlayerEntity player, NamespacedIdentifier id, Inventory inventory, ScreenHandler container) {
		ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
		ServerPlayerEntitySyncIdAccessor accessor = (ServerPlayerEntitySyncIdAccessor) serverPlayer;
		accessor.retroapi$incrementScreenHandlerSyncId();
		int syncId = accessor.retroapi$getScreenHandlerSyncId();

		ServerPlayNetworking.send(serverPlayer, RetroAPINetworking.OPEN_GUI_CHANNEL, buffer -> {
			buffer.writeString(id.toString());
			buffer.writeVarInt(syncId);
		});

		player.currentScreenHandler = container;
		container.syncId = syncId;
		container.addListener((ScreenHandlerListener) serverPlayer);
	}
}
