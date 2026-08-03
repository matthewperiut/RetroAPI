package com.periut.retroapi.storage;

import com.periut.retroapi.network.RetroAPINetworking;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.World;
import net.ornithemc.osl.networking.api.server.ServerPlayNetworking;

/**
 * Server-only half of single-position {@link RetroBlockData} sync. Kept in its own class so client
 * environments never load ServerPlayerEntity/ServerPlayNetworking (RetroBlockData only calls in here
 * when the environment type is SERVER), mirroring {@code StateSyncServer}.
 *
 * <p>Only players whose view could contain the position are sent the change - a data write is a
 * per-position event, and a world can hold a great many of them.
 */
final class BlockDataSyncServer {

	/** Chunk-radius the vanilla server tracks around a player, in blocks, with a chunk of slack. */
	private static final double RANGE = 16 * 16;

	private BlockDataSyncServer() {}

	static void send(World world, int x, int y, int z, RetroBlockDataType type, int value) {
		String key = type.getKey();
		for (Object candidate : world.players) {
			if (!(candidate instanceof ServerPlayerEntity player)) {
				continue;
			}
			double dx = player.x - (x + 0.5);
			double dz = player.z - (z + 0.5);
			if (dx * dx + dz * dz > RANGE * RANGE) {
				continue;
			}
			ServerPlayNetworking.send(player, RetroAPINetworking.BLOCK_DATA_CHANNEL, buf -> {
				buf.writeInt(x);
				buf.writeInt(y);
				buf.writeInt(z);
				buf.writeString(key);
				buf.writeInt(value);
			});
		}
	}
}
