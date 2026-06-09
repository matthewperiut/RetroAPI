package com.periut.retroapi.gui.client;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;

/**
 * Client-side GUI handler: the screen factory plus the inventory factory used on remote
 * clients (which have no real block entity instance). Mirrors StationAPI's {@code GuiHandler}.
 */
public record RetroGuiHandler(ScreenFactory screenFactory, InventoryFactory inventoryFactory) {

	@FunctionalInterface
	public interface ScreenFactory {
		Screen create(PlayerEntity player, Inventory inventory);
	}

	@FunctionalInterface
	public interface InventoryFactory {
		Inventory create();
	}
}
