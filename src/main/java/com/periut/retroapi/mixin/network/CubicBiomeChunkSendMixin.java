package com.periut.retroapi.mixin.network;

import com.periut.retroapi.biome.cubic.CubicBiomeSyncServer;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sends a chunk's cubic biome cells alongside the vanilla chunk packet.
 *
 * <p>Unlike {@link ChunkSendMixin} this only <em>observes</em> the chunk packet going out and then
 * pushes RetroAPI's own OSL message; it does not touch the packet. That is what lets it stay enabled
 * under StationAPI, where {@code ChunkSendMixin} and {@code WorldChunkPacketMixin} must be off because
 * StationAPI owns chunk packet contents - the reason cubic biomes ride their own channel in the first
 * place (see {@code RetroAPINetworking.CUBIC_BIOME_CHANNEL}).
 *
 * <p>Dedicated server only. b1.7.3 singleplayer <em>is</em> the client, sharing one World object, so the
 * client already holds every cell the server wrote and there is nothing to send.
 */
@Mixin(ServerPlayNetworkHandler.class)
public class CubicBiomeChunkSendMixin {

	@Shadow public ServerPlayerEntity player;

	@Inject(method = "sendPacket", at = @At("HEAD"), require = 0)
	private void retroapi$sendCubicBiomes(Packet packet, CallbackInfo ci) {
		if (!(packet instanceof ChunkDataS2CPacket chunkPacket)) {
			return;
		}
		Chunk chunk = player.world.getChunk(chunkPacket.x >> 4, chunkPacket.z >> 4);
		CubicBiomeSyncServer.sendChunk(player, chunk);
	}
}
