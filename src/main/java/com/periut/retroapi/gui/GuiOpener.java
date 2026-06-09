package com.periut.retroapi.gui;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.ScreenHandler;
import net.ornithemc.osl.core.api.util.NamespacedIdentifier;

/**
 * Side-specific GUI opening strategy. The server and client each register their own
 * implementation from their entrypoints (environment-isolation: this common interface
 * references only env-neutral classes).
 */
public interface GuiOpener {
	void open(PlayerEntity player, NamespacedIdentifier id, Inventory inventory, ScreenHandler container);
}
