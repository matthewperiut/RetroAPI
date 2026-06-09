package com.periut.retroapi.state;

import com.periut.retroapi.network.RetroAPINetworking;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.World;
import net.ornithemc.osl.networking.api.server.ServerPlayNetworking;

/**
 * Server-only half of single-block state sync: broadcasts a full flattened index to every
 * player in the world over {@code retroapi:state_sync}. Kept in its own class so client
 * environments never load ServerPlayerEntity/ServerPlayNetworking (RetroStates only calls
 * in here when the environment type is SERVER).
 */
final class StateSyncServer {

	private StateSyncServer() {}

	static void send(World world, int x, int y, int z, int index) {
		for (Object player : world.players) {
			if (player instanceof ServerPlayerEntity) {
				ServerPlayNetworking.send((ServerPlayerEntity) player, RetroAPINetworking.STATE_SYNC_CHANNEL, buf -> {
					buf.writeInt(x);
					buf.writeInt(y);
					buf.writeInt(z);
					buf.writeVarInt(index);
				});
			}
		}
	}
}
