package com.periut.retroapi.mixin.network;

import com.periut.retroapi.network.BlocksUpdatePacketAccess;
import com.periut.retroapi.storage.ChunkExtendedBlocks;
import com.periut.retroapi.storage.ExtendedBlocksAccess;
import com.periut.retroapi.network.WorldChunkPacketAccess;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class ChunkSendMixin {

	@Shadow public ServerPlayerEntity player;

	@Inject(method = "sendPacket", at = @At("HEAD"))
	private void retroapi$populateExtendedData(Packet packet, CallbackInfo ci) {
		if (packet instanceof ChunkDataS2CPacket wcp) {
			WorldChunkPacketAccess access = (WorldChunkPacketAccess) wcp;
			if (access.retroapi$getExtCount() > 0) return;

			int chunkX = wcp.x >> 4;
			int chunkZ = wcp.z >> 4;
			Chunk chunk = player.world.getChunk(chunkX, chunkZ);
			if (chunk == null) return;

			ChunkExtendedBlocks extended = ((ExtendedBlocksAccess) chunk).retroapi$getExtendedBlocks();
			access.retroapi$populateExtended(extended);
		} else if (packet instanceof ChunkDeltaUpdateS2CPacket bsp) {
			BlocksUpdatePacketAccess access = (BlocksUpdatePacketAccess) bsp;
			if (access.retroapi$getFullBlockIds() != null) return;

			Chunk chunk = player.world.getChunk(bsp.x, bsp.z);
			access.retroapi$populateFullIds(chunk);
		}
	}
}
