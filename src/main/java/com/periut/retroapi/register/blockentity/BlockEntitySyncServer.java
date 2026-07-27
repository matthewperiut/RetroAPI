package com.periut.retroapi.register.blockentity;

import com.periut.retroapi.network.RetroAPINetworking;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.ornithemc.osl.networking.api.server.ServerPlayNetworking;

import java.util.List;

/**
 * Server-only half of block-entity sync. Kept in its own class - the same shape as
 * {@code StateSyncServer} - so a client environment never loads ServerPlayerEntity or
 * ServerPlayNetworking; callers only reach it from SERVER-side mixins.
 */
public final class BlockEntitySyncServer {

	private BlockEntitySyncServer() {}

	/** Sends one block entity's state to a single player. */
	public static void sendTo(ServerPlayerEntity player, BlockEntity blockEntity) {
		byte[] data = BlockEntitySyncCodec.encode(blockEntity);
		if (data == null) {
			return;
		}
		send(player, blockEntity, data);
	}

	/** Sends one block entity's state to every player in a list (the chunk's trackers). */
	public static void sendToAll(List<?> players, BlockEntity blockEntity) {
		if (players == null || players.isEmpty()) {
			return;
		}
		byte[] data = BlockEntitySyncCodec.encode(blockEntity);
		if (data == null) {
			return;
		}
		for (Object player : players) {
			if (player instanceof ServerPlayerEntity) {
				send((ServerPlayerEntity) player, blockEntity, data);
			}
		}
	}

	private static void send(ServerPlayerEntity player, BlockEntity blockEntity, byte[] data) {
		ServerPlayNetworking.send(player, RetroAPINetworking.BLOCK_ENTITY_SYNC_CHANNEL, buf -> {
			buf.writeInt(blockEntity.x);
			buf.writeInt(blockEntity.y);
			buf.writeInt(blockEntity.z);
			buf.writeByteArray(data);
		});
	}
}
