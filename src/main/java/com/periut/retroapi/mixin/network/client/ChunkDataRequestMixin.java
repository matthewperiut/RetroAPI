package com.periut.retroapi.mixin.network.client;

import com.periut.retroapi.register.blockentity.BlockEntitySyncRequest;
import net.minecraft.client.network.ClientNetworkHandler;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Asks the server what is inside a chunk that just arrived.
 *
 * <p>The other direction - the server volunteering block-entity state as it sends each chunk - cannot
 * be relied on by itself. At join time those sends happen before the client has finished registering
 * its channels and OSL discards them, and even later they can land a moment before the chunk they
 * describe. Both are races about WHEN the client is ready, so the client asks: by the time this runs
 * the chunk is in its world, its channels are up, and there is nothing left to be early for.
 *
 * <p>Cheap by construction - one small request per chunk, answered only for chunks that actually hold
 * a synced block entity, and most hold none.
 */
@Mixin(ClientNetworkHandler.class)
public class ChunkDataRequestMixin {

	@Inject(method = "handleChunkData", at = @At("TAIL"))
	private void retroapi$askWhatIsInside(ChunkDataS2CPacket packet, CallbackInfo ci) {
		BlockEntitySyncRequest.request(packet.x >> 4, packet.z >> 4);
	}
}
