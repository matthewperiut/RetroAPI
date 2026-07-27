package com.periut.retroapi.mixin.network;

import com.periut.retroapi.register.blockentity.BlockEntitySyncServer;
import com.periut.retroapi.register.blockentity.RetroSyncedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The other half of {@link RetroSyncedBlockEntity} sync: initial state when a chunk is first sent to a
 * player. Vanilla walks the chunk's block entities here and asks each for a {@code createUpdatePacket()},
 * which is null for anything modded, so without this a barrel would show up empty until the next time
 * someone touched it. Sends to this one player only, which is exactly the scope vanilla intends here.
 */
@Mixin(ServerPlayerEntity.class)
public class PlayerChunkBlockEntitySyncMixin {

	@Inject(method = "updateBlockEntity", at = @At("HEAD"))
	private void retroapi$syncBlockEntity(BlockEntity blockEntity, CallbackInfo ci) {
		if (blockEntity instanceof RetroSyncedBlockEntity) {
			BlockEntitySyncServer.sendTo((ServerPlayerEntity) (Object) this, blockEntity);
		}
	}
}
