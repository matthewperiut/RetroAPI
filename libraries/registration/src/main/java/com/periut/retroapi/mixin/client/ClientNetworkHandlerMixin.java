package com.periut.retroapi.mixin.client;

import com.periut.retroapi.network.BlocksUpdatePacketAccess;
import com.periut.retroapi.storage.ChunkExtendedBlocks;
import com.periut.retroapi.storage.ExtendedBlocksAccess;
import com.periut.retroapi.network.WorldChunkPacketAccess;
import net.minecraft.client.network.handler.ClientNetworkHandler;
import net.minecraft.client.world.MultiplayerWorld;
import net.minecraft.network.packet.BlocksUpdatePacket;
import net.minecraft.network.packet.WorldChunkPacket;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientNetworkHandler.class)
public class ClientNetworkHandlerMixin {

	@Shadow private MultiplayerWorld world;

	@Inject(method = "handleBlocksUpdate", at = @At("HEAD"), cancellable = true)
	private void retroapi$handleBlocksUpdate(BlocksUpdatePacket packet, CallbackInfo ci) {
		short[] fullBlockIds = ((BlocksUpdatePacketAccess) packet).retroapi$getFullBlockIds();
		if (fullBlockIds == null) return; // fallback to vanilla if no extended data

		WorldChunk chunk = world.getChunkAt(packet.chunkX, packet.chunkZ);
		int baseX = packet.chunkX * 16;
		int baseZ = packet.chunkZ * 16;

		for (int i = 0; i < packet.blockChangeCount; i++) {
			short pos = packet.positions[i];
			int localX = pos >> 12 & 15;
			int localZ = pos >> 8 & 15;
			int y = pos & 255;
			int blockId = fullBlockIds[i] & 0xFFFF;
			int meta = packet.blockMetadata[i];

			chunk.setBlockWithMetadataAt(localX, y, localZ, blockId, meta);
			world.clearBlockResets(
				localX + baseX, y, localZ + baseZ,
				localX + baseX, y, localZ + baseZ
			);
			world.notifyRegionChanged(
				localX + baseX, y, localZ + baseZ,
				localX + baseX, y, localZ + baseZ
			);
		}

		ci.cancel();
	}

	@Inject(method = "handleWorldChunk", at = @At("RETURN"))
	private void retroapi$afterHandleWorldChunk(WorldChunkPacket packet, CallbackInfo ci) {
		WorldChunkPacketAccess access = (WorldChunkPacketAccess) packet;
		if (access.retroapi$getExtCount() == 0) return;

		int chunkX = packet.x >> 4;
		int chunkZ = packet.z >> 4;
		WorldChunk chunk = world.getChunkAt(chunkX, chunkZ);
		if (chunk == null) return;

		ChunkExtendedBlocks extended = ((ExtendedBlocksAccess) chunk).retroapi$getExtendedBlocks();
		int[] indices = access.retroapi$getExtIndices();
		int[] blockIds = access.retroapi$getExtBlockIds();
		int[] meta = access.retroapi$getExtMeta();

		for (int i = 0; i < access.retroapi$getExtCount(); i++) {
			extended.set(indices[i], blockIds[i], meta[i]);
		}

		// Trigger heightmap and sky light recalculation now that extended blocks are loaded
		chunk.populateHeightMap();

		// Re-render the chunk
		int baseX = chunkX * 16;
		int baseZ = chunkZ * 16;
		world.notifyRegionChanged(baseX, 0, baseZ, baseX + 15, 127, baseZ + 15);
	}
}
