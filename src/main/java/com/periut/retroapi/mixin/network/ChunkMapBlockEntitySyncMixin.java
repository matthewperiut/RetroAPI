package com.periut.retroapi.mixin.network;

import com.periut.retroapi.register.blockentity.BlockEntitySyncServer;
import com.periut.retroapi.register.blockentity.RetroSyncedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Carries {@link RetroSyncedBlockEntity} state on the per-chunk block-update path.
 *
 * <p>Vanilla already calls {@code sendBlockEntityUpdate} in all three of {@code updateChunk}'s branches
 * (one dirty block, a whole changed region, and the multi-block delta), so this is the complete
 * "something changed here" hook - it just does nothing for modded block entities, because the method
 * asks for a {@code createUpdatePacket()} and b1.7.3's protocol has no packet a modded block entity
 * could return. Everything RetroAPI needs is already computed by the time we get here: which block
 * entity, and exactly which players are tracking that chunk.
 */
@Mixin(targets = "net.minecraft.server.ChunkMap$TrackedChunk")
public class ChunkMapBlockEntitySyncMixin {

	@Shadow private List players;

	@Inject(method = "sendBlockEntityUpdate", at = @At("HEAD"))
	private void retroapi$syncBlockEntity(BlockEntity blockEntity, CallbackInfo ci) {
		if (blockEntity instanceof RetroSyncedBlockEntity) {
			BlockEntitySyncServer.sendToAll(this.players, blockEntity);
		}
	}
}
