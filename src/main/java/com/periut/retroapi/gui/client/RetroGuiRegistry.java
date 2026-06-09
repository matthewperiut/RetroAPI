package com.periut.retroapi.gui.client;

import net.ornithemc.osl.core.api.util.NamespacedIdentifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Client registry of container-GUI handlers, keyed by the same identifier passed to
 * {@link com.periut.retroapi.gui.RetroGuis#openGUI}. Register from {@code client-init}:
 *
 * <pre>{@code
 * RetroGuiRegistry.register(id("freezer"), new RetroGuiHandler(
 *     (player, inv) -> new GuiFreezer(player.inventory, (BlockEntityFreezer) inv),
 *     BlockEntityFreezer::new));
 * }</pre>
 */
public final class RetroGuiRegistry {
	private static final Map<String, RetroGuiHandler> HANDLERS = new HashMap<>();

	private RetroGuiRegistry() {}

	public static void register(NamespacedIdentifier id, RetroGuiHandler handler) {
		HANDLERS.put(id.toString(), handler);
	}

	public static RetroGuiHandler get(String id) {
		return HANDLERS.get(id);
	}
}
