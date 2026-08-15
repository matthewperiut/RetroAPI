package com.periut.retroapi.register.blockentity;

import com.periut.retroapi.network.RetroAPINetworking;
import net.ornithemc.osl.networking.api.client.ClientPlayNetworking;

/**
 * The client half of "what is in this chunk?".
 *
 * <p>Sent once per chunk as it arrives; the server answers with one sync packet per synced block
 * entity inside it, or with nothing at all, which is the common case. Client-only, and only reached
 * from a client mixin, so a dedicated server never loads it.
 */
public final class BlockEntitySyncRequest {
	private BlockEntitySyncRequest() {}

	public static void request(int chunkX, int chunkZ) {
		if (!ClientPlayNetworking.isPlayReady(RetroAPINetworking.BLOCK_ENTITY_SYNC_CHANNEL)) {
			return;   // singleplayer, a vanilla server, or a handshake still in progress
		}
		ClientPlayNetworking.send(RetroAPINetworking.BLOCK_ENTITY_SYNC_CHANNEL, buffer -> {
			buffer.writeInt(chunkX);
			buffer.writeInt(chunkZ);
		});
	}
}
