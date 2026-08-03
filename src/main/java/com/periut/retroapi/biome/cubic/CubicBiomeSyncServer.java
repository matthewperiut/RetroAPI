package com.periut.retroapi.biome.cubic;

import com.periut.retroapi.network.RetroAPINetworking;
import com.periut.retroapi.storage.ChunkExtendedBlocks;
import com.periut.retroapi.storage.ExtendedBlocksAccess;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.ornithemc.osl.networking.api.server.ServerPlayNetworking;

import java.util.Map;

/**
 * Server-only half of cubic biome sync. In its own class so client environments never load
 * {@code ServerPlayerEntity}/{@code ServerPlayNetworking} - the callers only reach in here when the
 * environment type is SERVER, mirroring {@code StateSyncServer} and {@code BlockDataSyncServer}.
 *
 * <p>The wire form is one message shape for both cases (a chunk's whole cell table on chunk send, a
 * single cell on change), because a chunk's table is a handful of entries at most: 8 cells for a
 * vanilla-height world, a couple of dozen for an extended one.
 *
 * <p>Biomes go over the wire <b>by id string</b>, not by runtime id. Client and server allocate runtime
 * ids independently from their own registration order, so a numeric id means different things on the two
 * ends the moment their mod sets differ even slightly.
 */
public final class CubicBiomeSyncServer {

	private CubicBiomeSyncServer() {}

	/** Sends one changed cell to every player who could be looking at that chunk. */
	static void sendCell(World world, int chunkX, int cellY, int chunkZ, RetroCubicBiome biome) {
		String id = biome == null ? "" : biome.getId().toString();
		for (Object candidate : world.players) {
			if (!(candidate instanceof ServerPlayerEntity player)) {
				continue;
			}
			if (!inRange(player, chunkX, chunkZ)) {
				continue;
			}
			ServerPlayNetworking.send(player, RetroAPINetworking.CUBIC_BIOME_CHANNEL, buf -> {
				buf.writeInt(chunkX);
				buf.writeInt(chunkZ);
				buf.writeShort(1);
				buf.writeInt(cellY);
				buf.writeString(id);
			});
		}
	}

	/**
	 * Sends a chunk's whole cell table to one player, as the chunk is sent. No-op for a chunk with no
	 * assigned cells, which is almost every chunk - an unassigned chunk must cost nothing on the wire
	 * just as it costs nothing on disk.
	 */
	public static void sendChunk(ServerPlayerEntity player, Chunk chunk) {
		if (chunk == null) {
			return;
		}
		ChunkExtendedBlocks extended = ((ExtendedBlocksAccess) chunk).retroapi$getExtendedBlocks();
		if (!extended.hasAnyCubicBiome()) {
			return;
		}
		// Snapshot: the packet may be written after this returns while generation keeps mutating the
		// live map.
		Map<Integer, Integer> cells = new java.util.LinkedHashMap<>(extended.getCubicBiomeMap());
		int chunkX = chunk.x;
		int chunkZ = chunk.z;
		ServerPlayNetworking.send(player, RetroAPINetworking.CUBIC_BIOME_CHANNEL, buf -> {
			buf.writeInt(chunkX);
			buf.writeInt(chunkZ);
			buf.writeShort(cells.size());
			for (Map.Entry<Integer, Integer> entry : cells.entrySet()) {
				buf.writeInt(entry.getKey());
				RetroCubicBiome biome = RetroCubicBiomes.byRuntimeId(entry.getValue());
				buf.writeString(biome == null ? "" : biome.getId().toString());
			}
		});
	}

	/** Chunk-radius the vanilla server tracks around a player, in chunks, with a chunk of slack. */
	private static boolean inRange(ServerPlayerEntity player, int chunkX, int chunkZ) {
		int px = ((int) player.x) >> 4;
		int pz = ((int) player.z) >> 4;
		return Math.abs(px - chunkX) <= 16 && Math.abs(pz - chunkZ) <= 16;
	}
}
