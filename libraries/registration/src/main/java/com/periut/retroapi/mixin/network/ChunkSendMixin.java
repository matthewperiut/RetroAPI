package com.periut.retroapi.mixin.network;

import com.periut.retroapi.network.BlocksUpdatePacketAccess;
import com.periut.retroapi.storage.ChunkExtendedBlocks;
import com.periut.retroapi.storage.ExtendedBlocksAccess;
import com.periut.retroapi.network.WorldChunkPacketAccess;
import net.minecraft.network.packet.BlocksUpdatePacket;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.WorldChunkPacket;
import net.minecraft.server.entity.mob.player.ServerPlayerEntity;
import net.minecraft.server.network.handler.ServerPlayNetworkHandler;
import net.minecraft.world.chunk.WorldChunk;
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
		if (packet instanceof WorldChunkPacket wcp) {
			WorldChunkPacketAccess access = (WorldChunkPacketAccess) wcp;
			if (access.retroapi$getExtCount() > 0) return;

			int chunkX = wcp.x >> 4;
			int chunkZ = wcp.z >> 4;
			WorldChunk chunk = player.world.getChunkAt(chunkX, chunkZ);
			if (chunk == null) return;

			ChunkExtendedBlocks extended = ((ExtendedBlocksAccess) chunk).retroapi$getExtendedBlocks();
			access.retroapi$populateExtended(extended);
		} else if (packet instanceof BlocksUpdatePacket bsp) {
			BlocksUpdatePacketAccess access = (BlocksUpdatePacketAccess) bsp;
			if (access.retroapi$getFullBlockIds() != null) return;

			WorldChunk chunk = player.world.getChunkAt(bsp.chunkX, bsp.chunkZ);
			access.retroapi$populateFullIds(chunk);
		}
	}
}
