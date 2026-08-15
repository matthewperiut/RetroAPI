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

	/** Answers a client's "what is in this chunk?" with every synced block entity inside it. */
	public static void sendChunk(ServerPlayerEntity player, int chunkX, int chunkZ) {
		if (player == null || !(player.world instanceof net.minecraft.world.ServerWorld world)) {
			return;
		}
		// Only a chunk the player could actually be watching, so the request cannot be used to read
		// block entities from across the world.
		if (Math.abs(chunkX - ((int) player.x >> 4)) > 16 || Math.abs(chunkZ - ((int) player.z >> 4)) > 16) {
			return;
		}

		final int x = chunkX << 4;
		final int z = chunkZ << 4;
		for (final Object candidate : world.getBlockEntities(x, 0, z, x + 15, 128, z + 15)) {
			if (candidate instanceof BlockEntity blockEntity && blockEntity instanceof RetroSyncedBlockEntity) {
				sendTo(player, blockEntity);
			}
		}
	}

	/** Beta's own view distance, in blocks: the furthest a joining player can already be watching. */
	private static final int VIEW_RADIUS = 10 * 16;

	/**
	 * Everything synced within sight of a player who has just become reachable.
	 *
	 * <p>Vanilla sends a chunk's block entities the moment it sends the chunk, and RetroAPI hangs its own
	 * sync off that - but at join time those chunks go out before the client has finished registering its
	 * channels, and OSL <em>drops</em> a send to a player whose channel is not ready yet. Not queues:
	 * drops. So the state that rode along with the first chunks was thrown away, and nothing sent it
	 * again - which is why a spawner set to creepers came back as a pig every time you rejoined, then
	 * corrected itself the moment anything touched it.
	 *
	 * <p>Called from {@code ServerConnectionEvents.PLAY_READY}, which is the first moment a send to this
	 * player actually goes anywhere.
	 */
	public static void sendNearby(ServerPlayerEntity player) {
		if (player == null || !(player.world instanceof net.minecraft.world.ServerWorld world)) {
			return;
		}

		final int x = (int) player.x;
		final int z = (int) player.z;
		final List<?> found = world.getBlockEntities(
			x - VIEW_RADIUS, 0, z - VIEW_RADIUS, x + VIEW_RADIUS, 128, z + VIEW_RADIUS);

		for (final Object candidate : found) {
			if (candidate instanceof BlockEntity blockEntity && blockEntity instanceof RetroSyncedBlockEntity) {
				sendTo(player, blockEntity);
			}
		}
	}

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
