package com.periut.retroapi.gui;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.ScreenHandler;
import net.ornithemc.osl.core.api.util.NamespacedIdentifier;

/**
 * Side-independent container-GUI opener, mirroring StationAPI's {@code GuiHelper}.
 * Call from a block's {@code onUse} without side checks:
 *
 * <pre>{@code
 * RetroGuis.openGUI(player, id("freezer"), freezerBlockEntity, new ContainerFreezer(player.inventory, freezerBlockEntity));
 * }</pre>
 *
 * <p>On a dedicated server this assigns a window sync id, sends the
 * {@code retroapi:open_gui} channel message, and installs the container on the player.
 * In singleplayer it opens the registered screen directly. The client screen factory is
 * registered via {@link com.periut.retroapi.gui.client.RetroGuiRegistry}.</p>
 */
public final class RetroGuis {
	private static GuiOpener serverOpener;
	private static GuiOpener clientOpener;

	private RetroGuis() {}

	/** Internal: set by RetroAPIServer at init. */
	public static void setServerOpener(GuiOpener opener) {
		serverOpener = opener;
	}

	/** Internal: set by RetroAPIClient at init. */
	public static void setClientOpener(GuiOpener opener) {
		clientOpener = opener;
	}

	public static void openGUI(PlayerEntity player, NamespacedIdentifier id, Inventory inventory, ScreenHandler container) {
		// A remote client never opens server-driven GUIs itself; the open_gui packet does.
		if (player.world.isRemote) {
			return;
		}
		// Env-neutral side dispatch: avoid loading ServerPlayerEntity in a client-only environment.
		boolean dedicated = player.getClass().getName().endsWith("ServerPlayerEntity");
		GuiOpener opener = dedicated ? serverOpener : clientOpener;
		if (opener != null) {
			opener.open(player, id, inventory, container);
		}
	}
}
