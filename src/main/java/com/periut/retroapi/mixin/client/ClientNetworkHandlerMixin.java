package com.periut.retroapi.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.periut.retroapi.component.ComponentWire;
import com.periut.retroapi.component.SpawnComponentCarrier;
import com.periut.retroapi.network.BlocksUpdatePacketAccess;
import com.periut.retroapi.storage.ChunkExtendedBlocks;
import com.periut.retroapi.storage.ExtendedBlocksAccess;
import net.minecraft.client.network.ClientNetworkHandler;
import net.minecraft.entity.ItemEntity;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ItemEntitySpawnS2CPacket;
import net.minecraft.world.ClientWorld;
import net.minecraft.world.chunk.Chunk;
import com.periut.retroapi.network.WorldChunkPacketAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientNetworkHandler.class)
public class ClientNetworkHandlerMixin {

	@Shadow private ClientWorld world;

	/**
	 * Apply the dropped item's component blob once the client has rebuilt its stack. The
	 * spawn packet carried the blob (see ItemEntitySpawnComponentMixin); here the freshly
	 * constructed ItemEntity is the local the handler just made, and its stack gets the
	 * components so things like the Mood Gem's texture render correctly on the ground.
	 */
	@Inject(method = "onItemEntitySpawn", at = @At("TAIL"))
	private void retroapi$applyDroppedItemComponents(ItemEntitySpawnS2CPacket packet, CallbackInfo ci,
			@Local ItemEntity entity) {
		ComponentWire.applyBlob(entity.stack, ((SpawnComponentCarrier) packet).retroapi$spawnComponents());
	}

	@Inject(method = "onChunkDeltaUpdate", at = @At("HEAD"), cancellable = true)
	private void retroapi$handleBlocksUpdate(ChunkDeltaUpdateS2CPacket packet, CallbackInfo ci) {
		short[] fullBlockIds = ((BlocksUpdatePacketAccess) packet).retroapi$getFullBlockIds();
		if (fullBlockIds == null) return; // fallback to vanilla if no extended data

		Chunk chunk = world.getChunk(packet.x, packet.z);
		int baseX = packet.x * 16;
		int baseZ = packet.z * 16;

		for (int i = 0; i < packet.size; i++) {
			short pos = packet.positions[i];
			int localX = pos >> 12 & 15;
			int localZ = pos >> 8 & 15;
			int y = pos & 255;
			int blockId = fullBlockIds[i] & 0xFFFF;
			int meta = packet.blockMetadata[i];

			chunk.setBlock(localX, y, localZ, blockId, meta);
			world.clearBlockResets(
				localX + baseX, y, localZ + baseZ,
				localX + baseX, y, localZ + baseZ
			);
			world.setBlocksDirty(
				localX + baseX, y, localZ + baseZ,
				localX + baseX, y, localZ + baseZ
			);
		}

		ci.cancel();
	}

	@Inject(method = "handleChunkData", at = @At("RETURN"))
	private void retroapi$afterHandleWorldChunk(ChunkDataS2CPacket packet, CallbackInfo ci) {
		WorldChunkPacketAccess access = (WorldChunkPacketAccess) packet;
		int[] xPositions = access.retroapi$getXmetaPositions();
		if (access.retroapi$getExtCount() == 0 && xPositions == null) return;

		int chunkX = packet.x >> 4;
		int chunkZ = packet.z >> 4;
		Chunk chunk = world.getChunk(chunkX, chunkZ);
		if (chunk == null) return;

		ChunkExtendedBlocks extended = ((ExtendedBlocksAccess) chunk).retroapi$getExtendedBlocks();
		int[] indices = access.retroapi$getExtIndices();
		int[] blockIds = access.retroapi$getExtBlockIds();
		int[] meta = access.retroapi$getExtMeta();

		for (int i = 0; i < access.retroapi$getExtCount(); i++) {
			extended.set(indices[i], blockIds[i], meta[i]);
		}

		// Secondary meta (state index bits 4-11) rides the same packet.
		if (xPositions != null) {
			int[] xValues = access.retroapi$getXmetaValues();
			for (int i = 0; i < xPositions.length; i++) {
				extended.setXmeta(xPositions[i], xValues[i]);
			}
		}

		// Fix the heightmap now that extended blocks are loaded - but ONLY the heightmap.
		// populateHeightMap (full variant) rewrote the sky-light columns from the air-masked raw
		// arrays, clobbering the authoritative light the server sent in the vanilla chunk packet -
		// blocks flashed bright in dark spaces until client-side updates converged them back.
		chunk.populateHeightMapOnly();

		// Re-render the chunk
		int baseX = chunkX * 16;
		int baseZ = chunkZ * 16;
		world.setBlocksDirty(baseX, 0, baseZ, baseX + 15, 127, baseZ + 15);
	}
}
