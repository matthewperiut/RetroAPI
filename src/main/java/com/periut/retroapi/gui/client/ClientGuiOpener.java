package com.periut.retroapi.gui.client;

import com.periut.retroapi.gui.GuiOpener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.ScreenHandler;
import net.ornithemc.osl.core.api.util.NamespacedIdentifier;

/**
 * Singleplayer GUI opener: opens the registered screen directly against the real inventory
 * (block entity). Mirrors StationAPI's {@code GuiHelperClientImpl} behavior.
 */
public class ClientGuiOpener implements GuiOpener {

	@Override
	public void open(PlayerEntity player, NamespacedIdentifier id, Inventory inventory, ScreenHandler container) {
		RetroGuiHandler handler = RetroGuiRegistry.get(id.toString());
		if (handler != null) {
			Minecraft minecraft = (Minecraft) FabricLoader.getInstance().getGameInstance();
			minecraft.setScreen(handler.screenFactory().create(player, inventory));
		}
	}
}
