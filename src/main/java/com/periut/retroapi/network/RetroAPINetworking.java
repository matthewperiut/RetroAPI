package com.periut.retroapi.network;

import net.ornithemc.osl.core.api.util.NamespacedIdentifier;
import net.ornithemc.osl.networking.api.ChannelIdentifiers;
import net.ornithemc.osl.networking.api.ChannelRegistry;

public class RetroAPINetworking {
	public static final NamespacedIdentifier ID_SYNC_CHANNEL =
		ChannelRegistry.register(ChannelIdentifiers.from("retroapi", "id_sync"), true, false);
	public static final NamespacedIdentifier CHUNK_EXTENDED_CHANNEL =
		ChannelRegistry.register(ChannelIdentifiers.from("retroapi", "chunk_ext"), true, false);
}
